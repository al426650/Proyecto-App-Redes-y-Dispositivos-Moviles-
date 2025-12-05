package com.example.planurfood.ui.gallery;

public class PantryItem {
    private String name;
    private String quantity;
    private int iconResId; // El ID del recurso de imagen (R.drawable.manzana)

    // Constructor
    public PantryItem(String name, String quantity, int iconResId) {
        this.name = name;
        this.quantity = quantity;
        this.iconResId = iconResId;
    }

    // Getters (para poder leer los datos)
    public String getName() { return name; }
    public String getQuantity() { return quantity; }
    public int getIconResId() { return iconResId; }
}