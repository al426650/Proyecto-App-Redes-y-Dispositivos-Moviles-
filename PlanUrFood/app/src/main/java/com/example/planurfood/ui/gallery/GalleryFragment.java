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
import java.util.Arrays;
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

    // Archivos
    private static final String ARCHIVO_PANTRY = "midestpensa.json";
    private static final String ARCHIVO_PLAN = "plan_semanal.json";
    private static final String ARCHIVO_COMPRA = "lista_compra.json";
    private static final String ARCHIVO_RECETAS_CACHE = "recetas_cache.json";

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

        // Listener Añadir Manual
        binding.fabAddPantryItem.setOnClickListener(v -> mostrarDialogo(null));

        // Listener Cámara (Nuevo)
        binding.fabCameraUpdate.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Próximamente: Escanear despensa con IA", Toast.LENGTH_SHORT).show();
        });

        return root;
    }

    // =========================================================================
    // LÓGICA DE AÑADIR Y ASIGNACIÓN AUTOMÁTICA
    // =========================================================================

    private void mostrarDialogo(PantryItem itemAEditar) {
        View customView = getLayoutInflater().inflate(R.layout.dialog_add_pantry, null);

        android.widget.AutoCompleteTextView inputName = customView.findViewById(R.id.inputFoodName);
        TextInputEditText inputQty = customView.findViewById(R.id.inputFoodQty);
        MaterialButtonToggleGroup toggleGroup = customView.findViewById(R.id.toggleUnit);
        Spinner spinner = customView.findViewById(R.id.spinnerCategory);
        View btnGuardar = customView.findViewById(R.id.btnAddItem);

        List<String> nombresComida = FoodResources.getAvailableNames();
        inputName.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, nombresComida));
        spinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, FoodResources.CATEGORIES));

        inputName.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFood = (String) parent.getItemAtPosition(position);
            String autoCategory = FoodResources.getCategoryFor(selectedFood);
            // Selección automática de categoría...
        });

        if (itemAEditar != null) {
            inputName.setText(itemAEditar.getName());
            double cant = extractNumber(itemAEditar.getQuantity());
            inputQty.setText(cant == (long)cant ? String.valueOf((long)cant) : String.valueOf(cant));
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(customView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnGuardar.setOnClickListener(v -> {
            String rawInput = inputName.getText().toString();
            String nombre = FoodResources.getSingularName(rawInput);
            String cantidadStr = inputQty.getText().toString();
            String categoriaElegida = (spinner.getSelectedItem() != null) ? spinner.getSelectedItem().toString() : "Others";
            String unidadSeleccionada = (toggleGroup.getCheckedButtonId() == R.id.btnUnits) ? "ud." : (FoodResources.isLiquid(nombre) ? "ml" : "g");

            if (!nombre.isEmpty() && !cantidadStr.isEmpty()) {
                if (itemAEditar != null) eliminarDeMemoria(itemAEditar);

                if (!despensaMap.containsKey(categoriaElegida)) despensaMap.put(categoriaElegida, new ArrayList<>());
                List<PantryItem> listaActual = despensaMap.get(categoriaElegida);

                boolean found = false;
                double cantidadAñadida = extractNumber(cantidadStr);

                // Guardamos en memoria
                for (PantryItem item : listaActual) {
                    if (item.getName().equalsIgnoreCase(nombre)) {
                        double currentQty = extractNumber(item.getQuantity());
                        double total = (itemAEditar == null) ? (currentQty + cantidadAñadida) : cantidadAñadida;
                        String finalString = (total == (long) total) ? (long)total + " " + unidadSeleccionada : String.format("%.1f %s", total, unidadSeleccionada);
                        item.setQuantity(finalString);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    String cantidadFinal = cantidadStr + " " + unidadSeleccionada;
                    listaActual.add(new PantryItem(nombre, cantidadFinal, FoodResources.getIconFor(nombre)));
                }

                actualizarListaVisual();
                guardarDatosEnArchivo();

                // AUTOMÁTICO: Si añadimos cantidad, intentamos asignar al plan
                if (itemAEditar == null || cantidadAñadida > 0) {
                    distribuirIngredienteAutomaticamente(nombre, cantidadAñadida);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void distribuirIngredienteAutomaticamente(String nombreIngrediente, double cantidadAñadida) {
        String nombreNorm = limpiarNombre(nombreIngrediente);

        JSONObject planSemanal = leerJSON(ARCHIVO_PLAN);
        JSONObject recetasCache = leerJSON(ARCHIVO_RECETAS_CACHE);
        JSONObject listaCompra = leerJSON(ARCHIVO_COMPRA);

        List<String> ordenDias = Arrays.asList("MONDAY_Breakfast", "MONDAY_Lunch", "MONDAY_Dinner", "TUESDAY_Breakfast", "TUESDAY_Lunch", "TUESDAY_Dinner", "WEDNESDAY_Breakfast", "WEDNESDAY_Lunch", "WEDNESDAY_Dinner", "THURSDAY_Breakfast", "THURSDAY_Lunch", "THURSDAY_Dinner", "FRIDAY_Breakfast", "FRIDAY_Lunch", "FRIDAY_Dinner", "SATURDAY_Breakfast", "SATURDAY_Lunch", "SATURDAY_Dinner", "SUNDAY_Breakfast", "SUNDAY_Lunch", "SUNDAY_Dinner");

        double cantidadRestante = cantidadAñadida;
        boolean huboCambiosCompra = false;
        boolean huboCambiosPantry = false;
        String mensajeAccion = "";

        for (String diaKey : ordenDias) {
            if (cantidadRestante <= 0) break;

            if (planSemanal.has(diaKey)) {
                try {
                    String nombreReceta = planSemanal.getString(diaKey);
                    String tipo = diaKey.split("_")[1].toLowerCase();

                    if (recetasCache.has(tipo) && recetasCache.getJSONObject(tipo).has(nombreReceta)) {
                        JSONArray ings = recetasCache.getJSONObject(tipo).getJSONArray(nombreReceta);

                        for (int i = 0; i < ings.length(); i++) {
                            String lineaIng = ings.getString(i);
                            String ingNorm = limpiarNombre(lineaIng);

                            if (ingNorm.equals(nombreNorm)) {
                                if (estaEnListaCompra(listaCompra, nombreNorm)) {

                                    double necesito = extractNumber(lineaIng);
                                    if (necesito <= 0) necesito = 1.0;

                                    double aGastar = Math.min(cantidadRestante, necesito);

                                    restarDeListaCompra(listaCompra, nombreNorm, aGastar);
                                    huboCambiosCompra = true;

                                    restarDeDespensaMemoriaYArchivo(nombreNorm, aGastar);
                                    huboCambiosPantry = true;

                                    cantidadRestante -= aGastar;
                                    String diaLimpio = diaKey.split("_")[0];
                                    mensajeAccion = "Asignado a " + nombreReceta + " (" + diaLimpio + ")";

                                    if (cantidadRestante <= 0) break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        }
        if (huboCambiosCompra) guardarJSON(ARCHIVO_COMPRA, listaCompra);
        if (!mensajeAccion.isEmpty()) Toast.makeText(getContext(), mensajeAccion, Toast.LENGTH_LONG).show();
    }

    private void restarDeDespensaMemoriaYArchivo(String nombreNorm, double cantidad) {
        boolean modificado = false;
        outerLoop:
        for (List<PantryItem> lista : despensaMap.values()) {
            Iterator<PantryItem> iter = lista.iterator();
            while (iter.hasNext()) {
                PantryItem item = iter.next();
                if (limpiarNombre(item.getName()).equals(nombreNorm)) {
                    double actual = extractNumber(item.getQuantity());
                    double nuevo = actual - cantidad;

                    if (nuevo <= 0.01) iter.remove();
                    else {
                        String unidad = FoodResources.getAutoUnit(nombreNorm, nuevo);
                        String cantStr = (nuevo == (long)nuevo) ? String.valueOf((long)nuevo) : String.valueOf(nuevo);
                        item.setQuantity(cantStr + " " + unidad);
                    }
                    modificado = true;
                    break outerLoop;
                }
            }
        }
        if (modificado) {
            actualizarListaVisual();
            guardarDatosEnArchivo();
        }
    }

    // Helpers
    private boolean estaEnListaCompra(JSONObject jsonCompra, String nombreNorm) {
        try {
            if (!jsonCompra.has("items")) return false;
            JSONArray items = jsonCompra.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                if (limpiarNombre(items.getJSONObject(i).getString("nombre")).equals(nombreNorm))
                    return items.getJSONObject(i).getDouble("cantidadNum") > 0;
            }
        } catch (Exception e) {}
        return false;
    }

    private void restarDeListaCompra(JSONObject jsonCompra, String nombreNorm, double cantidad) {
        try {
            JSONArray items = jsonCompra.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (limpiarNombre(item.getString("nombre")).equals(nombreNorm)) {
                    double actual = item.getDouble("cantidadNum");
                    item.put("cantidadNum", Math.max(0, actual - cantidad));
                    break;
                }
            }
        } catch (Exception e) {}
    }

    private String limpiarNombre(String n) {
        if (n == null) return "";
        if (n.contains("(")) n = n.substring(0, n.indexOf("("));
        n = n.trim().toLowerCase();
        if (n.endsWith("s") && n.length() > 3) n = n.substring(0, n.length() - 1);
        return n;
    }

    // Gestión de Archivos y UI base
    private JSONObject leerJSON(String archivo) { try{FileInputStream f=requireActivity().openFileInput(archivo); BufferedReader r=new BufferedReader(new InputStreamReader(f)); StringBuilder s=new StringBuilder(); String l; while((l=r.readLine())!=null)s.append(l); f.close(); return new JSONObject(s.toString());}catch(Exception e){return new JSONObject();} }
    private void guardarJSON(String archivo, JSONObject json) { try{FileOutputStream f=requireActivity().openFileOutput(archivo, MODE_PRIVATE); f.write(json.toString().getBytes()); f.close();}catch(Exception e){} }

    private void configurarSwipe(RecyclerView r) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder v, int d) {
                int p = v.getAdapterPosition();
                Object o = adapter.getItemAt(p);
                if(o instanceof PantryItem) eliminarItem((PantryItem)o);
                else adapter.notifyItemChanged(p);
            }
        }).attachToRecyclerView(r);
    }
    private void eliminarItem(PantryItem i) { for(List<PantryItem> l:despensaMap.values()) if(l.remove(i)){ guardarDatosEnArchivo(); actualizarListaVisual(); break; } }
    private void eliminarDeMemoria(PantryItem i) { for(List<PantryItem> l:despensaMap.values()) if(l.remove(i)) return; }
    private double extractNumber(String t) { try{ Matcher m=Pattern.compile("[-+]?[0-9]*\\.?[0-9]+").matcher(t); if(m.find()) return Double.parseDouble(m.group()); }catch(Exception e){} return 0.0; }
    private void guardarDatosEnArchivo() { JSONObject r = new JSONObject(); try { for (Map.Entry<String, List<PantryItem>> e : despensaMap.entrySet()) { JSONArray a = new JSONArray(); for (PantryItem i : e.getValue()) { JSONObject o = new JSONObject(); o.put("nombre", i.getName()); o.put("cantidad", i.getQuantity()); o.put("imagen", i.getIconResId()); a.put(o); } r.put(e.getKey(), a); } FileOutputStream f = requireActivity().openFileOutput(ARCHIVO_PANTRY, MODE_PRIVATE); f.write(r.toString().getBytes()); f.close(); } catch (Exception e) {} }
    private void cargarDatosDesdeArchivo() { try { FileInputStream f = requireActivity().openFileInput(ARCHIVO_PANTRY); BufferedReader r = new BufferedReader(new InputStreamReader(f)); StringBuilder s = new StringBuilder(); String l; while ((l = r.readLine()) != null) s.append(l); JSONObject root = new JSONObject(s.toString()); despensaMap = new HashMap<>(); Iterator<String> k = root.keys(); while (k.hasNext()) { String c = k.next(); JSONArray a = root.getJSONArray(c); List<PantryItem> list = new ArrayList<>(); for (int i = 0; i < a.length(); i++) { JSONObject o = a.getJSONObject(i); list.add(new PantryItem(o.getString("nombre"), o.getString("cantidad"), o.getInt("imagen"))); } despensaMap.put(c, list); } } catch (Exception e) { despensaMap = new HashMap<>(); } actualizarListaVisual(); }
    private void actualizarListaVisual() { itemsParaMostrar.clear(); for (Map.Entry<String, List<PantryItem>> e : despensaMap.entrySet()) { List<PantryItem> v = new ArrayList<>(); for (PantryItem i : e.getValue()) if (extractNumber(i.getQuantity()) > 0) v.add(i); if (!v.isEmpty()) { itemsParaMostrar.add(e.getKey()); itemsParaMostrar.addAll(v); } } if (adapter != null) adapter.notifyDataSetChanged(); }
}