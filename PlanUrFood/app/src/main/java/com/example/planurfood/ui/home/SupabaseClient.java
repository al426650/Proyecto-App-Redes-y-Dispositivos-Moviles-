package com.example.planurfood.ui.home;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    // URL correcta con el sufijo para la API
    private static final String SUPABASE_URL = "https://bakrzijgsakdslrizalg.supabase.co/rest/v1/";

    // Tu clave pública (Anon Key)
    public static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJha3J6aWpnc2FrZHNscml6YWxnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ5NDM5MTcsImV4cCI6MjA4MDUxOTkxN30.eA_U2R7giohx1PEp0bUPQE1L8nQpjqmX0yPhatH-jjE";

    private static Retrofit retrofit = null;

    public static SupabaseApi getApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(SUPABASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SupabaseApi.class);
    }
}