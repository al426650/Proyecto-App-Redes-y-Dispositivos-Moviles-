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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planurfood.R;
import com.example.planurfood.databinding.FragmentGalleryBinding;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

        adapter = new PantryAdapter(itemsParaMostrar, this::mostrarDialogo);
        recyclerView.setAdapter(adapter);

        configurarSwipe(recyclerView);

        FloatingActionButton fab = binding.fabAddPantryItem;
        fab.setOnClickListener(v -> mostrarDialogo(null));

        return root;
    }

    private void configurarSwipe(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Object objeto = adapter.getItemAt(position);
                if (objeto instanceof PantryItem) {
                    eliminarItem((PantryItem) objeto);
                } else {
                    adapter.notifyItemChanged(position);
                }
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void eliminarItem(PantryItem itemABorrar) {
        boolean borrado = false;
        for (List<PantryItem> lista : despensaMap.values()) {
            if (lista.remove(itemABorrar)) {
                borrado = true;
                break;
            }
        }
        if (borrado) {
            guardarDatosEnArchivo();
            actualizarListaVisual();
            Toast.makeText(getContext(), "Deleted: " + itemABorrar.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogo(PantryItem itemAEditar) {
        View customView = getLayoutInflater().inflate(R.layout.dialog_add_pantry, null);

        android.widget.AutoCompleteTextView inputName = customView.findViewById(R.id.inputFoodName);
        TextInputEditText inputQty = customView.findViewById(R.id.inputFoodQty);
        MaterialButtonToggleGroup toggleGroup = customView.findViewById(R.id.toggleUnit);
        Spinner spinner = customView.findViewById(R.id.spinnerCategory);
        View btnGuardar = customView.findViewById(R.id.btnAddItem);

        List<String> nombresComida = FoodResources.getAvailableNames();
        ArrayAdapter<String> autoAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, nombresComida);
        inputName.setAdapter(autoAdapter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, FoodResources.CATEGORIES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        inputName.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFood = (String) parent.getItemAtPosition(position);
            String autoCategory = FoodResources.getCategoryFor(selectedFood);
            int spinnerPos = spinnerAdapter.getPosition(autoCategory);
            if (spinnerPos >= 0) spinner.setSelection(spinnerPos);
        });

        if (itemAEditar != null) {
            inputName.setText(itemAEditar.getName());
            double cantidadNumerica = extractNumber(itemAEditar.getQuantity());
            if (cantidadNumerica <= 0) cantidadNumerica = 0;

            if (cantidadNumerica == (long) cantidadNumerica) {
                inputQty.setText(String.valueOf((long) cantidadNumerica));
            } else {
                inputQty.setText(String.valueOf(cantidadNumerica));
            }

            String unidadTexto = extractUnit(itemAEditar.getQuantity());
            if (unidadTexto.toLowerCase().contains("ud")) {
                toggleGroup.check(R.id.btnUnits);
            } else {
                toggleGroup.check(R.id.btnGrams);
            }

            String catActual = encontrarCategoriaDe(itemAEditar);
            if (catActual != null) {
                int spinnerPosition = spinnerAdapter.getPosition(catActual);
                if (spinnerPosition >= 0) spinner.setSelection(spinnerPosition);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(customView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnGuardar.setOnClickListener(v -> {
            String rawInput = inputName.getText().toString();
            String nombre = FoodResources.getSingularName(rawInput);
            String cantidadStr = inputQty.getText().toString();
            String categoriaElegida = (spinner.getSelectedItem() != null) ? spinner.getSelectedItem().toString() : "Others";

            // --- LÓGICA DE UNIDADES (RESPETANDO USUARIO + CORRECCIÓN) ---
            String unidadSeleccionada;
            if (toggleGroup.getCheckedButtonId() == R.id.btnUnits) {
                unidadSeleccionada = "ud.";
            } else {
                // Si elige "g", corregimos a "ml" si es líquido
                if (FoodResources.isLiquid(nombre)) {
                    unidadSeleccionada = "ml";
                } else {
                    unidadSeleccionada = "g";
                }
            }

            if (!nombre.isEmpty() && !cantidadStr.isEmpty()) {
                if (itemAEditar != null) eliminarDeMemoria(itemAEditar);

                int iconoCorrecto = FoodResources.getIconFor(nombre);
                if (!despensaMap.containsKey(categoriaElegida)) {
                    despensaMap.put(categoriaElegida, new ArrayList<>());
                }

                List<PantryItem> listaActual = despensaMap.get(categoriaElegida);
                boolean found = false;

                for (PantryItem item : listaActual) {
                    if (item.getName().equalsIgnoreCase(nombre)) {
                        double currentQty = extractNumber(item.getQuantity());
                        double inputQtyNum = extractNumber(cantidadStr);

                        double total = (itemAEditar == null) ? (currentQty + inputQtyNum) : inputQtyNum;

                        if (total <= 0) {
                            listaActual.remove(item);
                            Toast.makeText(getContext(), "Removed (Qty <= 0)", Toast.LENGTH_SHORT).show();
                        } else {
                            String finalString = (total == (long) total) ? (long)total + " " + unidadSeleccionada : String.format("%.1f %s", total, unidadSeleccionada);
                            item.setQuantity(finalString.trim());
                        }
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    double inputQtyNum = extractNumber(cantidadStr);
                    if (inputQtyNum > 0) {
                        String cantidadFinal = cantidadStr + " " + unidadSeleccionada;
                        listaActual.add(new PantryItem(nombre, cantidadFinal, iconoCorrecto));
                    }
                }

                actualizarListaVisual();
                guardarDatosEnArchivo();
                dialog.dismiss();
            } else {
                if (nombre.isEmpty()) inputName.setError("Required");
                if (cantidadStr.isEmpty()) inputQty.setError("Required");
            }
        });

        dialog.show();
    }

    private void eliminarDeMemoria(PantryItem item) {
        for (List<PantryItem> lista : despensaMap.values()) {
            if (lista.remove(item)) return;
        }
    }

    private String encontrarCategoriaDe(PantryItem item) {
        for (Map.Entry<String, List<PantryItem>> entry : despensaMap.entrySet()) {
            if (entry.getValue().contains(item)) return entry.getKey();
        }
        return null;
    }

    private double extractNumber(String text) {
        try {
            Matcher matcher = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+").matcher(text);
            if (matcher.find()) return Double.parseDouble(matcher.group());
        } catch (Exception e) { }
        return 0.0;
    }

    private String extractUnit(String text) {
        return text.replaceAll("[-+0-9.]", "").trim();
    }

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
            despensaMap.put("Pantry", new ArrayList<>());
        }
        actualizarListaVisual();
    }

    private void actualizarListaVisual() {
        itemsParaMostrar.clear();
        for (Map.Entry<String, List<PantryItem>> entry : despensaMap.entrySet()) {
            String categoria = entry.getKey();
            List<PantryItem> todosLosItems = entry.getValue();
            List<PantryItem> itemsVisibles = new ArrayList<>();

            // FILTRO: Solo mostrar positivos
            for (PantryItem item : todosLosItems) {
                double cantidad = extractNumber(item.getQuantity());
                if (cantidad > 0) itemsVisibles.add(item);
            }

            if (!itemsVisibles.isEmpty()) {
                itemsParaMostrar.add(categoria);
                itemsParaMostrar.addAll(itemsVisibles);
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