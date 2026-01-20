package com.example.planurfood.ui.gallery;

import com.example.planurfood.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
En esta clase se implementarán métodos en los que nos apoyaremos
a la hora de añadir elementos en la pantry. También contiene elementos
como diccionarios en los que guardamos referencias a recursos usados
en la pantry (iconos de los alimentos, categorías por defecto).
 */
public class FoodResources {

    private static final Map<String, Integer> iconMap = new HashMap<>();
    private static final Map<String, String> categoryMap = new HashMap<>();

    // Lista de líquidos para poner bien las unidades en el
    // caso de que sea un líquido.
    private static final Set<String> LIQUIDS = new HashSet<>(Arrays.asList(
            "milk", "oil", "water", "broth", "cream", "vinegar",
            "soy sauce", "juice", "wine", "beer", "tea", "coffee",
            "olive oil", "lemon juice"
    ));
    // Lista con las categorías de alimentos
    public static final List<String> CATEGORIES = Arrays.asList(
            "Produce", "Dairy & Eggs", "Meat & Fish", "Pantry", "Bakery", "Frozen", "Others"
    );

    // Mapa para añadir el icono correcto a cada alimento.
    static {
        // --- ICONOS ---
        iconMap.put("egg", R.drawable.huevos);
        iconMap.put("bread", R.drawable.pan);
        iconMap.put("potato", R.drawable.patata);
        iconMap.put("tomato", R.drawable.tomate);
        iconMap.put("chicken", R.drawable.pollo);
        iconMap.put("milk", R.drawable.leche);
        iconMap.put("rice", R.drawable.arroz);
        iconMap.put("tomato sauce", R.drawable.salsa_tomate);
        iconMap.put("noodles", R.drawable.pasta);
        iconMap.put("pasta", R.drawable.pasta);
        iconMap.put("shrimp", R.drawable.gambas);
        iconMap.put("onion", R.drawable.cebolla);
        iconMap.put("carrot", R.drawable.zanahoria);
        iconMap.put("cheese", R.drawable.queso);
        iconMap.put("tuna", R.drawable.atun);
        iconMap.put("pepper", R.drawable.pimiento);
        iconMap.put("oil", R.drawable.aceite);
        iconMap.put("yogurt", R.drawable.yogur);
        iconMap.put("apple", R.drawable.manzana);
        iconMap.put("strawberry", R.drawable.fresa);


        // --- CATEGORÍAS ---
        categoryMap.put("milk", "Dairy & Eggs");
        categoryMap.put("yogurt", "Dairy & Eggs");
        categoryMap.put("cheese", "Dairy & Eggs");
        categoryMap.put("egg", "Dairy & Eggs");
        categoryMap.put("potato", "Produce");
        categoryMap.put("tomato", "Produce");
        categoryMap.put("onion", "Produce");
        categoryMap.put("carrot", "Produce");
        categoryMap.put("chicken", "Meat & Fish");
        categoryMap.put("beef", "Meat & Fish");
        categoryMap.put("rice", "Pantry");
        categoryMap.put("pasta", "Pantry");
        categoryMap.put("oil", "Pantry");
        categoryMap.put("bread", "Bakery");
    }

    //Obtenemos una lista con los nombres de los alimentos con igono disponibles.
    public static List<String> getAvailableNames() {
        return new ArrayList<>(iconMap.keySet());
    }
    /*
    Intentamos obtener el nombre en singular del alimento, de esta
    forma no habrá duplicados con tomate y tomates.
     */
    public static String getSingularName(String input) {
        if (input == null || input.isEmpty()) return "";
        String clean = input.trim().toLowerCase();
        // Intentamos directamente
        if (iconMap.containsKey(clean)) return capitalize(clean);
        // Regla del plural simple "s" y evitamos las palabras con "ss"
        if (clean.endsWith("s") && !clean.endsWith("ss")) {
            String noS = clean.substring(0, clean.length() - 1);
            if (iconMap.containsKey(noS)) return capitalize(noS);
        }
        // Regla del plural "es"
        if (clean.endsWith("es")) {
            String noEs = clean.substring(0, clean.length() - 2);
            if (iconMap.containsKey(noEs)) return capitalize(noEs);
        }
        // Regla plural 'ies' (ej. berries -> berry)
        if (clean.endsWith("ies")) {
            String withY = clean.substring(0, clean.length() - 3) + "y";
            if (iconMap.containsKey(withY)) return capitalize(withY);
        }
        return capitalize(input.trim());
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // Método para encontrar el icono correspondiente, si no está en la lista se pone uno genérico de cámara
    public static int getIconFor(String foodName) {
        if (foodName == null) return android.R.drawable.ic_menu_camera;
        String singular = getSingularName(foodName).toLowerCase();
        if (iconMap.containsKey(singular)) return iconMap.get(singular);
        return android.R.drawable.ic_menu_camera;
    }

    // Método para obtener la categoría correspondiente.
    public static String getCategoryFor(String foodName) {
        if (foodName == null) return "Others";
        String singular = getSingularName(foodName).toLowerCase();
        if (categoryMap.containsKey(singular)) return categoryMap.get(singular);
        return "Others";
    }

    // A continuación dos métodos para agregar la unidad correspondiente si es un líquido.
    public static boolean isLiquid(String foodName) {
        if (foodName == null) return false;
        return LIQUIDS.contains(getSingularName(foodName).toLowerCase());
    }

    /**
     * LÓGICA DE UNIDADES:
     * 1. Líquidos -> "ml"
     * 2. Cantidad < 10 -> "uds"
     * 3. Cantidad >= 10 -> "g"
     */
    public static String getAutoUnit(String foodName, double quantity) {
        if (isLiquid(foodName)) return "ml";
        if (Math.abs(quantity) < 10) return "uds"; // Menor de 10 -> uds
        return "g"; // 10 o más -> g
    }
}