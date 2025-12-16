package com.example.planurfood.ui.home;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

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

public class NetworkConectionPython extends  Thread{

    private static final int HTTP_SERVER_PORT = 8082;
    // Esta mal <-- No olvidar cambiar
    String url_yolo = "https://predict.ultralytics.com/";
    String api = "be7dd2e555dbbf5f5e4809e94f0637e7a238977017";
    String url_modelo = "https://hub.ultralytics.com/models/Pi3uAeiO8JYD2bX0wotZ";
    JSONObject data;
    JSONObject cabezera;
    JSONObject fichero;
    static File mediaStorageDir;
    static File mediaFile;
    final public int CODE_OK = 200;
    final public int CODE_BADREQUEST = 400;
    final public int CODE_FORBIDDEN = 403;
    final public int CODE_NOTFOUND = 404;
    final public int CODE_INTERNALSERVERERROR = 500;
    final public int CODE_NOTIMPLEMENTED = 501;
    private static Context Ncontext;

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

    private String getHTTP_HeaderStatus(int headerStatusCode) {
        String result = "";
        switch (headerStatusCode) {
            case CODE_OK:
                result = "200 OK";
                break;
            case CODE_BADREQUEST:
                result = "400 Bad Request";
                break;
            case CODE_FORBIDDEN:
                result = "403 Forbidden";
                break;
            case CODE_NOTFOUND:
                result = "404 Not Found";
                break;
            case CODE_INTERNALSERVERERROR:
                result = "500 Internal Server Error";
                break;
            case CODE_NOTIMPLEMENTED:
                result = "501 Not Implemented";
                break;
        }
        return ("HTTP/1.0 " + result);
    }
    private String getHTTP_HeaderContentLength(int headerFileLength){
        return "Content-Length: " + headerFileLength + "\r\n";
    }
    private String getHTTP_HeaderContentType(String headerContentType){
        return "Content-Type: "+headerContentType+"\r\n";
    }
    private String getHTTP_Header(int headerStatusCode, String headerContentType, int
            headerFileLength) {
        String result = getHTTP_HeaderStatus(headerStatusCode) +
                "\r\n" +
                getHTTP_HeaderContentLength(headerFileLength)+
                getHTTP_HeaderContentType(headerContentType)+
                "\r\n";
        return result;
    }
    // A diferencia del anterior, no creamos un socket sino que nos conectamos a un web server externo
    public void run() {
        Message msg = new Message();
        msg.arg1 = 0;
        msg.obj = "CAMERA";
        handlerNetworkExecutor.sendMessage(msg);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Versión python

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(Ncontext));
        }

        python = Python.getInstance();
        mainModule = python.getModule("main"); // Refers to main.py

        File imagefile = getOutputMediaFile();
        respuesta = mainModule.callAttr("request_yolo", imagefile.getPath());
        Log.e("RESPUESTA", respuesta.toString());




    }

    private String readStream(InputStream in) throws IOException {

        BufferedReader r = null;
        r = new BufferedReader(new InputStreamReader(in));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        if(r != null){
            r.close();
        }
        in.close();
        return total.toString();
    }

    private static File getOutputMediaFile() {
        if (mediaStorageDir == null){
            mediaStorageDir = new File(Ncontext.getFilesDir(), "MyCameraApp");
            Log.v("mediaStorageDir", String.valueOf(mediaStorageDir.getAbsolutePath()));
            if (! mediaStorageDir.exists()) {
                Log.e("EXISTE", "Existe");
                if (! mediaStorageDir.mkdirs()) {
                    Log.e("MyCameraApp", "failed to create directory");
                    return null;
                }
            }
            Log.e("DONDE", "creado");
        }

        if (mediaFile==null) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG.jpg");
        }
        Log.e("Existe dir?", String.valueOf(mediaStorageDir.exists()));
        return mediaFile;
    }
}