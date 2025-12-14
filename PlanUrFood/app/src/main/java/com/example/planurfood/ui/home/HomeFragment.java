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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planurfood.R;
import com.example.planurfood.databinding.FragmentHomeBinding;
import com.example.planurfood.ui.gallery.FoodResources;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
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
    private Map<String, Map<String, List<String>>> libroDeRecetas = new HashMap<>();
    private Map<String, String> menuSemanal = new HashMap<>();
    private static final String ARCHIVO_PLAN = "listacompra.json";
    private static final String ARCHIVO_PANTRY = "midestpensa.json";
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

        RecyclerView recyclerView = binding.recyclerWeek;
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            weekAdapter = new WeekAdapter(diasSemana, menuSemanal, (dia, tipoComida, cajonTocado) ->
                    mostrarSelectorDeRecetas(dia, tipoComida, cajonTocado)
            );
            recyclerView.setAdapter(weekAdapter);
        }

        binding.fabShoppingList.setOnClickListener(v -> generarYMostrarListaCompra());
        binding.fabAddRecipe.setOnClickListener(v -> step1_PedirNombre());

        return root;
    }

    // --- ZONA 1: PERSISTENCIA ---
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
        try {
            FileInputStream fis = requireActivity().openFileInput(ARCHIVO_PLAN);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            fis.close();
            JSONObject rootObject = new JSONObject(sb.toString());
            menuSemanal = new HashMap<>();
            Iterator<String> keys = rootObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                menuSemanal.put(key, rootObject.getString(key));
            }
        } catch (Exception e) { menuSemanal = new HashMap<>(); }
    }

    private void cargarDatosDesdeSupabase() {
        SupabaseApi api = SupabaseClient.getApi();
        Call<List<RecetaModelo>> llamada = api.obtenerRecetas(SupabaseClient.SUPABASE_KEY, "Bearer " + SupabaseClient.SUPABASE_KEY);
        llamada.enqueue(new Callback<List<RecetaModelo>>() {
            @Override
            public void onResponse(Call<List<RecetaModelo>> call, Response<List<RecetaModelo>> response) {
                if (response.isSuccessful() && response.body() != null) procesarRecetas(response.body());
            }
            @Override
            public void onFailure(Call<List<RecetaModelo>> call, Throwable t) {
                Toast.makeText(getContext(), "Error conexión Supabase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void procesarRecetas(List<RecetaModelo> lista) {
        libroDeRecetas.clear();
        libroDeRecetas.put("breakfast", new HashMap<>());
        libroDeRecetas.put("lunch", new HashMap<>());
        libroDeRecetas.put("dinner", new HashMap<>());

        for (RecetaModelo r : lista) {
            if (r.getCategoria() == null || r.getNombre() == null) continue;
            String catLimpia = r.getCategoria().trim().toLowerCase();
            if (catLimpia.equals("diner")) catLimpia = "dinner";

            Map<String, List<String>> recetasDeEsaCategoria = libroDeRecetas.get(catLimpia);
            if (recetasDeEsaCategoria != null) {
                List<String> ingredientesTexto = new ArrayList<>();
                if (r.getIngredientes() != null) {
                    for (RecetaModelo.Ingrediente item : r.getIngredientes()) {
                        if (item.getNombre() != null) {
                            String ing = item.getNombre();
                            if(item.getCantidad() != null) ing += " (" + item.getCantidad() + ")";
                            ingredientesTexto.add(ing);
                        }
                    }
                }
                recetasDeEsaCategoria.put(r.getNombre(), ingredientesTexto);
            }
        }
    }

    private void step1_PedirNombre() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Paso 1/3: Nombre");
        final TextInputEditText input = new TextInputEditText(getContext());
        FrameLayout container = new FrameLayout(getContext());
        input.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(input);
        builder.setView(container);
        builder.setPositiveButton("Siguiente", (dialog, which) -> {
            if (!input.getText().toString().isEmpty()) step2_ElegirCategoria(input.getText().toString());
        });
        builder.show();
    }

    private void step2_ElegirCategoria(String nombre) { /* Código igual al anterior... */ }
    private void step3_PedirIngredientes(String nombre, String cat) { /* Código igual... */ }

    // --- ZONA PRINCIPAL: LOGICA ---
    private void mostrarSelectorDeRecetas(String dia, String tipoComida, TextInputEditText cajon) {
        View customView = getLayoutInflater().inflate(R.layout.dialog_meal_selector, null);
        TextView titulo = customView.findViewById(R.id.dialogTitle);
        titulo.setText(tipoComida + " (" + dia + ")");

        String claveBusqueda = tipoComida.trim().toLowerCase();
        if (claveBusqueda.equals("diner")) claveBusqueda = "dinner";

        Map<String, List<String>> recetasMap = libroDeRecetas.get(claveBusqueda);
        final List<String> nombresRecetas = new ArrayList<>();
        if (recetasMap != null) nombresRecetas.addAll(recetasMap.keySet());
        else nombresRecetas.add("No hay recetas");

        // Cargar despensa temporal para colorear
        Map<String, Double> pantryStockTemp = new HashMap<>();
        try {
            FileInputStream fis = requireActivity().openFileInput(ARCHIVO_PANTRY);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) sb.append(line);
            fis.close();
            JSONObject json = new JSONObject(sb.toString());
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                org.json.JSONArray items = json.getJSONArray(keys.next());
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String nombreNorm = FoodResources.getSingularName(item.getString("nombre")).toLowerCase();
                    double qty = extraerNumero(item.getString("cantidad"));
                    pantryStockTemp.put(nombreNorm, pantryStockTemp.getOrDefault(nombreNorm, 0.0) + qty);
                }
            }
        } catch (Exception e) { /* Vacío */ }

        ListView lista = customView.findViewById(R.id.listRecetas);
        if (recetasMap != null) {
            ColoredRecipeAdapter adapter = new ColoredRecipeAdapter(getContext(), nombresRecetas, recetasMap, pantryStockTemp);
            lista.setAdapter(adapter);
        } else {
            lista.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, nombresRecetas));
        }

        Button btnDelete = customView.findViewById(R.id.btnDeleteRecipe);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(customView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        lista.setOnItemClickListener((parent, view, position, id) -> {
            String recetaNueva = nombresRecetas.get(position);
            if (!recetaNueva.equals("No hay recetas")) {
                String key = dia + "_" + tipoComida;
                String recetaAnterior = menuSemanal.get(key);

                // Intercambio
                if (recetaAnterior != null && !recetaAnterior.isEmpty() && !recetaAnterior.equals(recetaNueva)) {
                    devolverIngredientesALaDespensa(recetaAnterior, tipoComida);
                }

                cajon.setText(recetaNueva);
                menuSemanal.put(key, recetaNueva);
                guardarPlanSemanal();
                descontarIngredientesDeDespensa(recetaNueva, tipoComida);
                dialog.dismiss();
            }
        });

        btnDelete.setOnClickListener(v -> {
            String key = dia + "_" + tipoComida;
            String recetaAnterior = menuSemanal.get(key);
            if (recetaAnterior != null && !recetaAnterior.isEmpty()) {
                devolverIngredientesALaDespensa(recetaAnterior, tipoComida);
            }
            menuSemanal.remove(key);
            cajon.setText("");
            guardarPlanSemanal();
            if (weekAdapter != null) weekAdapter.notifyDataSetChanged();
            dialog.dismiss();
            Toast.makeText(getContext(), "Deleted & Restored", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void generarYMostrarListaCompra() {
        ArrayList<String> listaCompra = new ArrayList<>();
        try {
            FileInputStream fis = requireActivity().openFileInput(ARCHIVO_PANTRY);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder jsonStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) jsonStr.append(line);
            fis.close();
            JSONObject json = new JSONObject(jsonStr.toString());
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                org.json.JSONArray items = json.getJSONArray(keys.next());
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String cantidadStr = item.getString("cantidad");
                    double stock = extraerNumero(cantidadStr);
                    if (stock < -0.01) {
                        String nombre = item.getString("nombre");
                        double falta = Math.abs(stock);

                        // AQUÍ FORZAMOS LA UNIDAD CORRECTA VISUALMENTE
                        String unidad = FoodResources.getAutoUnit(nombre, falta);

                        String cantidadBonita = (falta == (long) falta) ? String.valueOf((long)falta) : String.format("%.1f", falta);
                        listaCompra.add(nombre.toUpperCase() + " (Buy: " + cantidadBonita + " " + unidad + ")");
                    }
                }
            }
        } catch (Exception e) { }

        if (listaCompra.isEmpty()) Toast.makeText(getContext(), "Everything in stock!", Toast.LENGTH_SHORT).show();
        else new AlertDialog.Builder(getContext()).setTitle("Shopping List").setMultiChoiceItems(listaCompra.toArray(new String[0]), null, null).setPositiveButton("Close", null).show();
    }

    private void descontarIngredientesDeDespensa(String nombreReceta, String tipoComida) {
        String keyMap = tipoComida.toLowerCase();
        if(keyMap.equals("diner")) keyMap = "dinner";
        if (!libroDeRecetas.containsKey(keyMap) || !libroDeRecetas.get(keyMap).containsKey(nombreReceta)) return;

        List<String> ingredientesReceta = libroDeRecetas.get(keyMap).get(nombreReceta);
        JSONObject pantryRoot;
        try {
            FileInputStream fis = requireActivity().openFileInput(ARCHIVO_PANTRY);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            fis.close();
            pantryRoot = new JSONObject(sb.toString());
        } catch (Exception e) { pantryRoot = new JSONObject(); }

        boolean huboCambios = false;
        try {
            Map<String, Double> requisitos = new HashMap<>();
            List<String> encontrados = new ArrayList<>();

            for (String linea : ingredientesReceta) {
                String nombreRaw = linea;
                double cantidad = 1.0;
                if (linea.contains("(") && linea.contains(")")) {
                    nombreRaw = linea.substring(0, linea.indexOf("(")).trim();
                    cantidad = extraerNumero(linea);
                }
                String nombreNorm = FoodResources.getSingularName(nombreRaw).toLowerCase();
                requisitos.put(nombreNorm, cantidad);
            }

            Iterator<String> categorias = pantryRoot.keys();
            while (categorias.hasNext()) {
                String cat = categorias.next();
                org.json.JSONArray items = pantryRoot.getJSONArray(cat);
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String nombrePantryNorm = FoodResources.getSingularName(item.getString("nombre")).toLowerCase();
                    if (requisitos.containsKey(nombrePantryNorm)) {
                        double aRestar = requisitos.get(nombrePantryNorm);
                        double stockActual = extraerNumero(item.getString("cantidad"));
                        String unidad = item.getString("cantidad").replaceAll("[-+0-9.]", "").trim();
                        if(unidad.isEmpty()) unidad = "ud.";

                        double nuevoStock = stockActual - aRestar;
                        if (Math.abs(nuevoStock) < 0.01) {
                            nuevoStock = 0.0;
                        }
                        // NO CAMBIAMOS UNIDAD AQUÍ, MANTENEMOS LA DEL USUARIO
                        String finalStr = (nuevoStock == (long) nuevoStock) ? (long)nuevoStock + " " + unidad : String.format("%.1f %s", nuevoStock, unidad);
                        item.put("cantidad", finalStr);

                        encontrados.add(nombrePantryNorm);
                        huboCambios = true;
                    }
                }
            }

            // CREAR FALTANTES (NEGATIVOS)
            for (Map.Entry<String, Double> req : requisitos.entrySet()) {
                if (!encontrados.contains(req.getKey())) {
                    String categoriaDestino = "Pantry";
                    if (!pantryRoot.has(categoriaDestino)) pantryRoot.put(categoriaDestino, new org.json.JSONArray());
                    org.json.JSONArray listaCat = pantryRoot.getJSONArray(categoriaDestino);
                    JSONObject nuevoItem = new JSONObject();

                    String nombreBonito = req.getKey().substring(0, 1).toUpperCase() + req.getKey().substring(1);
                    double cantidadNegativa = -req.getValue();

                    // UNIDAD AUTOMÁTICA AQUÍ
                    String unidadAuto = FoodResources.getAutoUnit(nombreBonito, cantidadNegativa);

                    nuevoItem.put("nombre", nombreBonito);
                    nuevoItem.put("cantidad", cantidadNegativa + " " + unidadAuto);
                    nuevoItem.put("imagen", FoodResources.getIconFor(nombreBonito));
                    listaCat.put(nuevoItem);
                    huboCambios = true;
                }
            }
            if (huboCambios) {
                FileOutputStream fos = requireActivity().openFileOutput(ARCHIVO_PANTRY, MODE_PRIVATE);
                fos.write(pantryRoot.toString().getBytes());
                fos.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void devolverIngredientesALaDespensa(String nombreReceta, String tipoComida) {
        /* Código idéntico al de descontar, pero sumando 'cantidadADevolver' y sin crear nuevos items */
        // ... (Para abreviar, usa el código que ya te di en pasos anteriores, solo cambiando - por +)
        // La lógica es exactamente igual de simple: buscar, sumar, guardar.
        // Aquí no necesitamos crear nada porque si no está, no hay nada que devolver.
        // Copia el método devolverIngredientesALaDespensa de la respuesta anterior.
        String keyMap = tipoComida.toLowerCase();
        if(keyMap.equals("diner")) keyMap = "dinner";

        if (!libroDeRecetas.containsKey(keyMap) || !libroDeRecetas.get(keyMap).containsKey(nombreReceta)) return;

        List<String> ingredientesReceta = libroDeRecetas.get(keyMap).get(nombreReceta);
        JSONObject pantryRoot;
        try {
            FileInputStream fis = requireActivity().openFileInput(ARCHIVO_PANTRY);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            fis.close();
            pantryRoot = new JSONObject(sb.toString());
        } catch (Exception e) { return; }

        boolean huboCambios = false;
        try {
            Map<String, Double> aDevolver = new HashMap<>();
            for (String linea : ingredientesReceta) {
                String nombreRaw = linea;
                double cantidad = 1.0;
                if (linea.contains("(") && linea.contains(")")) {
                    nombreRaw = linea.substring(0, linea.indexOf("(")).trim();
                    cantidad = extraerNumero(linea);
                }
                String nombreNorm = FoodResources.getSingularName(nombreRaw).toLowerCase();
                aDevolver.put(nombreNorm, cantidad);
            }

            Iterator<String> categorias = pantryRoot.keys();
            while (categorias.hasNext()) {
                String cat = categorias.next();
                org.json.JSONArray items = pantryRoot.getJSONArray(cat);
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String nombrePantryNorm = FoodResources.getSingularName(item.getString("nombre")).toLowerCase();

                    if (aDevolver.containsKey(nombrePantryNorm)) {
                        double cantidadADevolver = aDevolver.get(nombrePantryNorm);
                        double stockActual = extraerNumero(item.getString("cantidad"));
                        String unidad = item.getString("cantidad").replaceAll("[-+0-9.]", "").trim();

                        double nuevoStock = stockActual + cantidadADevolver;
                        String finalStr = (nuevoStock == (long) nuevoStock) ? (long)nuevoStock + " " + unidad : String.format("%.1f %s", nuevoStock, unidad);
                        item.put("cantidad", finalStr);
                        huboCambios = true;
                    }
                }
            }
            if (huboCambios) {
                FileOutputStream fos = requireActivity().openFileOutput(ARCHIVO_PANTRY, MODE_PRIVATE);
                fos.write(pantryRoot.toString().getBytes());
                fos.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private double extraerNumero(String texto) {
        try {
            Matcher matcher = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+").matcher(texto);
            if (matcher.find()) return Double.parseDouble(matcher.group());
        } catch (Exception e) { }
        return 0.0;
    }

    private class ColoredRecipeAdapter extends ArrayAdapter<String> {
        private final Map<String, List<String>> recetasMap;
        private final Map<String, Double> pantryStock;
        public ColoredRecipeAdapter(Context context, List<String> recetas, Map<String, List<String>> recetasMap, Map<String, Double> pantryStock) {
            super(context, android.R.layout.simple_list_item_1, recetas);
            this.recetasMap = recetasMap;
            this.pantryStock = pantryStock;
        }
        @NonNull @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            String nombreReceta = getItem(position);
            if (nombreReceta == null || nombreReceta.equals("No hay recetas")) {
                view.setBackgroundColor(Color.WHITE); return view;
            }
            List<String> ingredientesNecesarios = recetasMap.get(nombreReceta);
            int total = ingredientesNecesarios.size(), tengo = 0;
            for (String ingRaw : ingredientesNecesarios) {
                String nombreNorm = "";
                double cantNec = 1.0;
                if (ingRaw.contains("(") && ingRaw.contains(")")) {
                    nombreNorm = FoodResources.getSingularName(ingRaw.substring(0, ingRaw.indexOf("(")).trim()).toLowerCase();
                    cantNec = extraerNumero(ingRaw);
                } else {
                    nombreNorm = FoodResources.getSingularName(ingRaw).toLowerCase();
                }
                if (pantryStock.containsKey(nombreNorm)) {
                    double cantTengo = pantryStock.get(nombreNorm);
                    if (cantTengo >= cantNec) tengo++;
                    else if (cantTengo > 0) tengo++;
                }
            }
            if (tengo == total) view.setBackgroundColor(Color.parseColor("#81C784"));
            else if (tengo > 0) view.setBackgroundColor(Color.parseColor("#FFF176"));
            else view.setBackgroundColor(Color.parseColor("#E57373"));
            return view;
        }
    }
}