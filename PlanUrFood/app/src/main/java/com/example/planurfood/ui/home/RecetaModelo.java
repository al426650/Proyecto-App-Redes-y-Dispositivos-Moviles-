package com.example.planurfood.ui.home;

import java.util.List;

public class RecetaModelo {
    private String nombre;      // Nombre de la receta (ej: "Tortitas")
    private String categoria;   // Categoría (ej: "Breakfast")

    // Aquí está el cambio: Es una lista de objetos Ingrediente, no de textos simples
    private List<Ingrediente> ingredientes;

    // --- Getters ---
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public List<Ingrediente> getIngredientes() { return ingredientes; }

    // --- Clase interna para leer tu JSON de ingredientes ---
    public static class Ingrediente {
        private String name;
        private String quantity; // Usamos String para que acepte números (100) o texto ("100g")

        public String getNombre() { return name; }
        public String getCantidad() { return quantity; }
    }
}