package com.example.planurfood.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputEditText;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planurfood.R;

import java.util.List;

public class WeekAdapter extends RecyclerView.Adapter<WeekAdapter.WeekViewHolder> {

    private final List<String> days;
    private final OnMealClickListener listener;
    // Interfaz para manejar clics en los elementos de la lista
    public interface OnMealClickListener {
        void onMealClick(String dia, String tipoComida, TextInputEditText cajonTocado);
    }

    // Constructor: aquí recibimos la lista de días
    public WeekAdapter(List<String> days, OnMealClickListener listener) {
        this.days = days;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WeekViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Aquí "inflamos" (creamos) la vista de la tarjeta usando tu archivo item_day.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false);
        return new WeekViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekViewHolder holder, int position) {
        // Aquí es donde ponemos el texto correcto (Lunes, Martes...) en cada tarjeta
        String dayName = days.get(position);
        holder.textDayName.setText(dayName);

        //Cuando toquen el desayuno avisamos a fragment con el listener.
        holder.inputBreakfast.setOnClickListener(v -> {
            listener.onMealClick(dayName, "Breakfast", holder.inputBreakfast);
        });
        // Lo mismo con cada comida
        holder.inputLunch.setOnClickListener(v -> {
            listener.onMealClick(dayName, "Lunch", holder.inputLunch);
        });
        holder.inputDiner.setOnClickListener(v -> {
            listener.onMealClick(dayName, "Diner", holder.inputDiner);
        });
    }

    @Override
    public int getItemCount() {
        return days.size(); // Le dice a la lista cuántos elementos hay (7)
    }

    // Clase interna para guardar las referencias a los elementos visuales
    public static class WeekViewHolder extends RecyclerView.ViewHolder {
        TextView textDayName;
        TextInputEditText inputBreakfast;
        TextInputEditText inputLunch;
        TextInputEditText inputDiner;


        public WeekViewHolder(@NonNull View itemView) {
            super(itemView);
            // Buscamos el TextView del título que creamos en el XML
            textDayName = itemView.findViewById(R.id.textDayName);
            // Lo mismo con los cajones de texto
            inputBreakfast = itemView.findViewById(R.id.inputBreakfast);
            inputLunch = itemView.findViewById(R.id.inputLunch);
            inputDiner = itemView.findViewById(R.id.inputDiner);
        }
    }
}