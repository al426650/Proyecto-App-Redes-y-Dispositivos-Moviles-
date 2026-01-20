package com.example.planurfood.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.planurfood.R;
import com.example.planurfood.databinding.FragmentHomeBinding;
import com.example.planurfood.ui.gallery.FoodResources;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    // Memoria
    private Map<String, Map<String, List<String>>> libroDeRecetas = new HashMap<>();
    private Map<String, String> menuSemanal = new HashMap<>();

    // Archivos
    private static final String ARCHIVO_PLAN = "plan_semanal.json";
    private static final String ARCHIVO_PANTRY = "midestpensa.json";
    private static final String ARCHIVO_COMPRA = "lista_compra.json";
    private static final String ARCHIVO_RECETAS_CACHE = "recetas_cache.json";

    private WeekAdapter weekAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        cargarDatosDesdeSupabase();
        cargarPlanSemanal();

        ArrayList<String> diasSemana = new ArrayList<>(Arrays.asList(
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
        ));

        binding.recyclerWeek.setLayoutManager(new LinearLayoutManager(getContext()));
        weekAdapter = new WeekAdapter(diasSemana, menuSemanal, (dia, tipoComida, cajonTocado) ->
                mostrarSelectorDeRecetas(dia, tipoComida, cajonTocado)
        );
        binding.recyclerWeek.setAdapter(weekAdapter);

        binding.fabShoppingList.setOnClickListener(v -> mostrarListaCompra());
        binding.fabAddRecipe.setOnClickListener(v -> step1_PedirNombre());

        return root;
    }

    // =========================================================================
    // LOGICA PRINCIPAL: GESTIÓN DE STOCK Y LISTA COMPRA
    // =========================================================================

    private void procesarIngredientesReceta(String nombreReceta, String tipoComida, boolean esAnadir) {
        String keyMap = tipoComida.toLowerCase().trim();
        if(keyMap.equals("diner")) keyMap = "dinner";

        if (!libroDeRecetas.containsKey(keyMap) || !libroDeRecetas.get(keyMap).containsKey(nombreReceta)) return;

        List<String> ingredientesNecesarios = libroDeRecetas.get(keyMap).get(nombreReceta);

        JSONObject jsonPantry = leerJSON(ARCHIVO_PANTRY);
        JSONObject jsonCompra = leerJSON(ARCHIVO_COMPRA);

        // Mapeo de stock actual
        Map<String, Double> stockMap = new HashMap<>();
        Map<String, JSONObject> refMap = new HashMap<>();

        try {
            Iterator<String> keys = jsonPantry.keys();
            while (keys.hasNext()) {
                String cat = keys.next();
                JSONArray items = jsonPantry.getJSONArray(cat);
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String n = limpiarNombre(item.getString("nombre"));
                    double q = extraerNumero(item.getString("cantidad"));
                    stockMap.put(n, stockMap.getOrDefault(n, 0.0) + q);
                    if (!refMap.containsKey(n)) refMap.put(n, item);
                }
            }
        } catch (Exception e) {}

        boolean pantryChanged = false;
        boolean compraChanged = false;

        for (String ingRaw : ingredientesNecesarios) {
            String nombreIng = limpiarNombre(ingRaw);
            double cantidadNecesaria = extraerNumero(ingRaw);
            if(cantidadNecesaria <= 0) cantidadNecesaria = 1.0;

            if (esAnadir) {
                // AÑADIR: Restar de despensa, si falta -> lista compra
                double stockDisponible = stockMap.getOrDefault(nombreIng, 0.0);

                if (stockDisponible >= cantidadNecesaria) {
                    actualizarItemPantry(refMap.get(nombreIng), stockDisponible - cantidadNecesaria, nombreIng);
                    stockMap.put(nombreIng, stockDisponible - cantidadNecesaria);
                    pantryChanged = true;
                } else {
                    double loQueTengo = stockDisponible;
                    double loQueFalta = cantidadNecesaria - loQueTengo;

                    if (loQueTengo > 0) {
                        actualizarItemPantry(refMap.get(nombreIng), 0.0, nombreIng);
                        stockMap.put(nombreIng, 0.0);
                        pantryChanged = true;
                    }
                    agregarAListaCompra(jsonCompra, nombreIng, loQueFalta);
                    compraChanged = true;
                }
            } else {
                // BORRAR RECETA: devuelve alimentos a la pantry
                double cantidadEnListaCompra = obtenerCantidadEnListaCompra(jsonCompra, nombreIng);

                if (cantidadEnListaCompra > 0) {
                    // Si estaba en la lista de compra, lo quitamos de ahí
                    double aQuitarDeCompra = Math.min(cantidadEnListaCompra, cantidadNecesaria);
                    restarDeListaCompra(jsonCompra, nombreIng, aQuitarDeCompra);
                    compraChanged = true;

                    // Si sobró algo, eso sí vuelve a despensa
                    double restoParaDespensa = cantidadNecesaria - aQuitarDeCompra;
                    if (restoParaDespensa > 0.01) {
                        devolverADespensa(jsonPantry, refMap, nombreIng, restoParaDespensa);
                        pantryChanged = true;
                    }
                } else {
                    // Si no estaba en lista compra, es que lo gastamos de la despensa. Lo devolvemos.
                    devolverADespensa(jsonPantry, refMap, nombreIng, cantidadNecesaria);
                    pantryChanged = true;
                }
            }
        }

        if (pantryChanged) guardarJSON(ARCHIVO_PANTRY, jsonPantry);
        if (compraChanged) guardarJSON(ARCHIVO_COMPRA, jsonCompra);
    }


    private void devolverADespensa(JSONObject jsonPantry, Map<String, JSONObject> refMap, String nombreIng, double cantidad) {
        try {
            if (refMap.containsKey(nombreIng)) {
                JSONObject item = refMap.get(nombreIng);
                double actual = extraerNumero(item.getString("cantidad"));
                actualizarItemPantry(item, actual + cantidad, nombreIng);
            } else {
                crearItemEnPantry(jsonPantry, nombreIng, cantidad);
            }
        } catch (Exception e) {}
    }

    private double obtenerCantidadEnListaCompra(JSONObject jsonCompra, String nombreNorm) {
        try {
            if (!jsonCompra.has("items")) return 0.0;
            JSONArray items = jsonCompra.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (limpiarNombre(item.getString("nombre")).equals(nombreNorm)) {
                    return item.optDouble("cantidadNum", 0.0);
                }
            }
        } catch (Exception e) {}
        return 0.0;
    }

    private void restarDeListaCompra(JSONObject jsonCompra, String nombreNorm, double cantidad) {
        try {
            JSONArray items = jsonCompra.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (limpiarNombre(item.getString("nombre")).equals(nombreNorm)) {
                    double actual = item.optDouble("cantidadNum", 0.0);
                    double nuevo = Math.max(0, actual - cantidad);
                    item.put("cantidadNum", nuevo);
                    break;
                }
            }
        } catch (Exception e) {}
    }

    private void actualizarItemPantry(JSONObject item, double nuevaCantidad, String nombreBase) {
        if (item == null) return;
        try {
            String unidad = FoodResources.getAutoUnit(nombreBase, nuevaCantidad);
            String qtyStr = (nuevaCantidad == (long) nuevaCantidad) ? String.valueOf((long) nuevaCantidad) : String.valueOf(nuevaCantidad);
            item.put("cantidad", qtyStr + " " + unidad);
        } catch (Exception e) {}
    }

    private void crearItemEnPantry(JSONObject jsonPantry, String nombre, double cantidad) {
        try {
            if(!jsonPantry.has("Pantry")) jsonPantry.put("Pantry", new JSONArray());
            JSONArray list = jsonPantry.getJSONArray("Pantry");
            JSONObject newItem = new JSONObject();
            String nombreCap = nombre.substring(0,1).toUpperCase() + nombre.substring(1);
            String unidad = FoodResources.getAutoUnit(nombre, cantidad);
            String qtyStr = (cantidad == (long) cantidad) ? String.valueOf((long) cantidad) : String.valueOf(cantidad);

            newItem.put("nombre", nombreCap);
            newItem.put("cantidad", qtyStr + " " + unidad);
            newItem.put("imagen", FoodResources.getIconFor(nombreCap));
            list.put(newItem);
        } catch (Exception e){}
    }

    private void agregarAListaCompra(JSONObject jsonCompra, String nombre, double cantidadFaltante) {
        try {
            if (!jsonCompra.has("items")) jsonCompra.put("items", new JSONArray());
            JSONArray items = jsonCompra.getJSONArray("items");

            boolean found = false;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String n = limpiarNombre(item.getString("nombre"));
                if (n.equals(nombre)) {
                    double actual = item.optDouble("cantidadNum", 0.0);
                    item.put("cantidadNum", actual + cantidadFaltante);
                    found = true;
                    break;
                }
            }
            if (!found) {
                JSONObject newItem = new JSONObject();
                String nombreCap = nombre.substring(0,1).toUpperCase() + nombre.substring(1);
                newItem.put("nombre", nombreCap);
                newItem.put("cantidadNum", cantidadFaltante);
                items.put(newItem);
            }
        } catch (Exception e) {}
    }

    private String limpiarNombre(String nombreRaw) {
        if (nombreRaw == null) return "";
        String nombre = nombreRaw;
        if (nombre.contains("(")) {
            nombre = nombre.substring(0, nombre.indexOf("("));
        }
        nombre = nombre.trim().toLowerCase();
        if (nombre.endsWith("s") && nombre.length() > 3) {
            nombre = nombre.substring(0, nombre.length() - 1);
        }
        if (nombre.endsWith("ce")) nombre = nombre.substring(0, nombre.length()-2) + "z"; // nueces -> nuez
        return nombre;
    }


    // =========================================================================
    // UI Y ADAPTADORES
    // =========================================================================

    private void mostrarSelectorDeRecetas(String dia, String tipoComida, TextInputEditText cajon) {
        View customView = getLayoutInflater().inflate(R.layout.dialog_meal_selector, null);
        TextView titulo = customView.findViewById(R.id.dialogTitle);
        titulo.setText(tipoComida + " (" + dia + ")");

        String claveBusqueda = tipoComida.trim().toLowerCase();
        if (claveBusqueda.equals("diner")) claveBusqueda = "dinner";

        Map<String, List<String>> recetasMap = libroDeRecetas.get(claveBusqueda);
        List<String> nombresRecetas = new ArrayList<>();
        if (recetasMap != null) nombresRecetas.addAll(recetasMap.keySet());
        else nombresRecetas.add("No hay recetas");

        Map<String, Double> stockActual = leerStockDeArchivo();

        ListView lista = customView.findViewById(R.id.listRecetas);
        if (recetasMap != null) {
            ColoredRecipeAdapter adapter = new ColoredRecipeAdapter(getContext(), nombresRecetas, recetasMap, stockActual);
            lista.setAdapter(adapter);
        } else {
            lista.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, nombresRecetas));
        }

        Button btnDelete = customView.findViewById(R.id.btnDeleteRecipe);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(customView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        lista.setOnItemClickListener((parent, view, position, id) -> {
            String recetaNueva = nombresRecetas.get(position);
            if (!recetaNueva.equals("No hay recetas")) {
                String key = dia + "_" + tipoComida;

                String recetaVieja = menuSemanal.get(key);
                if (recetaVieja != null && !recetaVieja.isEmpty()) {
                    procesarIngredientesReceta(recetaVieja, tipoComida, false);
                }

                procesarIngredientesReceta(recetaNueva, tipoComida, true);

                cajon.setText(recetaNueva);
                menuSemanal.put(key, recetaNueva);
                guardarPlanSemanal();
                dialog.dismiss();
            }
        });

        // BOTÓN BORRAR
        btnDelete.setOnClickListener(v -> {
            String key = dia + "_" + tipoComida;
            String recetaVieja = menuSemanal.get(key);
            if (recetaVieja != null && !recetaVieja.isEmpty()) {
                procesarIngredientesReceta(recetaVieja, tipoComida, false);
                Toast.makeText(getContext(), "Recipe deleted", Toast.LENGTH_SHORT).show();
            }
            menuSemanal.remove(key);
            cajon.setText("");
            guardarPlanSemanal();
            dialog.dismiss();
        });

        dialog.show();
    }

    private class ColoredRecipeAdapter extends ArrayAdapter<String> {
        private final Map<String, List<String>> recetasMap;
        private final Map<String, Double> stockActual;

        public ColoredRecipeAdapter(Context context, List<String> recetas, Map<String, List<String>> recetasMap, Map<String, Double> stockActual) {
            super(context, android.R.layout.simple_list_item_1, recetas);
            this.recetasMap = recetasMap;
            this.stockActual = stockActual;
        }

        @NonNull @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            String nombreReceta = getItem(position);

            if (nombreReceta == null || nombreReceta.equals("No hay recetas")) {
                view.setBackgroundColor(Color.WHITE);
                return view;
            }

            List<String> ingredientes = recetasMap.get(nombreReceta);
            boolean tengoTodo = true;
            boolean tengoAlgo = false;

            for (String itemRaw : ingredientes) {
                String nombreNorm = limpiarNombre(itemRaw);
                double cantidadNecesaria = extraerNumero(itemRaw);
                if (cantidadNecesaria <= 0) cantidadNecesaria = 1.0;

                double cantidadTengo = stockActual.getOrDefault(nombreNorm, 0.0);

                if (cantidadTengo >= cantidadNecesaria) {
                    tengoAlgo = true;
                } else {
                    tengoTodo = false;
                    if (cantidadTengo > 0) tengoAlgo = true;
                }
            }

            if (tengoTodo) view.setBackgroundColor(Color.parseColor("#A5D6A7")); // Verde
            else if (tengoAlgo) view.setBackgroundColor(Color.parseColor("#FFF59D")); // Amarillo
            else view.setBackgroundColor(Color.parseColor("#EF9A9A")); // Rojo

            return view;
        }
    }

    // =========================================================================
    // CARGA DE DATOS Y ARCHIVOS
    // =========================================================================

    private Map<String, Double> leerStockDeArchivo() {
        Map<String, Double> stock = new HashMap<>();
        JSONObject json = leerJSON(ARCHIVO_PANTRY);
        try {
            Iterator<String> keys = json.keys();
            while(keys.hasNext()) {
                JSONArray arr = json.getJSONArray(keys.next());
                for(int i=0; i<arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String n = limpiarNombre(obj.getString("nombre"));
                    double q = extraerNumero(obj.getString("cantidad"));
                    stock.put(n, stock.getOrDefault(n, 0.0) + q);
                }
            }
        } catch(Exception e){}
        return stock;
    }

    private JSONObject leerJSON(String archivo) {
        try {
            FileInputStream fis = requireActivity().openFileInput(archivo);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            fis.close();
            return new JSONObject(sb.toString());
        } catch (Exception e) { return new JSONObject(); }
    }

    private void guardarJSON(String archivo, JSONObject json) {
        try {
            FileOutputStream fos = requireActivity().openFileOutput(archivo, MODE_PRIVATE);
            fos.write(json.toString().getBytes());
            fos.close();
        } catch (Exception e) {}
    }

    private void guardarPlanSemanal() {
        JSONObject rootObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : menuSemanal.entrySet()) {
                rootObject.put(entry.getKey(), entry.getValue());
            }
            FileOutputStream fos = requireActivity().openFileOutput(ARCHIVO_PLAN, MODE_PRIVATE);
            fos.write(rootObject.toString().getBytes());
            fos.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void cargarPlanSemanal() {
        menuSemanal.clear(); //  Limpiar memoria
        try {
            FileInputStream fis = requireActivity().openFileInput(ARCHIVO_PLAN);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            fis.close();

            JSONObject rootObject = new JSONObject(sb.toString());
            Iterator<String> keys = rootObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                menuSemanal.put(key, rootObject.getString(key));
            }
        } catch (Exception e) {}
    }

    private double extraerNumero(String t) {
        try { Matcher m = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+").matcher(t);
            if(m.find()) return Double.parseDouble(m.group()); } catch(Exception e){}
        return 0.0;
    }

    private void mostrarListaCompra() {
        JSONObject json = leerJSON(ARCHIVO_COMPRA);
        List<String> itemsVisuales = new ArrayList<>();

        try {
            if (json.has("items")) {
                JSONArray arr = json.getJSONArray("items");
                for(int i=0; i<arr.length(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    String nombre = item.getString("nombre");
                    double qty = item.getDouble("cantidadNum");
                    if (qty > 0.01) {
                        String unidad = FoodResources.getAutoUnit(nombre, qty);
                        String qtyStr = (qty == (long)qty) ? String.valueOf((long)qty) : String.format("%.1f", qty);
                        itemsVisuales.add(nombre + ": " + qtyStr + " " + unidad);
                    }
                }
            }
        } catch (Exception e) {}

        if (itemsVisuales.isEmpty()) {
            Toast.makeText(getContext(), "Empty shopping list", Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(getContext())
                    .setTitle("Lista de la Compra")
                    .setItems(itemsVisuales.toArray(new String[0]), null)
                    .setPositiveButton("Cerrar", null)
                    .setNeutralButton("Borrar Todo", (d, w) -> {
                        getContext().deleteFile(ARCHIVO_COMPRA);
                        Toast.makeText(getContext(), "Deleted list", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        }
    }

    private void cargarDatosDesdeSupabase() {
        SupabaseApi api = SupabaseClient.getApi();
        Call<List<RecetaModelo>> llamada = api.obtenerRecetas(SupabaseClient.SUPABASE_KEY, "Bearer " + SupabaseClient.SUPABASE_KEY);
        llamada.enqueue(new Callback<List<RecetaModelo>>() {
            @Override public void onResponse(Call<List<RecetaModelo>> call, Response<List<RecetaModelo>> response) { if (response.body() != null) procesarRecetas(response.body()); }
            @Override public void onFailure(Call<List<RecetaModelo>> call, Throwable t) {}
        });
    }

    private void procesarRecetas(List<RecetaModelo> lista) {
        libroDeRecetas.clear();
        libroDeRecetas.put("breakfast", new HashMap<>());
        libroDeRecetas.put("lunch", new HashMap<>());
        libroDeRecetas.put("dinner", new HashMap<>());

        JSONObject cacheJson = new JSONObject();
        try {
            cacheJson.put("breakfast", new JSONObject());
            cacheJson.put("lunch", new JSONObject());
            cacheJson.put("dinner", new JSONObject());

            for (RecetaModelo r : lista) {
                if (r.getCategoria() == null) continue;
                String catLimpia = r.getCategoria().trim().toLowerCase();
                if (catLimpia.equals("diner")) catLimpia = "dinner";

                if (libroDeRecetas.containsKey(catLimpia)) {
                    List<String> ingredientesTexto = new ArrayList<>();
                    JSONArray jsonIngs = new JSONArray();

                    if (r.getIngredientes() != null) {
                        for (RecetaModelo.Ingrediente item : r.getIngredientes()) {
                            String ing = item.getNombre() + " (" + item.getCantidad() + ")";
                            ingredientesTexto.add(ing);
                            jsonIngs.put(ing);
                        }
                    }
                    libroDeRecetas.get(catLimpia).put(r.getNombre(), ingredientesTexto);
                    cacheJson.getJSONObject(catLimpia).put(r.getNombre(), jsonIngs);
                }
            }
            // Guardar Cache para GalleryFragment
            FileOutputStream fos = requireActivity().openFileOutput(ARCHIVO_RECETAS_CACHE, MODE_PRIVATE);
            fos.write(cacheJson.toString().getBytes());
            fos.close();

        } catch (Exception e) { e.printStackTrace(); }
    }


    private void step1_PedirNombre() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Nueva Receta: Nombre");
        final TextInputEditText input = new TextInputEditText(getContext());
        builder.setView(input);

        builder.setPositiveButton("Siguiente", (dialog, which) -> {
            String nombre = input.getText().toString();
            if (!nombre.isEmpty()) {
                step2_PedirTipo(nombre);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void step2_PedirTipo(String nombreReceta) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("¿Para qué comida es?");
        String[] tipos = {"Breakfast", "Lunch", "Dinner"};

        builder.setItems(tipos, (dialog, which) -> {
            String tipoSeleccionado = tipos[which].toLowerCase();
            step3_PedirIngredientes(nombreReceta, tipoSeleccionado);
        });
        builder.show();
    }

    private void step3_PedirIngredientes(String nombreReceta, String tipoComida) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Ingredientes (separados por coma)");
        final TextInputEditText input = new TextInputEditText(getContext());
        input.setHint("Ej: 2 huevos, 100g harina...");
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String rawIngredientes = input.getText().toString();
            if (!rawIngredientes.isEmpty()) {
                guardarNuevaReceta(nombreReceta, tipoComida, rawIngredientes);
            }
        });
        builder.show();
    }

    private void guardarNuevaReceta(String nombre, String categoria, String ingredientesRaw) {
        // 1. Guardar en Memoria (libroDeRecetas)
        if (!libroDeRecetas.containsKey(categoria)) {
            libroDeRecetas.put(categoria, new HashMap<>());
        }

        List<String> listaIng = new ArrayList<>();
        String[] partes = ingredientesRaw.split(",");
        for (String p : partes) {
            listaIng.add(p.trim());
        }

        libroDeRecetas.get(categoria).put(nombre, listaIng);

        // 2. Guardar en Archivo Caché (recetas_cache.json) para persistencia local
        try {
            JSONObject cache = new JSONObject();
            // Leemos lo que había o creamos nuevo
            try {
                FileInputStream fis = requireActivity().openFileInput(ARCHIVO_RECETAS_CACHE);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                cache = new JSONObject(sb.toString());
            } catch (Exception e) {
                // Si no existe, estructura base
                cache.put("breakfast", new JSONObject());
                cache.put("lunch", new JSONObject());
                cache.put("dinner", new JSONObject());
            }

            // Añadimos la nueva
            JSONArray jsonIngs = new JSONArray();
            for (String ing : listaIng) jsonIngs.put(ing);

            if (!cache.has(categoria)) cache.put(categoria, new JSONObject());
            cache.getJSONObject(categoria).put(nombre, jsonIngs);

            FileOutputStream fos = requireActivity().openFileOutput(ARCHIVO_RECETAS_CACHE, MODE_PRIVATE);
            fos.write(cache.toString().getBytes());
            fos.close();

            Toast.makeText(getContext(), "Recipe saved locally", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving recipe", Toast.LENGTH_SHORT).show();
        }
    }
}