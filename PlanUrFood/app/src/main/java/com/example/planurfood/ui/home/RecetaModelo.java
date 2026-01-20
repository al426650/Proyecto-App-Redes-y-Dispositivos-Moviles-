package com.example.planurfood.ui.home;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RecetaModelo {

    //

    @SerializedName("nombre") // TU TABLA DICE 'nombre'
    private String nombre;

    @SerializedName("categoria") // TU TABLA DICE 'categoria'
    private String categoria;

    @SerializedName("ingredientes") // TU TABLA DICE 'ingredientes'
    private List<Ingrediente> ingredientes;

    @SerializedName("id") // Esto suele ser estándar
    private Integer id;

    // --- GETTERS  ---
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public List<Ingrediente> getIngredientes() { return ingredientes; }
    public Integer getId() { return id; }

    // --- SETTERS ---
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setIngredientes(List<Ingrediente> ingredientes) { this.ingredientes = ingredientes; }


    // --- NIVEL 2: EL JSON DE DENTRO ---
    public static class Ingrediente {

        @SerializedName("name") // EN EL JSON PONE "name"
        private String name;

        @SerializedName("quantity") // EN EL JSON PONE "quantity"
        private String quantity;

        // Getters
        public String getNombre() { return name; }
        public String getCantidad() { return quantity; }

        // Setters
        public void setName(String name) { this.name = name; }
        public void setQuantity(String quantity) { this.quantity = quantity; }

        // toString para depurar
        @Override
        public String toString() {
            return name + " (" + quantity + ")";
        }
    }
}