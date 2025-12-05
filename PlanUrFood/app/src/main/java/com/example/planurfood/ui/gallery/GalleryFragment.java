package com.example.planurfood.ui.gallery;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.planurfood.R;
import com.example.planurfood.databinding.FragmentGalleryBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;

    // VARIABLES GLOBALES
    private List<Object> itemsParaMostrar = new ArrayList<>();
    private PantryAdapter adapter;
    // Ahora el mapa es global para poder añadirle cosas luego
    private Map<String, List<PantryItem>> despensaMap = new HashMap<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 1. Cargar datos iniciales
        cargarDatosEjemplo();

        // 2. Configurar RecyclerView
        RecyclerView recyclerView = binding.recyclerPantry;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Creamos el adapter una vez y lo guardamos
        adapter = new PantryAdapter(itemsParaMostrar);
        recyclerView.setAdapter(adapter);

        // 3. Botón flotante (+)
        FloatingActionButton fab = binding.fabAddPantryItem;
        fab.setOnClickListener(v -> mostrarDialogoAñadir());

        return root;
    }

    private void cargarDatosEjemplo() {
        // Solo cargamos datos si el mapa está vacío (para no duplicar al rotar pantalla)
        if (despensaMap.isEmpty()) {
            // Categoría: Frutas y Verduras
            List<PantryItem> frutas = new ArrayList<>();
            frutas.add(new PantryItem("Manzanas", "6 uds", R.drawable.manzana)); // Asegúrate de tener este icono o usa uno por defecto
            frutas.add(new PantryItem("Zanahorias", "1 kg", R.drawable.zanahoria));
            despensaMap.put("Frutas y Verduras", frutas);

            // Categoría: Lácteos
            List<PantryItem> lacteos = new ArrayList<>();
            lacteos.add(new PantryItem("Leche", "2 L", R.drawable.leche));
            lacteos.add(new PantryItem("Huevos", "12", R.drawable.huevos));
            despensaMap.put("Lácteos y Huevos", lacteos);

            // Creamos una categoría vacía "Otros" por si acaso
            despensaMap.put("Despensa General", new ArrayList<>());
        }

        actualizarListaVisual();
    }

    // Este método aplana el mapa y actualiza la pantalla
    private void actualizarListaVisual() {
        itemsParaMostrar.clear();
        for (Map.Entry<String, List<PantryItem>> entry : despensaMap.entrySet()) {
            // Si la categoría tiene cosas, la mostramos
            if (!entry.getValue().isEmpty()) {
                itemsParaMostrar.add(entry.getKey()); // Cabecera
                itemsParaMostrar.addAll(entry.getValue()); // Productos
            }
        }
        // Si el adapter ya existe, le avisamos del cambio
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void mostrarDialogoAñadir() {
        // 1. Inflar el diseño del diálogo
        View customView = getLayoutInflater().inflate(R.layout.dialog_add_pantry, null);

        android.widget.AutoCompleteTextView inputName = customView.findViewById(R.id.inputFoodName);
        TextInputEditText inputQty = customView.findViewById(R.id.inputFoodQty);
        Spinner spinner = customView.findViewById(R.id.spinnerCategory);
        View btnGuardar = customView.findViewById(R.id.btnAddItem);

        // A. Conseguimos la lista de nombres de tu diccionario (asegúrate de haber creado este método en FoodResources)
        List<String> nombresComida = FoodResources.getNombresDisponibles();

        // B. Creamos el adaptador para las sugerencias
        ArrayAdapter<String> autoAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line, // Diseño estándar de lista desplegable
                nombresComida
        );

        // C. Se lo enchufamos al campo de texto
        inputName.setAdapter(autoAdapter);

        // ------------------------------------------------------

        // 2. Rellenar el Spinner con las categorías (ESTO YA LO TENÍAS Y ESTÁ BIEN)
        List<String> categorias = new ArrayList<>(despensaMap.keySet());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categorias);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        // 3. Crear la alerta
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(customView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 4. Lógica del botón Guardar
        btnGuardar.setOnClickListener(v -> {
            String nombre = inputName.getText().toString();
            String cantidad = inputQty.getText().toString();
            String categoriaElegida = spinner.getSelectedItem().toString();

            if (!nombre.isEmpty()) {
                int iconoCorrecto = FoodResources.getIconoPara(nombre);
                PantryItem nuevoItem = new PantryItem(nombre, cantidad, iconoCorrecto);

                if (despensaMap.get(categoriaElegida) != null) {
                    despensaMap.get(categoriaElegida).add(nuevoItem);
                }

                actualizarListaVisual();
                dialog.dismiss();
                Toast.makeText(getContext(), "Guardado: " + nombre, Toast.LENGTH_SHORT).show();
            } else {
                inputName.setError("Escribe un nombre");
            }
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}