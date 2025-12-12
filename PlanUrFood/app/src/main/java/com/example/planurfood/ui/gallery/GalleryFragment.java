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

        adapter = new PantryAdapter(itemsParaMostrar);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = binding.fabAddPantryItem;
        fab.setOnClickListener(v -> mostrarDialogoAñadir());

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        guardarDatosEnArchivo();
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarDatosDesdeArchivo() {
        FileInputStream fis = null;
        try {
            fis = requireActivity().openFileInput(ARCHIVO_JSON);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String jsonString = sb.toString();
            JSONObject rootObject = new JSONObject(jsonString);

            despensaMap = new HashMap<>();
            Iterator<String> keys = rootObject.keys();

            while (keys.hasNext()) {
                String categoria = keys.next();
                JSONArray jsonArray = rootObject.getJSONArray(categoria);
                List<PantryItem> listaItems = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject itemObj = jsonArray.getJSONObject(i);

                    String nombre = itemObj.getString("nombre");
                    String cantidad = itemObj.getString("cantidad");
                    int imagenId = itemObj.getInt("imagen");

                    listaItems.add(new PantryItem(nombre, cantidad, imagenId));
                }
                despensaMap.put(categoria, listaItems);
            }

        } catch (IOException | JSONException e) {
            despensaMap = new HashMap<>();
            despensaMap.put("Frutas y Verduras", new ArrayList<>());
            despensaMap.put("Lácteos y Huevos", new ArrayList<>());
            despensaMap.put("Despensa General", new ArrayList<>());
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (IOException e) { e.printStackTrace(); }
            }
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

    private void mostrarDialogoAñadir() {
        View customView = getLayoutInflater().inflate(R.layout.dialog_add_pantry, null);

        android.widget.AutoCompleteTextView inputName = customView.findViewById(R.id.inputFoodName);
        TextInputEditText inputQty = customView.findViewById(R.id.inputFoodQty);
        Spinner spinner = customView.findViewById(R.id.spinnerCategory);
        View btnGuardar = customView.findViewById(R.id.btnAddItem);

        List<String> nombresComida = FoodResources.getNombresDisponibles();
        ArrayAdapter<String> autoAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                nombresComida
        );
        inputName.setAdapter(autoAdapter);

        List<String> categorias = new ArrayList<>(despensaMap.keySet());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categorias);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(customView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnGuardar.setOnClickListener(v -> {
            String nombre = inputName.getText().toString();
            String cantidad = inputQty.getText().toString();
            String categoriaElegida = "";
            if(spinner.getSelectedItem() != null) {
                categoriaElegida = spinner.getSelectedItem().toString();
            }

            if (!nombre.isEmpty() && !categoriaElegida.isEmpty()) {
                int iconoCorrecto = FoodResources.getIconoPara(nombre);
                PantryItem nuevoItem = new PantryItem(nombre, cantidad, iconoCorrecto);

                if (despensaMap.get(categoriaElegida) != null) {
                    despensaMap.get(categoriaElegida).add(nuevoItem);
                }

                actualizarListaVisual();
                guardarDatosEnArchivo();
                dialog.dismiss();
                Toast.makeText(getContext(), "Guardado: " + nombre, Toast.LENGTH_SHORT).show();
            } else {
                inputName.setError("Revisa los campos");
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