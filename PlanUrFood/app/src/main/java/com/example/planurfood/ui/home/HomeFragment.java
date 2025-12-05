package com.example.planurfood.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.planurfood.databinding.FragmentHomeBinding;
import com.example.planurfood.ui.home.WeekAdapter;
import com.example.planurfood.R;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;


import android.app.AlertDialog;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Map<String, Map<String, List<String>>> libroDeRecetas = new HashMap<>();
    private Map<String, String> menuSemanal = new HashMap<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Esta línea conecta con el diseño XML
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        cargarDatosEjemplo();
        View root = binding.getRoot();

        cargarDatosDesdeSupabase();

        // 1. Preparamos los datos
        ArrayList<String> diasSemana = new ArrayList<>();
        diasSemana.add("MONDAY");
        diasSemana.add("TUESDAY");
        diasSemana.add("WEDNESDAY");
        diasSemana.add("THURSDAY");
        diasSemana.add("FRIDAY");
        diasSemana.add("SATURDAY");
        diasSemana.add("SUNDAY");

        // 2. Buscamos el RecyclerView en el XML
        // IMPORTANTE: Asegúrate de que en fragment_home.xml el id es android:id="@+id/recyclerWeek"
        androidx.recyclerview.widget.RecyclerView recyclerView = binding.recyclerWeek;

        // 3. Le decimos que se coloque como lista vertical
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));

            // 4. Creamos el Adapter y se lo ponemos
            WeekAdapter adapter = new WeekAdapter(diasSemana, new WeekAdapter.OnMealClickListener() {
                @Override
                public void onMealClick(String dia, String tipoComida, TextInputEditText cajonTocado) {
                    mostrarSelectorDeRecetas(dia, tipoComida, cajonTocado);
                }
            });
            recyclerView.setAdapter(adapter);
        }

        binding.fabShoppingList.setOnClickListener(v -> {
            generarYMostrarListaCompra();
        });

        return root;
    }
    private void mostrarSelectorDeRecetas(String dia, String tipoComida, TextInputEditText cajon) {
        View customView = getLayoutInflater().inflate(R.layout.dialog_meal_selector, null);

        android.widget.TextView titulo = customView.findViewById(R.id.dialogTitle);
        titulo.setText(tipoComida + " of " + dia);

        // --- DEBUG: Comprobamos qué está buscando la app ---
        Map<String, List<String>> recetasDelTipo = libroDeRecetas.get(tipoComida);
        final List<String> nombresRecetas = new ArrayList<>();

        if (recetasDelTipo != null && !recetasDelTipo.isEmpty()) {
            nombresRecetas.addAll(recetasDelTipo.keySet());
        } else {
            nombresRecetas.add("No hay recetas disponibles");
            // ESTO ES IMPORTANTE: Te dirá si el fallo es que la caja está vacía
            Toast.makeText(getContext(), "La categoría '" + tipoComida + "' está vacía", Toast.LENGTH_SHORT).show();
        }

        android.widget.ListView lista = customView.findViewById(R.id.listRecetas);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_list_item_1,
                nombresRecetas
        );
        lista.setAdapter(adapter);

        android.widget.Button btnNueva = customView.findViewById(R.id.btnNewRecipe);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(customView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        lista.setOnItemClickListener((parent, view, position, id) -> {
            String recetaElegida = nombresRecetas.get(position);
            // Evitamos seleccionar el texto de aviso
            if (!recetaElegida.equals("No hay recetas disponibles")) {
                cajon.setText(recetaElegida);
                menuSemanal.put(dia + "_" + tipoComida, recetaElegida);
                dialog.dismiss();
            }
        });

        btnNueva.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Función crear receta pendiente...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void procesarRecetas(List<RecetaModelo> lista) {
        // 1. Limpiamos datos viejos
        libroDeRecetas.clear();

        // 2. Inicializamos las cajas (Claves en Inglés para coincidir con WeekAdapter)
        libroDeRecetas.put("Breakfast", new HashMap<>());
        libroDeRecetas.put("Lunch", new HashMap<>());
        libroDeRecetas.put("Dinner", new HashMap<>());

        int contadorRecetas = 0; // Para saber cuántas hemos cargado

        // 3. Recorremos lo que viene de Supabase
        for (RecetaModelo r : lista) {

            // --- TRADUCTOR A PRUEBA DE BALAS ---
            String catEnBaseDatos = r.getCategoria();

            // Si la categoría viene vacía, pasamos a la siguiente
            if (catEnBaseDatos == null) continue;

            // Limpiamos espacios y pasamos a minúsculas para comparar fácil
            String catLimpia = catEnBaseDatos.trim().toLowerCase();



            // Buscamos la caja correcta
            Map<String, List<String>> categoriaMap = libroDeRecetas.get(catLimpia);

            if (categoriaMap != null) {
                // Sacamos los nombres de los ingredientes
                List<String> listaSoloNombres = new ArrayList<>();
                if (r.getIngredientes() != null) {
                    for (RecetaModelo.Ingrediente item : r.getIngredientes()) {
                        if (item.getNombre() != null) {
                            listaSoloNombres.add(item.getNombre());
                        }
                    }
                }
                // Guardamos la receta
                categoriaMap.put(r.getNombre(), listaSoloNombres);
                contadorRecetas++;
            }
        }

        // --- MENSAJE DE CONTROL ---
        Toast.makeText(getContext(), "Cargadas " + contadorRecetas + " recetas desde Supabase", Toast.LENGTH_LONG).show();
    }

    private void cargarDatosDesdeSupabase() {
        // Obtenemos la API
        SupabaseApi api = SupabaseClient.getApi();

        // Preparamos la llamada. Authorization necesita "Bearer " delante de la clave
        Call<List<RecetaModelo>> llamada = api.obtenerRecetas(
                SupabaseClient.SUPABASE_KEY,
                "Bearer " + SupabaseClient.SUPABASE_KEY
        );

        // Hacemos la llamada en segundo plano (enqueue)
        llamada.enqueue(new Callback<List<RecetaModelo>>() {
            @Override
            public void onResponse(Call<List<RecetaModelo>> call, Response<List<RecetaModelo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<RecetaModelo> recetasDescargadas = response.body();
                    procesarRecetas(recetasDescargadas);
                } else {
                    // --- CAMBIA ESTO PARA VER EL ERROR REAL ---
                    try {
                        // Esto nos dirá si es 404 (No encontrado), 401 (Permiso), etc.
                        String errorMsg = "Error " + response.code() + ": " + response.message();
                        if (response.errorBody() != null) {
                            errorMsg += "\n" + response.errorBody().string();
                        }
                        // Usamos LENGTH_LONG para que te dé tiempo a leerlo
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                        // También lo imprimimos en la consola de abajo (Logcat)
                        System.out.println("ERROR SUPABASE: " + errorMsg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<RecetaModelo>> call, Throwable t) {
                // --- CAMBIA ESTO TAMBIÉN ---
                Toast.makeText(getContext(), "FALLO TÉCNICO: " + t.getMessage(), Toast.LENGTH_LONG).show();
                t.printStackTrace();
            }
        });
    }

    private void cargarDatosEjemplo() {

        // --- DESAYUNOS (Breakfast) ---
        Map<String, List<String>> desayunos = new HashMap<>();
        desayunos.put("Tostadas con Aguacate", Arrays.asList("Pan integral", "Aguacate", "Sal", "Aceite"));
        desayunos.put("Avena con Frutas", Arrays.asList("Avena", "Leche", "Plátano", "Fresas"));
        desayunos.put("Tortilla Francesa", Arrays.asList("Huevos", "Sal", "Aceite"));
        libroDeRecetas.put("Breakfast", desayunos); // Usamos la clave en inglés porque así la manda el Adapter

        // --- COMIDAS (Lunch) ---
        Map<String, List<String>> comidas = new HashMap<>();
        comidas.put("Pollo al Curry", Arrays.asList("Pechuga de pollo", "Curry", "Arroz basmati", "Cebolla"));
        comidas.put("Lentejas Estofadas", Arrays.asList("Lentejas", "Chorizo", "Zanahoria", "Patata"));
        comidas.put("Ensalada César", Arrays.asList("Lechuga", "Pollo", "Picatostes", "Salsa César", "Queso"));
        libroDeRecetas.put("Lunch", comidas);

        // --- CENAS (Diner) ---
        Map<String, List<String>> cenas = new HashMap<>();
        cenas.put("Merluza a la Plancha", Arrays.asList("Merluza", "Ajo", "Perejil", "Limón"));
        cenas.put("Sopa de Verduras", Arrays.asList("Caldo", "Fideos", "Zanahoria", "Puerro"));
        cenas.put("Yogur con Nueces", Arrays.asList("Yogur Griego", "Nueces", "Miel"));
        libroDeRecetas.put("Diner", cenas);
    }

    private void generarYMostrarListaCompra() {
        // 1. RECOPILAR INGREDIENTES
        // Usamos un Set para que NO se repitan (si dos recetas piden "Sal", que salga solo una vez)
        java.util.Set<String> listaIngredientes = new java.util.HashSet<>();

        // Recorremos todo lo que el usuario ha planeado (menuSemanal)
        for (String nombreReceta : menuSemanal.values()) {

            // Buscamos esa receta en todas las categorías del libro (Desayuno, Comida, Cena)
            for (Map<String, List<String>> categoria : libroDeRecetas.values()) {
                if (categoria.containsKey(nombreReceta)) {
                    // ¡Encontrada! Añadimos sus ingredientes a la lista de compra
                    List<String> ingredientes = categoria.get(nombreReceta);
                    if (ingredientes != null) {
                        listaIngredientes.addAll(ingredientes);
                    }
                }
            }
        }

        if (listaIngredientes.isEmpty()) {
            Toast.makeText(getContext(), "Primero añade comidas al menú", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. PREPARAR LA VENTANA DE CHECKBOXES
        // Convertimos el Set a Array para que lo entienda el Dialog
        String[] ingredientesArray = listaIngredientes.toArray(new String[0]);
        // Array de booleanos para saber cuáles están marcados (al principio ninguno)
        boolean[] marcados = new boolean[ingredientesArray.length];

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Lista de la Compra");

        // Esta función crea automáticamente una lista con checkboxes
        builder.setMultiChoiceItems(ingredientesArray, marcados, (dialog, which, isChecked) -> {
            // Aquí puedes hacer algo cuando el usuario marca/desmarca
            marcados[which] = isChecked;
        });

        builder.setPositiveButton("Cerrar", null);

        // Opcional: Botón para copiar o enviar
        builder.setNeutralButton("Compartir", (dialog, which) -> {
            // Aquí podrías poner código para enviar por WhatsApp
            Toast.makeText(getContext(), "Función de compartir (pendiente)", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}