package com.example.planurfood.ui.home;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class NetworkConectionPython extends Thread {

    private static final int HTTP_SERVER_PORT = 8082;
    String url_yolo = "https://predict.ultralytics.com/";
    String api = "be7dd2e555dbbf5f5e4809e94f0637e7a238977017";
    String url_modelo = "https://hub.ultralytics.com/models/Pi3uAeiO8JYD2bX0wotZ";
    JSONObject data;
    JSONObject cabezera;
    JSONObject fichero;
    static File mediaStorageDir;
    static File mediaFile;

    // Contexto estático para poder acceder a las rutas desde fuera si es necesario
    public static Context Ncontext;

    private Handler handlerNetworkExecutor;
    private HttpURLConnection HttpURLConnection;
    private String body;
    private Python python;
    private PyObject mainModule;
    private PyObject respuesta;

    public NetworkConectionPython(Context context, Handler _hadlerNetworkResult) {
        this.Ncontext = context;
        this.handlerNetworkExecutor = _hadlerNetworkResult;
    }

    @Override
    public void run() {
        Message msgStart = new Message();
        msgStart.obj = "Procesando...";
        handlerNetworkExecutor.sendMessage(msgStart);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(Ncontext));
        }

        python = Python.getInstance();
        mainModule = python.getModule("main"); // Refers to main.py

        // Obtenemos el archivo (que ya habrá sido guardado por la cámara)
        File imagefile = getOutputMediaFile();

        Log.e("PYTHON", "Procesando imagen: " + imagefile.getPath());

        // Llamada original a su función python
        respuesta = mainModule.callAttr("request_yolo", imagefile.getPath());

        Log.e("RESPUESTA", respuesta.toString());

        // --- ÚNICO CAMBIO IMPORTANTE: ENVIAR RESPUESTA A LA APP ---
        Message msg = new Message();
        msg.obj = respuesta.toString();
        handlerNetworkExecutor.sendMessage(msg);
    }

    // --- CAMBIO: Cambiado de private a PUBLIC para usarlo en CameraFragment ---
    public static File getOutputMediaFile() {
        if (mediaStorageDir == null){
            mediaStorageDir = new File(Ncontext.getFilesDir(), "MyCameraApp");
            if (! mediaStorageDir.exists()) {
                if (! mediaStorageDir.mkdirs()) {
                    Log.e("MyCameraApp", "failed to create directory");
                    return null;
                }
            }
        }

        if (mediaFile==null) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG.jpg");
        }
        return mediaFile;
    }
}