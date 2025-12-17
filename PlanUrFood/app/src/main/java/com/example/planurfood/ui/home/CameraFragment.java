package com.example.planurfood.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.planurfood.R;
import com.example.planurfood.ui.gallery.FoodResources;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CameraFragment extends Fragment {

    private Camera mCamera;
    private CameraPreview mCameraPreview; // Usamos SU clase CameraPreview
    private FrameLayout cameraPreviewFrameLayout;
    private TextView txtStatus;
    private FloatingActionButton btnCapture;
    private boolean isProcessing = false;

    // Handler para recibir la respuesta de Python
    Handler handlerNetworkExecutorResult = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String respuesta = (String) msg.obj;
            Log.e("API_RESULT", "Recibido: " + respuesta);

            if (respuesta != null && !respuesta.equals("Procesando...")) {
                if (respuesta.contains("Error") || respuesta.contains("Exception")) {
                    txtStatus.setText("Error en la detección");
                    isProcessing = false;
                    if (mCamera != null) mCamera.startPreview();
                } else {
                    procesarRespuestaJSON(respuesta);
                }
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_camera, container, false);

        cameraPreviewFrameLayout = root.findViewById(R.id.camera_container);
        txtStatus = root.findViewById(R.id.txt_status);
        btnCapture = root.findViewById(R.id.btn_capture);

        // Inicializamos el contexto estático de su clase para que no de error al buscar rutas
        NetworkConectionPython.Ncontext = requireContext();

        btnCapture.setOnClickListener(v -> {
            if (mCamera != null && !isProcessing) {
                try {
                    isProcessing = true;
                    txtStatus.setText("analyzing...");
                    mCamera.takePicture(null, null, mPicture);
                } catch (Exception e) {
                    isProcessing = false;
                }
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        iniciarCamara();
    }

    @Override
    public void onPause() {
        super.onPause();
        liberarCamara();
    }

    private void iniciarCamara() {
        try {
            mCamera = Camera.open();
            if (mCamera != null) {
                // Instanciamos SU CameraPreview
                mCameraPreview = new CameraPreview(requireContext(), mCamera);
                cameraPreviewFrameLayout.addView(mCameraPreview, 0);
            }
        } catch (Exception e) {
            Log.e("Camera", "Error: " + e.getMessage());
        }
    }

    private void liberarCamara() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        if (cameraPreviewFrameLayout != null) {
            cameraPreviewFrameLayout.removeView(mCameraPreview);
        }
    }

    // --- CALLBACK: Al hacer la foto ---
    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // 1. Conseguimos la ruta usando SU método
            File pictureFile = NetworkConectionPython.getOutputMediaFile();

            if (pictureFile == null) {
                Log.e("Camera", "Error creando archivo");
                return;
            }

            try {
                // 2. Guardamos la foto físicamente
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                // 3. Ejecutamos SU hilo de conexión
                NetworkConectionPython network = new NetworkConectionPython(requireContext(), handlerNetworkExecutorResult);
                network.start();

            } catch (Exception e) {
                Log.e("Camera", "Error guardando: " + e.getMessage());
                isProcessing = false;
            }
        }
    };

    // --- LÓGICA DE GUARDADO EN DESPENSA ---
    private void procesarRespuestaJSON(String jsonRaw) {
        List<String> detectados = new ArrayList<>();
        JSONObject jsonPantry = leerJSON("midestpensa.json");
        boolean huboCambios = false;

        try {
            // Limpieza por si Python devuelve comillas simples
            String jsonFormat = jsonRaw.replace("'", "\"").replace("None", "null");
            JSONObject resultados = new JSONObject(jsonFormat);
            JSONArray imagenes = resultados.getJSONArray("images");
            JSONObject results = imagenes.getJSONObject(0);
            JSONArray results2 = results.getJSONArray("results");
            for (int i = 0; i < results2.length(); i++) {
                JSONObject nombre = results2.getJSONObject(i);
                String name = nombre.getString("name");
                detectados.add(name);
                addToPantry(jsonPantry, name, 1.0);
                huboCambios = true;
                Log.e("JSON", jsonPantry.toString());
            }
            if (huboCambios) {
                guardarJSON("midestpensa.json", jsonPantry);
                txtStatus.setText("Added: " + detectados.toString());
                Toast.makeText(getContext(), "Saved correctly", Toast.LENGTH_SHORT).show();
                // Volver atrás tras 2 seg
                new Handler().postDelayed(() -> Navigation.findNavController(getView()).navigateUp(), 2000);
            } else {
                txtStatus.setText("Nothing was detected");
                isProcessing = false;
                mCamera.startPreview();
            }

        } catch (Exception e) {
            Log.e("JSON", "Error parseando: " + e.getMessage());
            isProcessing = false;
            mCamera.startPreview(); // Reactivar cámara si falla
        }
    }

    // --- Helpers JSON (Sin cambios) ---
    private void addToPantry(JSONObject jsonPantry, String nombre, double cantidad) {
        try {
            if(!jsonPantry.has("Pantry")) jsonPantry.put("Pantry", new JSONArray());
            JSONArray list = jsonPantry.getJSONArray("Pantry");
            boolean found = false;
            for(int i=0; i<list.length(); i++){
                JSONObject item = list.getJSONObject(i);
                if(item.getString("nombre").equalsIgnoreCase(nombre)){
                    String cantStr = item.getString("cantidad").split(" ")[0];
                    double cantActual = 0;
                    try { cantActual = Double.parseDouble(cantStr); } catch(Exception ex){}
                    item.put("cantidad", (cantActual + cantidad) + " detected");
                    found = true;
                    break;
                }
            }
            if (!found) {
                JSONObject newItem = new JSONObject();
                String nombreCap = nombre.substring(0,1).toUpperCase() + nombre.substring(1);
                newItem.put("nombre", nombreCap);
                newItem.put("cantidad", cantidad + " detected");
                newItem.put("imagen", FoodResources.getIconFor(nombreCap));
                list.put(newItem);
            }
        } catch (Exception e){
            Log.e("ERROR", e.toString());
        }
    }

    private JSONObject leerJSON(String archivo) {
        try {
            FileInputStream fis = requireActivity().openFileInput(archivo);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            fis.close();
            return new JSONObject(sb.toString());
        } catch (Exception e) { return new JSONObject(); }
    }

    private void guardarJSON(String archivo, JSONObject json) {
        try {
            FileOutputStream fos = requireActivity().openFileOutput(archivo, MODE_PRIVATE);
            fos.write(json.toString().getBytes());
            fos.close();
        } catch (Exception e) {}
    }
}