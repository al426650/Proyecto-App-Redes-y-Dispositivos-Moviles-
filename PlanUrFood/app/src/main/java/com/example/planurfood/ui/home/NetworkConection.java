package com.example.planurfood.ui.home;

import static java.lang.System.out;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.StringTokenizer;


import okhttp3.HttpUrl;

public class NetworkConection extends Thread {
    private static final int HTTP_SERVER_PORT = 8082;
    // Esta mal <-- No olvidar cambiar
    String url_yolo = "https://predict.ultralytics.coma/";
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

    public NetworkConection(Context context, Handler _hadlerNetworkResult) {
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
        // Intentamos leer la url de la web de yolo.
        try {
            cabezera = new JSONObject();
            cabezera.put("x-api-key", api);
            // Datos de la imagen
            Log.v("cabezara", String.valueOf(cabezera));
            File imagefile = getOutputMediaFile();

            data = new JSONObject();
            data.put("model", url_modelo);
            data.put("imgsz", 640);
            data.put("conf", 0.25);
            data.put("iou", 0.45);
            Log.v("ERROR", String.valueOf(data));

            URL url = new URL(url_yolo);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true); // permite la salida
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("POST");
            //urlConnection.setChunkedStreamingMode(0);

            //HttpURLConnection urlConnection = null;
            // Api-key
if (imagefile.exists()) {
    FileReader fr = new FileReader(imagefile);
    BufferedReader br = new BufferedReader(fr);
fichero = new JSONObject();
fichero.put("file", br);
    Log.v("ERROR", String.valueOf(br));
//Configuración de la url
    //urlConnection.getRequestMethod();
    //Log.e("ERROR", String.valueOf(urlConnection));
    //urlConnection.setRequestProperty("headers", String.valueOf(cabezera));
    Log.v("URL", String.valueOf(urlConnection));
    //urlConnection.setRequestProperty("files", String.valueOf(fichero.toString()));
    //urlConnection.setRequestProperty("data", String.valueOf(data));
    //Log.v("URL_content", String.valueOf(urlConnection.getContent()));
    Log.v("URL", String.valueOf(urlConnection.getRequestProperties()));

    try(OutputStream os = urlConnection.getOutputStream()) {
        byte[] input = cabezera.toString().getBytes("utf-8");
        os.write(input, 0, input.length);
    }
    Log.v("Respuesta", urlConnection.getResponseMessage());
} else {
    msg.arg1 = 0;
    msg.obj = "CAMERA";
    handlerNetworkExecutor.sendMessage(msg);
}
            Thread.sleep(1000);
            Integer codigoRespuesta = urlConnection.getResponseCode();
            Log.v("Respuesta", urlConnection.getResponseMessage());

            Log.v("Conexion", String.valueOf(codigoRespuesta));
            body = readStream(urlConnection.getInputStream());
            Log.v("ERROR", body);
            if(codigoRespuesta == 200){//Vemos si es 200 OK y leemos el cuerpo del mensaje.
                Log.e("Conectado", "Conectado1");
                // Envio de la imagen
                // cabezera

                // get a las predicciones --> enlace https://predict.ultralytics.com/
                body = readStream(urlConnection.getInputStream());
                Log.v("ERROR", body);

            } else {
                Log.e("WEB", "Desconectado");
            }

            // desconectamos
            urlConnection.disconnect();
        } catch (MalformedURLException e) {
            body = e.toString(); //Error URL incorrecta
            e.printStackTrace();
        } catch (SocketTimeoutException e){
            body = e.toString(); //Error: Finalizado el timeout esperando la respuesta del servidor.
            e.printStackTrace();
        } catch (Exception e) {
            body = e.toString();//Error diferente a los anteriores.
            e.printStackTrace();

        }

        //Log.e("ERROR_BODY", body);


    }

    private String readStream(InputStream in) throws IOException{

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