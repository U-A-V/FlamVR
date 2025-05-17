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

/*
 * MainActivity serves as the entry point of the application.
 * It manages the lifecycle of the app, initializes UI components,
 * OpenGL rendering surface, media playback, input handling, and state management.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private GLSurfaceView glSurfaceView; // OpenGL rendering surface view
    private OpenGLRenderer renderer; // Custom OpenGL renderer
    private MediaCodecPlayer mediaCodecPlayer; // Media player handling video/audio decoding and playback
    private InputController inputController; // Controller for user input events
    private StateHandler stateHandler; // State manager handling app state and event streams
    private IOInterface ioInterface; // Interface for IO operations, listens to state changes
    private UIHandler uiHandler; // Handles UI updates and interactions
    private boolean surfaceReady = false; // Flag to track if OpenGL surface is ready for rendering

    private ActivityMainBinding binding; // View binding for activity_main layout

    /*
     * onCreate is called when the activity is first created.
     * It sets up the fullscreen UI, initializes all core components,
     * and configures the OpenGL surface and renderer.
     */
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

    /*
     * Called when the OpenGL rendering surface is ready.
     * Initializes MediaCodecPlayer if not created, or updates surface if already created.
     */
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
    /*
     * Called when the activity goes into the background or is partially obscured.
     * Stops media playback and pauses OpenGL rendering to save resources.
     */
    @Override
    protected void onPause() {
        Log.e(TAG, "Activity Paused");
        if (mediaCodecPlayer != null && mediaCodecPlayer.isPlaying) {
            mediaCodecPlayer.stop();
        }
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        surfaceReady = false;
        super.onPause();
    }
    /*
     * Called when the activity becomes visible and ready to interact.
     * Resumes OpenGL rendering and restarts media playback if surface is ready.
     */
    @Override
    protected void onResume() {
        Log.e(TAG, "Activity Resumed");
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
        if(mediaCodecPlayer != null && surfaceReady){
            mediaCodecPlayer.onInitiate(stateHandler.getFileUri());
        }
    }
    /*
     * Called when the activity is being destroyed.
     * Stops media playback to release resources.
     */
    @Override
    protected void onDestroy() {
        Log.e(TAG, "Activity DESTROYED");
        if (mediaCodecPlayer != null && mediaCodecPlayer.isPlaying) {
            mediaCodecPlayer.stop();
        }
        super.onDestroy();
    }
}