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
import java.util.Map;

public class WeekAdapter extends RecyclerView.Adapter<WeekAdapter.WeekViewHolder> {

    private final List<String> days;
    private final OnMealClickListener listener;
    private Map<String, String> datosGuardados;

    // Interfaz para manejar clics
    public interface OnMealClickListener {
        void onMealClick(String dia, String tipoComida, TextInputEditText cajonTocado);
    }

    public WeekAdapter(List<String> days, Map<String, String> datos, OnMealClickListener listener) {
        this.days = days;
        this.datosGuardados = datos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WeekViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false);
        return new WeekViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekViewHolder holder, int position) {
        String dayName = days.get(position);
        holder.textDayName.setText(dayName);

        String desayuno = datosGuardados.get(dayName + "_Breakfast");
        String comida   = datosGuardados.get(dayName + "_Lunch");
        String cena     = datosGuardados.get(dayName + "_Dinner");

        holder.inputBreakfast.setText(desayuno != null ? desayuno : "");
        holder.inputLunch.setText(comida != null ? comida : "");
        holder.inputdinner.setText(cena != null ? cena : "");

        // Listeners
        holder.inputBreakfast.setOnClickListener(v -> {
            listener.onMealClick(dayName, "Breakfast", holder.inputBreakfast);
        });

        holder.inputLunch.setOnClickListener(v -> {
            listener.onMealClick(dayName, "Lunch", holder.inputLunch);
        });

        holder.inputdinner.setOnClickListener(v -> {
            // Pasamos "Dinner" para que la clave del Map se guarde bien (MONDAY_Dinner)
            listener.onMealClick(dayName, "Dinner", holder.inputdinner);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public static class WeekViewHolder extends RecyclerView.ViewHolder {
        TextView textDayName;
        TextInputEditText inputBreakfast;
        TextInputEditText inputLunch;
        TextInputEditText inputdinner;

        public WeekViewHolder(@NonNull View itemView) {
            super(itemView);
            textDayName = itemView.findViewById(R.id.textDayName);
            inputBreakfast = itemView.findViewById(R.id.inputBreakfast);
            inputLunch = itemView.findViewById(R.id.inputLunch);
            inputdinner = itemView.findViewById(R.id.inputdinner);
        }
    }
}