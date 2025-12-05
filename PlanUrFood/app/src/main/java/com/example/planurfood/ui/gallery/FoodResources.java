package com.example.planurfood.ui.gallery;

import com.example.planurfood.R;
import java.util.HashMap;
import java.util.Map;

public class FoodResources {

    // Este es mi diccionario
    private static final Map<String, Integer> mapaIconos = new HashMap<>();

    // Bloque estático: Se ejecuta una sola vez al iniciar la app
    static {
        // --- AQUI AÑADES TU LISTA GRANDE ---
        // Asegúrate de escribir las claves (nombres) en MINÚSCULAS

        // Lácteos
        mapaIconos.put("leche", R.drawable.leche);
        mapaIconos.put("huevos", R.drawable.huevos);
        mapaIconos.put("queso", R.drawable.queso);
        mapaIconos.put("yogur", R.drawable.yogur);

        // Frutas y Verduras
        mapaIconos.put("manzana", R.drawable.manzana);
        mapaIconos.put("manzanas", R.drawable.manzana); // Por si lo escriben en plural
        mapaIconos.put("zanahoria", R.drawable.zanahoria);
        mapaIconos.put("tomate", R.drawable.tomate);

        // Despensa
        mapaIconos.put("arroz", R.drawable.arroz);
        mapaIconos.put("pasta", R.drawable.pasta);
        mapaIconos.put("pan", R.drawable.pan);

        // ... Sigue añadiendo aquí
    }

    // Poder devolver los nombres de todos los iconos
    public static java.util.List<String> getNombresDisponibles() {
        return new java.util.ArrayList<>(mapaIconos.keySet());
    }

    /**
     * Método mágico: Le das un nombre y te devuelve el icono.
     * Si no encuentra el icono, devuelve uno por defecto.
     */
    public static int getIconoPara(String nombreComida) {
        if (nombreComida == null) return android.R.drawable.ic_menu_camera;

        // 1. Limpieza básica
        String clave = nombreComida.trim().toLowerCase();

        // 2. Intento Exacto: ¿Existe "tomates"?
        if (mapaIconos.containsKey(clave)) {
            return mapaIconos.get(clave);
        }

        // 3. Intento Singular (Quitar 's'): Si escribió "tomates", buscamos "tomate"
        if (clave.endsWith("s")) {
            String claveSinS = clave.substring(0, clave.length() - 1);
            if (mapaIconos.containsKey(claveSinS)) {
                return mapaIconos.get(claveSinS);
            }
        }

        // 4. Intento Singular (Quitar 'es'): Si escribió "panes", buscamos "pan"
        if (clave.endsWith("es")) {
            String claveSinEs = clave.substring(0, clave.length() - 2);
            if (mapaIconos.containsKey(claveSinEs)) {
                return mapaIconos.get(claveSinEs);
            }
        }

        // 5. Si nada funciona, icono por defecto
        return android.R.drawable.ic_menu_camera;
    }
}