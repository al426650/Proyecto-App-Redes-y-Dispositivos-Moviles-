package com.example.planurfood.ui.home;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface   SupabaseApi {

    // Pedimos a la tabla 'recetas' y seleccionamos todas las columnas (*)
    @GET("recetas?select=*")
    Call<List<RecetaModelo>> obtenerRecetas(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization
    );
}