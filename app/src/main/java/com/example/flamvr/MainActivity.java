package com.example.flamvr;


import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.flamvr.core.IOInterface;
import com.example.flamvr.core.MediaCodecPlayer;
import com.example.flamvr.core.StateHandler;
import com.example.flamvr.databinding.ActivityMainBinding;
import com.example.flamvr.input.InputController;
import com.example.flamvr.platform.opengl.OpenGLRenderer;
import com.example.flamvr.ui.UIHandler;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private GLSurfaceView glSurfaceView;
    private OpenGLRenderer renderer;
    private MediaCodecPlayer mediaCodecPlayer;
    private InputController inputController;
    private StateHandler stateHandler;
    private IOInterface ioInterface;
    private UIHandler uiHandler;
    private boolean surfaceReady = false;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //setting up full screen render
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        //setting up input bindings
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //setting up OpenGL context and renderer
        glSurfaceView = findViewById(R.id.surfaceView);
        glSurfaceView.setEGLContextClientVersion(3); // OpenGL ES 3.1
        renderer = new OpenGLRenderer(this);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        //setting up UIHandler
        uiHandler = new UIHandler(this);

        //setting up stateManager
        stateHandler = new StateHandler();
        stateHandler.addListener(uiHandler);
        stateHandler.addStream(uiHandler);
        stateHandler.addStreamVS(uiHandler);
        stateHandler.addStreamVS(renderer);
        //setting up IOInterface
        ioInterface = new IOInterface(this, stateHandler);
        stateHandler.addListener(ioInterface);

        //setting up input controller
        inputController = new InputController(binding, stateHandler);


        Log.e(TAG, "Activity created");

        //listen to surface creation callbacks
        renderer.setOnSurfaceReadyCallback(this::onSurfaceReady);
    }

    public void onSurfaceReady(Surface surface) {
        if(mediaCodecPlayer == null){
            mediaCodecPlayer = new MediaCodecPlayer(this, surface);
            stateHandler.addListener(mediaCodecPlayer);
            mediaCodecPlayer.addStream(stateHandler);
            mediaCodecPlayer.addStream(renderer);
        } else {
            mediaCodecPlayer.UpdateSurface(surface);
        }
        Log.e(TAG,"SURFACE CREATED!!!!!!!!!!!!");
        surfaceReady = true;
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "Activity Paused");
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        surfaceReady = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "Activity Resumed");
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "Activity DESTROYED");
        super.onDestroy();
        if (mediaCodecPlayer != null) {
            mediaCodecPlayer.stop();
        }
    }
}