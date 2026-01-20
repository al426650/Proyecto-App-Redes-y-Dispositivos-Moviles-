package com.example.planurfood.ui.home;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Context mContext;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        this.mCamera = camera;
        this.mContext = context;
        this.mSurfaceHolder = this.getHolder();
        this.mSurfaceHolder.addCallback(this);
        // Deprecated pero necesario en versiones muy antiguas de Android
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            Log.e("CameraPreview", "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            // Detenemos la preview para evitar conflictos
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // Ignorar
            }
            // Nota: El release() suele hacerse en la Activity, pero si lo gestionas aquí descomenta:
            // mCamera.release();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        if (mSurfaceHolder.getSurface() == null || mCamera == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignorar: intentó parar una preview que no existía
        }

        try {
            Camera.Parameters parameters = mCamera.getParameters();

            // 1. --- AUTOFOCUS ---
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            // 2. --- TAMAÑO DE LA IMAGEN (SOLUCIÓN PANTALLA NEGRA) ---
            // Buscamos el tamaño soportado más cercano a 360x360.
            // Si pones setPreviewSize(360, 360) directamente, la pantalla se verá negra
            // porque el hardware no lo soporta.
            List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalSize = getOptimalPreviewSize(supportedSizes, 360, 360);

            if (optimalSize != null) {
                parameters.setPreviewSize(optimalSize.width, optimalSize.height);
                Log.d("CameraPreview", "Tamaño real configurado: " + optimalSize.width + "x" + optimalSize.height);
            }

            // Orientación para que se vea vertical
            mCamera.setDisplayOrientation(90);

            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.e("CameraPreview", "Error iniciando preview: " + e.getMessage());
        }
    }

    /**
     * Busca el tamaño de cámara soportado más cercano al deseado (360x360)
     * para evitar que la cámara falle o se vea negra.
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1; // Tolerancia de aspecto si fuera necesaria
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Intentamos encontrar el tamaño más cercano a la altura deseada (360)
        int targetHeight = h;

        for (Camera.Size size : sizes) {
            // Nota: Camera.Size suele devolver width > height (modo landscape)
            // Simplemente buscamos el que tenga la dimensión más cercana a 360
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        return optimalSize;
    }
}