package com.example.planurfood.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
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
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    // ESTRUCTURA DE DATOS
    private Map<String, Map<String, List<String>>> libroDeRecetas = new HashMap<>();
    private Map<String, String> menuSemanal = new HashMap<>();
    private static final String ARCHIVO_PLAN = "listacompra.json";
    private static final String ARCHIVO_PANTRY = "midestpensa.json";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 1. Cargar recetas al iniciar
        cargarDatosDesdeSupabase();

        cargarPlanSemanal();

        // 2. Configurar el RecyclerView del calendario
        ArrayList<String> diasSemana = new ArrayList<>(Arrays.asList(
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
        ));

        RecyclerView recyclerView = binding.recyclerWeek;
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            WeekAdapter adapter = new WeekAdapter(diasSemana, menuSemanal, (dia, tipoComida, cajonTocado) ->
                    mostrarSelectorDeRecetas(dia, tipoComida, cajonTocado)
            );
            recyclerView.setAdapter(adapter);
        }

        // 3. Botón Lista de la Compra
        binding.fabShoppingList.setOnClickListener(v -> generarYMostrarListaCompra());

        // 4. Botón Añadir Receta (Inicia el Paso 1 del Asistente)
        binding.fabAddRecipe.setOnClickListener(v -> step1_PedirNombre());

        return root;
    }

    // =================================================================================
    //  ZONA 1: LECTURA Y ESCRITURA EN SUPABASE
    // =================================================================================
    private void guardarPlanSemanal() {
        JSONObject rootObject = new JSONObject();
        try {
            // Convertimos el Map<String, String> a JSON
            for (Map.Entry<String, String> entry : menuSemanal.entrySet()) {
                rootObject.put(entry.getKey(), entry.getValue());
            }

            FileOutputStream fos = requireActivity().openFileOutput(ARCHIVO_PLAN, MODE_PRIVATE);
            fos.write(rootObject.toString().getBytes());
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarPlanSemanal() {
        FileInputStream fis = null;
        try {
            fis = requireActivity().openFileInput(ARCHIVO_PLAN);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject rootObject = new JSONObject(sb.toString());
            menuSemanal = new HashMap<>();

            // Leemos todas las claves (ej: "MONDAY_Breakfast") y sus valores
            Iterator<String> keys = rootObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String valor = rootObject.getString(key);
                menuSemanal.put(key, valor);
            }

        } catch (IOException | JSONException e) {
            menuSemanal = new HashMap<>(); // Si falla o no existe, mapa vacío
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }
    private void cargarDatosDesdeSupabase() {
        SupabaseApi api = SupabaseClient.getApi();
        Call<List<RecetaModelo>> llamada = api.obtenerRecetas(
                SupabaseClient.SUPABASE_KEY, "Bearer " + SupabaseClient.SUPABASE_KEY
        );

        llamada.enqueue(new Callback<List<RecetaModelo>>() {
            @Override
            public void onResponse(Call<List<RecetaModelo>> call, Response<List<RecetaModelo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    procesarRecetas(response.body());
                } else {
                    Toast.makeText(getContext(), "Error servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<RecetaModelo>> call, Throwable t) {
                Toast.makeText(getContext(), "Error conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void procesarRecetas(List<RecetaModelo> lista) {
        libroDeRecetas.clear();
        // Inicializamos claves en minúscula
        libroDeRecetas.put("breakfast", new HashMap<>());
        libroDeRecetas.put("lunch", new HashMap<>());
        libroDeRecetas.put("dinner", new HashMap<>());

        int contador = 0;
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
                contador++;
            }
        }
        Toast.makeText(getContext(), "¡" + contador + " recetas cargadas!", Toast.LENGTH_SHORT).show();
    }

    private void subirRecetaASupabase(String nombre, String categoria, String ingredientesTexto) {
        RecetaModelo nuevaReceta = new RecetaModelo();
        nuevaReceta.setNombre(nombre);
        nuevaReceta.setCategoria(categoria);

        List<RecetaModelo.Ingrediente> listaIngs = new ArrayList<>();
        if (ingredientesTexto != null && !ingredientesTexto.isEmpty()) {
            String[] partes = ingredientesTexto.split(",");
            for (String parte : partes) {
                String[] datos = parte.split(":");
                RecetaModelo.Ingrediente ing = new RecetaModelo.Ingrediente();
                if (datos.length >= 1) {
                    ing.setName(datos[0].trim());
                    if (datos.length > 1) ing.setQuantity(datos[1].trim());
                    else ing.setQuantity("1");
                    listaIngs.add(ing);
                }
            }
        }
        nuevaReceta.setIngredientes(listaIngs);

        SupabaseApi api = SupabaseClient.getApi();
        Call<Void> llamada = api.crearReceta(
                SupabaseClient.SUPABASE_KEY, "Bearer " + SupabaseClient.SUPABASE_KEY,
                "return=minimal", nuevaReceta
        );

        llamada.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "¡Receta Guardada!", Toast.LENGTH_SHORT).show();
                    cargarDatosDesdeSupabase(); // Recargar la lista
                } else if (response.code() == 409) {
                    Toast.makeText(getContext(), "Error: Ya existe ese nombre", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Error al guardar: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(), "Fallo técnico: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =================================================================================
    //  ZONA 2: ASISTENTE PASO A PASO (WIZARD)
    // =================================================================================

    // PASO 1: NOMBRE
    private void step1_PedirNombre() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Paso 1/3: Nombre");

        final TextInputEditText input = new TextInputEditText(getContext());
        input.setHint("Ej: Paella Valenciana");

        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(60, 20, 60, 20);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Siguiente", (dialog, which) -> {
            String nombre = input.getText().toString().trim();
            if (!nombre.isEmpty()) step2_ElegirCategoria(nombre);
            else Toast.makeText(getContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // PASO 2: CATEGORÍA (BOTONES)
    private void step2_ElegirCategoria(String nombreReceta) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Paso 2/3: Categoría");
        builder.setMessage("Para: " + nombreReceta);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 20, 60, 20);

        Button btnBreakfast = new Button(getContext()); btnBreakfast.setText("Breakfast");
        Button btnLunch = new Button(getContext());     btnLunch.setText("Lunch");
        Button btnDinner = new Button(getContext());    btnDinner.setText("Dinner");

        layout.addView(btnBreakfast);
        layout.addView(btnLunch);
        layout.addView(btnDinner);
        builder.setView(layout);

        AlertDialog dialog = builder.create();
        View.OnClickListener listener = v -> {
            String categoria = ((Button) v).getText().toString();
            dialog.dismiss();
            step3_PedirIngredientes(nombreReceta, categoria);
        };

        btnBreakfast.setOnClickListener(listener);
        btnLunch.setOnClickListener(listener);
        btnDinner.setOnClickListener(listener);
        dialog.show();
    }

    // PASO 3: INGREDIENTES
    private void step3_PedirIngredientes(String nombreReceta, String categoriaReceta) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Paso 3/3: Ingredientes");
        builder.setMessage(nombreReceta + " (" + categoriaReceta + ")");

        final TextInputEditText input = new TextInputEditText(getContext());
        input.setHint("Ej: Arroz:100, Pollo:1");

        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(60, 20, 60, 20);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("¡Crear!", (dialog, which) -> {
            String ingredientes = input.getText().toString();
            subirRecetaASupabase(nombreReceta, categoriaReceta, ingredientes);
        });
        builder.setNegativeButton("Atrás", (dialog, which) -> step2_ElegirCategoria(nombreReceta));
        builder.show();
    }

    // =================================================================================
    //  ZONA 3: UI DEL SELECTOR Y LISTA COMPRA
    // =================================================================================

    private void mostrarSelectorDeRecetas(String dia, String tipoComida, TextInputEditText cajon) {
        View customView = getLayoutInflater().inflate(R.layout.dialog_meal_selector, null);
        TextView titulo = customView.findViewById(R.id.dialogTitle);
        titulo.setText(tipoComida + " (" + dia + ")");

        String claveBusqueda = tipoComida.trim().toLowerCase();
        if (claveBusqueda.equals("diner")) claveBusqueda = "dinner";

        Map<String, List<String>> recetasMap = libroDeRecetas.get(claveBusqueda);
        final List<String> nombresRecetas = new ArrayList<>();

        if (recetasMap != null && !recetasMap.isEmpty()) {
            nombresRecetas.addAll(recetasMap.keySet());
        } else {
            nombresRecetas.add("No hay recetas disponibles");
        }

        ListView lista = customView.findViewById(R.id.listRecetas);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, nombresRecetas);
        lista.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(customView);

        // --- NUEVO: Botón para Borrar la selección actual ---
        builder.setNeutralButton("Errase", (dialog, which) -> {
            // 1. Limpiamos el texto visualmente
            cajon.setText("");

            // 2. Quitamos la receta del mapa de memoria
            menuSemanal.remove(dia + "_" + tipoComida);

            // 3. Guardamos los cambios en el archivo para que no vuelva a aparecer
            guardarPlanSemanal();

            Toast.makeText(getContext(), "Plan borrado", Toast.LENGTH_SHORT).show();
        });

        // Botón cancelar normal
        builder.setNegativeButton("Close", null);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        lista.setOnItemClickListener((parent, view, position, id) -> {
            String receta = nombresRecetas.get(position);
            if (!receta.equals("No hay recetas disponibles")) {
                cajon.setText(receta);
                menuSemanal.put(dia + "_" + tipoComida, receta);
                guardarPlanSemanal();

                // Descontamos ingredientes (OJO: Esto sigue restando si cambias de receta)
                descontarIngredientesDeDespensa(receta, tipoComida);

                dialog.dismiss();
            }
        });

        View btnOld = customView.findViewById(R.id.btnNewRecipe);
        if(btnOld != null) btnOld.setVisibility(View.GONE);

        dialog.show();
    }

    private void generarYMostrarListaCompra() {
        Map<String, Double> necesito = new HashMap<>();
        Map<String, Double> tengo = new HashMap<>();

        // 1. SUMAR LO QUE NECESITO (Del Menú Semanal)
        for (String nombreReceta : menuSemanal.values()) {
            // Buscar receta en las 3 categorías (Breakfast, Lunch, Dinner)
            for (Map<String, List<String>> cat : libroDeRecetas.values()) {
                if (cat.containsKey(nombreReceta)) {
                    for (String ing : cat.get(nombreReceta)) {
                        // Si el ingrediente es "Arroz (200)", sacamos "arroz" y 200.0
                        String nombre = ing.split("\\(")[0].trim().toLowerCase();
                        necesito.put(nombre, necesito.getOrDefault(nombre, 0.0) + extraerNumero(ing));
                    }
                }
            }
        }

        // 2. VER LO QUE TENGO (Leer archivo Pantry)
        try {
            FileInputStream fis = requireActivity().openFileInput("midestpensa.json"); // Tu archivo
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder jsonStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) jsonStr.append(line);

            JSONObject json = new JSONObject(jsonStr.toString());
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) { // Recorrer categorías
                org.json.JSONArray items = json.getJSONArray(keys.next());
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String nombre = item.getString("nombre").toLowerCase();
                    tengo.put(nombre, tengo.getOrDefault(nombre, 0.0) + extraerNumero(item.getString("cantidad")));
                }
            }
        } catch (Exception e) { /* Si falla, asumimos despensa vacía */ }

        // 3. CALCULAR Y MOSTRAR
        ArrayList<String> listaFinal = new ArrayList<>();
        for (String ingrediente : necesito.keySet()) {
            double cantidadFaltante = necesito.get(ingrediente) - tengo.getOrDefault(ingrediente, 0.0);

            if (cantidadFaltante > 0) {
                listaFinal.add(ingrediente.toUpperCase() + " (Missing: " + (int)cantidadFaltante + ")");
            }
        }

        if (listaFinal.isEmpty()) {
            Toast.makeText(getContext(), "You already have everything!", Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(getContext())
                    .setTitle("TO BUY")
                    .setMultiChoiceItems(listaFinal.toArray(new String[0]), null, null)
                    .setPositiveButton("Close", null)
                    .show();
        }
    }


    private void descontarIngredientesDeDespensa(String nombreReceta, String tipoComida) {
        // 1. Buscamos qué ingredientes tiene esa receta
        // Nota: 'tipoComida' viene como "Breakfast", pero tu mapa puede tener "breakfast".
        String keyMap = tipoComida.toLowerCase();
        if(keyMap.equals("diner")) keyMap = "dinner";

        if (!libroDeRecetas.containsKey(keyMap) || !libroDeRecetas.get(keyMap).containsKey(nombreReceta)) {
            return; // No encontramos la receta
        }

        List<String> ingredientesReceta = libroDeRecetas.get(keyMap).get(nombreReceta);
        if (ingredientesReceta == null || ingredientesReceta.isEmpty()) return;

        // 2. Cargamos la despensa actual
        JSONObject pantryRoot = new JSONObject();
        try {
            FileInputStream fis = requireActivity().openFileInput(ARCHIVO_PANTRY);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            fis.close();

            pantryRoot = new JSONObject(sb.toString());

        } catch (Exception e) {
            // Si no existe la despensa, no podemos restar nada
            return;
        }

        // 3. Procesamos la resta
        boolean huboCambios = false;

        try {
            // Convertimos la lista de ingredientes de la receta (Strings) en un Mapa para buscar rápido
            // Ejemplo receta: "Huevos (2)", "Leche (200)" -> Map: "Huevos":2, "Leche":200
            Map<String, Double> requisitos = new HashMap<>();
            for (String linea : ingredientesReceta) {
                // Formato esperado: "Nombre (Cantidad)" o simplemente "Nombre"
                String nombre = linea;
                double cantidad = 1.0;

                if (linea.contains("(") && linea.contains(")")) {
                    nombre = linea.substring(0, linea.indexOf("(")).trim();
                    String cantStr = linea.substring(linea.indexOf("(") + 1, linea.indexOf(")"));
                    cantidad = extraerNumero(cantStr);
                }
                requisitos.put(nombre.toLowerCase(), cantidad);
            }

            // Recorremos la despensa JSON buscando coincidencias
            Iterator<String> categorias = pantryRoot.keys();
            while (categorias.hasNext()) {
                String cat = categorias.next();
                org.json.JSONArray items = pantryRoot.getJSONArray(cat);

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String nombreItem = item.getString("nombre").toLowerCase();

                    if (requisitos.containsKey(nombreItem)) {
                        // ¡Coincidencia! Restamos
                        double aRestar = requisitos.get(nombreItem);
                        String stockStr = item.getString("cantidad");
                        double stockActual = extraerNumero(stockStr);
                        String unidad = stockStr.replaceAll("[0-9.]", "").trim(); // Guardamos "kg", "uds"

                        double nuevoStock = stockActual - aRestar;
                        if (nuevoStock < 0) nuevoStock = 0;

                        // Guardamos el nuevo valor manteniendo la unidad (Ej: "4.0 uds")
                        // Usamos Math.round para evitar decimales feos si son enteros
                        nuevoStock = stockActual - aRestar;
                        if (nuevoStock < 0) nuevoStock = 0;


                        if (nuevoStock == (long) nuevoStock) {
                            item.put("cantidad", String.format("%d %s", (long)nuevoStock, unidad).trim());
                        } else {
                            item.put("cantidad", String.format("%.1f %s", nuevoStock, unidad).trim());
                        }
                        huboCambios = true;

                        // Notificamos al usuario
                        final String msg = "Descontado: " + item.getString("nombre");
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }

            // 4. Si hubo cambios, guardamos el archivo Pantry actualizado
            if (huboCambios) {
                FileOutputStream fos = requireActivity().openFileOutput(ARCHIVO_PANTRY, MODE_PRIVATE);
                fos.write(pantryRoot.toString().getBytes());
                fos.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Función auxiliar para sacar números de un texto ("2 kg" -> 2.0)
    private double extraerNumero(String texto) {
        try {
            // Busca el primer número (entero o decimal) que encuentre
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[0-9]+(\\.[0-9]+)?").matcher(texto);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group());
            }
        } catch (Exception e) { }
        return 1.0; // Valor por defecto si no hay número
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}