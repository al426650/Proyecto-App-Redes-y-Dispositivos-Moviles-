package com.example.planurfood.ui.home;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface SupabaseApi {

    @GET("recetas?select=*")
    Call<List<RecetaModelo>> obtenerRecetas(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization
    );

    @POST("recetas")
    Call<Void> crearReceta(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Header("Prefer") String prefer,
            @Body RecetaModelo nuevaReceta
    );
}