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

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Object> items;
    private final OnPantryClickListener listener; // Nuevo listener

    // Interfaz para comunicarse con el Fragment
    public interface OnPantryClickListener {
        void onItemClick(PantryItem item);
    }

    public PantryAdapter(List<Object> items, OnPantryClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) return TYPE_HEADER;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pantry_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pantry_product, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            ((HeaderViewHolder) holder).title.setText((String) items.get(position));
        } else {
            PantryItem item = (PantryItem) items.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            itemHolder.name.setText(item.getName());
            itemHolder.qty.setText(item.getQuantity());
            itemHolder.icon.setImageResource(item.getIconResId());
            itemHolder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // Método para obtener el objeto en una posición (usado para el Swipe)
    public Object getItemAt(int position) {
        return items.get(position);
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        HeaderViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.headerTitle);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView name, qty;
        ImageView icon;
        ItemViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.productName);
            qty = v.findViewById(R.id.productQty);
            icon = v.findViewById(R.id.productIcon);
        }
    }
}