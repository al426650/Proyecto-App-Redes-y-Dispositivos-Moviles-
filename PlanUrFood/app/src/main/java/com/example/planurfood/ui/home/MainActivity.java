package com.example.planurfood.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.FrameLayout;

import com.example.planurfood.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.planurfood.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    Handler handlerNetworkExecutorResult = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.e("handlerNetworkExecutorResult", (String) msg.obj);
            if (msg != null) {
                if (((String) msg.obj).contains("CAMERA")) {
                    captureCamera();
                    Log.e("Camera_handler", "Imagen_capturada");
                }
            }
        }
    };

    Camera mCamera;
    static File mediaStorageDir;
    static File mediaFile;
    private static final int REQUEST_CAMERA_WEB = 111;
    private CameraPreview mCameraPreview;
    private FrameLayout cameraPreviewFrameLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCamera = getCameraInstance();
     binding = ActivityMainBinding.inflate(getLayoutInflater());
     setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;


        navigationView.setItemIconTintList(null);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);


        NetworkConectionPython networkExecutor = new NetworkConectionPython(this, handlerNetworkExecutorResult);
        mCameraPreview = new CameraPreview(this, mCamera);
        cameraPreviewFrameLayout = (FrameLayout) findViewById(R.id.cameraView);
        cameraPreviewFrameLayout.addView(mCameraPreview);
        networkExecutor.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
    private android.hardware.Camera getCameraInstance() {
        android.hardware.Camera camera = null;
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_WEB);
        }
        try {
            camera = android.hardware.Camera.open(0);
        } catch (Exception e) {
// cannot get camera or does not exist
            Log.e("getCameraInstance", "ERROR" + e);
        }
        return camera;
    }

    private File getOutputMediaFile() {
        if (mediaStorageDir == null){
            mediaStorageDir = new File(this.getFilesDir(), "MyCameraApp");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.e("MyCameraApp", "failed to create directory");
                    return null;
                }
            }
        }
        if (mediaFile==null) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG.jpg");
        }
        Log.v("ERROR", String.valueOf(mediaStorageDir));
        Log.v("ERROR", String.valueOf(mediaFile));
        return mediaFile;
    }
    public void captureCamera(){

        // Cerrar cámara y volver a abrirla. O el Camera Preview
        Log.v("CAMARA", mCamera.toString());
        if (mCamera!=null) {
            try {
                mCamera.takePicture(null, null, mPicture);
                Log.e("CAMARA", "capturando imagen");
            } catch (Exception e) {
                Log.e("Camera", "Demasiadas capturas" + e);
            }

        }
    }
    byte[] resizeImage(byte[] input) {
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(input, 0, input.length);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 80, 107,
                true);
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob);
        return blob.toByteArray();
    }
    android.hardware.Camera.PictureCallback mPicture = new android.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            byte[] resized = resizeImage(data);
            File pictureFile = getOutputMediaFile();
            Log.v("ERROR_picture", String.valueOf(pictureFile));
            if (pictureFile == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(resized);
                fos.close();
                mCamera.stopPreview();
                mCamera.startPreview();
                Log.e("Camera", "Camera_reiniciada");
            } catch (Exception e) {
                Log.e("onPictureTaken", "ERROR:" + e);
            }
        }
    };
}