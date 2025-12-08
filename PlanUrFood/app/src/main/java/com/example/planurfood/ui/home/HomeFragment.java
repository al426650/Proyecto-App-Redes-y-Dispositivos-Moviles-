package com.example.planurfood.ui.home;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 1. Cargar recetas al iniciar
        cargarDatosDesdeSupabase();

        // 2. Configurar el RecyclerView del calendario
        ArrayList<String> diasSemana = new ArrayList<>(Arrays.asList(
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
        ));

        RecyclerView recyclerView = binding.recyclerWeek;
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            WeekAdapter adapter = new WeekAdapter(diasSemana, (dia, tipoComida, cajonTocado) ->
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
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        lista.setOnItemClickListener((parent, view, position, id) -> {
            String receta = nombresRecetas.get(position);
            if (!receta.equals("No hay recetas disponibles")) {
                cajon.setText(receta);
                menuSemanal.put(dia + "_" + tipoComida, receta);
                dialog.dismiss();
            }
        });

        // Ocultamos el botón viejo si existe en el XML
        View btnOld = customView.findViewById(R.id.btnNewRecipe);
        if(btnOld != null) btnOld.setVisibility(View.GONE);

        dialog.show();
    }

    private void generarYMostrarListaCompra() {
        Set<String> unicos = new HashSet<>();
        for (String receta : menuSemanal.values()) {
            for (Map<String, List<String>> cat : libroDeRecetas.values()) {
                if (cat.containsKey(receta)) {
                    List<String> ings = cat.get(receta);
                    if (ings != null) unicos.addAll(ings);
                }
            }
        }
        if (unicos.isEmpty()) {
            Toast.makeText(getContext(), "Planifica comidas primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = unicos.toArray(new String[0]);
        boolean[] checked = new boolean[items.length];

        new AlertDialog.Builder(getContext())
                .setTitle("Lista de la Compra")
                .setMultiChoiceItems(items, checked, (d, w, c) -> checked[w] = c)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}