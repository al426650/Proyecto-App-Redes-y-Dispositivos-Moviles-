package com.example.planurfood.ui.gallery;

import static android.content.Context.MODE_PRIVATE;

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
import androidx.recyclerview.widget.ItemTouchHelper; // Importante para el swipe
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planurfood.R;
import com.example.planurfood.databinding.FragmentGalleryBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private List<Object> itemsParaMostrar = new ArrayList<>();
    private PantryAdapter adapter;
    private Map<String, List<PantryItem>> despensaMap = new HashMap<>();
    private static final String ARCHIVO_JSON = "midestpensa.json";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        cargarDatosDesdeArchivo();

        RecyclerView recyclerView = binding.recyclerPantry;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Pasamos el listener (this::mostrarDialogo) al Adapter para editar al hacer clic
        adapter = new PantryAdapter(itemsParaMostrar, this::mostrarDialogo);
        recyclerView.setAdapter(adapter);

        // CONFIGURAR SWIPE (Deslizar para borrar)
        configurarSwipe(recyclerView);

        FloatingActionButton fab = binding.fabAddPantryItem;
        // Al dar al más, llamamos al diálogo con null (modo añadir)
        fab.setOnClickListener(v -> mostrarDialogo(null));

        return root;
    }

    // --- LÓGICA DE BORRADO (SWIPE) ---
    private void configurarSwipe(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // No queremos mover, solo deslizar
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Object objeto = adapter.getItemAt(position);

                // Solo borramos si es un producto, no una cabecera
                if (objeto instanceof PantryItem) {
                    eliminarItem((PantryItem) objeto);
                } else {
                    // Si intenta borrar una cabecera, recargamos para que vuelva a su sitio
                    adapter.notifyItemChanged(position);
                }
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void eliminarItem(PantryItem itemABorrar) {
        boolean borrado = false;
        // Buscamos en qué categoría está y lo borramos
        for (List<PantryItem> lista : despensaMap.values()) {
            if (lista.remove(itemABorrar)) {
                borrado = true;
                break;
            }
        }

        if (borrado) {
            guardarDatosEnArchivo();
            actualizarListaVisual();
            Toast.makeText(getContext(), "Eliminado: " + itemABorrar.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- LÓGICA DE DIÁLOGO (AÑADIR Y EDITAR) ---
    // Ahora recibe un parámetro. Si es null = AÑADIR. Si tiene datos = EDITAR.
    private void mostrarDialogo(PantryItem itemAEditar) {
        View customView = getLayoutInflater().inflate(R.layout.dialog_add_pantry, null);

        android.widget.AutoCompleteTextView inputName = customView.findViewById(R.id.inputFoodName);
        TextInputEditText inputQty = customView.findViewById(R.id.inputFoodQty);
        Spinner spinner = customView.findViewById(R.id.spinnerCategory);
        View btnGuardar = customView.findViewById(R.id.btnAddItem);

        // Configurar Autocompletado
        List<String> nombresComida = FoodResources.getNombresDisponibles();
        ArrayAdapter<String> autoAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, nombresComida);
        inputName.setAdapter(autoAdapter);

        // Configurar Spinner Categorías
        List<String> categorias = new ArrayList<>(despensaMap.keySet());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categorias);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        // --- SI ESTAMOS EN MODO EDICIÓN, RELLENAMOS LOS DATOS ---
        if (itemAEditar != null) {
            inputName.setText(itemAEditar.getName());
            inputQty.setText(itemAEditar.getQuantity());

            // Buscar la categoría del item para seleccionarla en el spinner
            String catActual = encontrarCategoriaDe(itemAEditar);
            if (catActual != null) {
                int spinnerPosition = spinnerAdapter.getPosition(catActual);
                spinner.setSelection(spinnerPosition);
            }
        }
        // ---------------------------------------------------------

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(customView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnGuardar.setOnClickListener(v -> {
            String nombre = inputName.getText().toString().trim();
            String cantidadInput = inputQty.getText().toString();
            String categoriaElegida = (spinner.getSelectedItem() != null) ? spinner.getSelectedItem().toString() : "Despensa General";

            if (!nombre.isEmpty()) {
                // 1. Si estábamos editando, PRIMERO BORRAMOS el antiguo
                // (Esto maneja automáticamente si cambiaste de categoría)
                if (itemAEditar != null) {
                    eliminarDeMemoria(itemAEditar);
                }

                // 2. Lógica de añadir/sumar (igual que antes)
                int iconoCorrecto = FoodResources.getIconoPara(nombre);
                if (!despensaMap.containsKey(categoriaElegida)) {
                    despensaMap.put(categoriaElegida, new ArrayList<>());
                }

                List<PantryItem> listaActual = despensaMap.get(categoriaElegida);
                boolean found = false;

                for (PantryItem item : listaActual) {
                    if (item.getName().equalsIgnoreCase(nombre)) {
                        double currentQty = extractNumber(item.getQuantity());
                        double addedQty = extractNumber(cantidadInput);
                        double total = currentQty + addedQty;
                        String unit = extractUnit(item.getQuantity());
                        if(unit.isEmpty()) unit = extractUnit(cantidadInput);

                        String finalString = (total == (long) total) ? (long)total + " " + unit : String.format("%.1f %s", total, unit);
                        item.setQuantity(finalString.trim());
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    listaActual.add(new PantryItem(nombre, cantidadInput, iconoCorrecto));
                }

                actualizarListaVisual();
                guardarDatosEnArchivo();
                dialog.dismiss();
                Toast.makeText(getContext(), (itemAEditar != null ? "Editado: " : "Guardado: ") + nombre, Toast.LENGTH_SHORT).show();
            } else {
                inputName.setError("Revisa los campos");
            }
        });

        dialog.show();
    }

    // Helper para borrar sin guardar en disco (usado durante la edición)
    private void eliminarDeMemoria(PantryItem item) {
        for (List<PantryItem> lista : despensaMap.values()) {
            if (lista.remove(item)) return;
        }
    }

    // Helper para encontrar la categoría de un item
    private String encontrarCategoriaDe(PantryItem item) {
        for (Map.Entry<String, List<PantryItem>> entry : despensaMap.entrySet()) {
            if (entry.getValue().contains(item)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // --- HELPERS MATEMÁTICOS ---
    private double extractNumber(String text) {
        try {
            Matcher matcher = Pattern.compile("[0-9]+(\\.[0-9]+)?").matcher(text);
            if (matcher.find()) return Double.parseDouble(matcher.group());
        } catch (Exception e) { }
        return 0.0;
    }

    private String extractUnit(String text) {
        return text.replaceAll("[0-9.]", "").trim();
    }

    // --- GUARDADO Y CARGA (SIN CAMBIOS) ---
    private void guardarDatosEnArchivo() {
        JSONObject rootObject = new JSONObject();
        try {
            for (Map.Entry<String, List<PantryItem>> entry : despensaMap.entrySet()) {
                String categoria = entry.getKey();
                List<PantryItem> listaItems = entry.getValue();
                JSONArray jsonArray = new JSONArray();
                for (PantryItem item : listaItems) {
                    JSONObject itemJson = new JSONObject();
                    itemJson.put("nombre", item.getName());
                    itemJson.put("cantidad", item.getQuantity());
                    itemJson.put("imagen", item.getIconResId());
                    jsonArray.put(itemJson);
                }
                rootObject.put(categoria, jsonArray);
            }
            FileOutputStream fos = requireActivity().openFileOutput(ARCHIVO_JSON, MODE_PRIVATE);
            fos.write(rootObject.toString().getBytes());
            fos.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void cargarDatosDesdeArchivo() {
        try {
            FileInputStream fis = requireActivity().openFileInput(ARCHIVO_JSON);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            JSONObject rootObject = new JSONObject(sb.toString());
            despensaMap = new HashMap<>();
            Iterator<String> keys = rootObject.keys();
            while (keys.hasNext()) {
                String categoria = keys.next();
                JSONArray jsonArray = rootObject.getJSONArray(categoria);
                List<PantryItem> listaItems = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject itemObj = jsonArray.getJSONObject(i);
                    listaItems.add(new PantryItem(itemObj.getString("nombre"), itemObj.getString("cantidad"), itemObj.getInt("imagen")));
                }
                despensaMap.put(categoria, listaItems);
            }
        } catch (Exception e) {
            despensaMap = new HashMap<>();
            despensaMap.put("Despensa General", new ArrayList<>());
        }
        actualizarListaVisual();
    }

    private void actualizarListaVisual() {
        itemsParaMostrar.clear();
        for (Map.Entry<String, List<PantryItem>> entry : despensaMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                itemsParaMostrar.add(entry.getKey());
                itemsParaMostrar.addAll(entry.getValue());
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        guardarDatosEnArchivo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}