package com.example.planurfood.ui.gallery;

public class PantryItem {
    private String name;
    private String quantity;
    private int iconResId;

    // Constructor
    public PantryItem(String name, String quantity, int iconResId) {
        this.name = name;
        this.quantity = quantity;
        this.iconResId = iconResId;
    }

    // Getters
    public String getName() { return name; }
    public String getQuantity() { return quantity; }
    public int getIconResId() { return iconResId; }

    // --- NUEVO: Setter necesario para actualizar la cantidad ---
    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }
}