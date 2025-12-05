package com.example.planurfood.ui.gallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.planurfood.R;

import java.util.List;

public class PantryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Definimos dos tipos de vista: Cabecera y Producto
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PRODUCT = 1;

    // La lista de datos contendrá TANTO Strings (cabeceras) COMO PantryItems (productos)
    private final List<Object> itemList;

    public PantryAdapter(List<Object> itemList) {
        this.itemList = itemList;
    }

    // Este método es el cerebro: le dice al Adapter qué tipo de fila es la posición actual
    @Override
    public int getItemViewType(int position) {
        if (itemList.get(position) instanceof String) {
            return TYPE_HEADER; // Si es un texto, es cabecera
        } else {
            return TYPE_PRODUCT; // Si es un PantryItem, es producto
        }
    }

    // Aquí decidimos qué XML inflar según el tipo
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_pantry_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_pantry_product, parent, false);
            return new ProductViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_HEADER) {
            // Es una cabecera
            String title = (String) itemList.get(position);
            ((HeaderViewHolder) holder).textHeaderTitle.setText(title);
        } else {
            // Es un producto
            PantryItem item = (PantryItem) itemList.get(position);
            ProductViewHolder productHolder = (ProductViewHolder) holder;
            productHolder.textProductName.setText(item.getName());
            productHolder.textProductQuantity.setText("(" + item.getQuantity() + ")");
            productHolder.imgProductIcon.setImageResource(item.getIconResId());

            // Aquí configurarías el clic en los 3 puntos (btnMoreOptions)
            productHolder.btnMoreOptions.setOnClickListener(v -> {
                // Lógica para abrir menú de opciones (borrar, editar...)
            });
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // --- VIEW HOLDERS ---

    // ViewHolder para la Cabecera
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textHeaderTitle;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textHeaderTitle = itemView.findViewById(R.id.textHeaderTitle);
        }
    }

    // ViewHolder para el Producto
    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView textProductName;
        TextView textProductQuantity;
        ImageView imgProductIcon;
        ImageView btnMoreOptions;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            textProductName = itemView.findViewById(R.id.textProductName);
            textProductQuantity = itemView.findViewById(R.id.textProductQuantity);
            imgProductIcon = itemView.findViewById(R.id.imgProductIcon);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}