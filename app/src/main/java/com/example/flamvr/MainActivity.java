package com.example.flamvr;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flamvr.core.MediaCodecPlayer;
import com.example.flamvr.databinding.ActivityMainBinding;
import com.example.flamvr.platform.opengl.OpenGLRenderer;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private GLSurfaceView glSurfaceView;
    private OpenGLRenderer renderer;
    private MediaCodecPlayer player;
    private boolean surfaceReady = false;
    private Uri pendingUri;
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> pickVideoLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        glSurfaceView = findViewById(R.id.surfaceView);
        glSurfaceView.setEGLContextClientVersion(3); // OpenGL ES 3.1
        renderer = new OpenGLRenderer(this);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        FrameLayout root = findViewById(R.id.root);

        setupActivityResultLauncher();
        Log.e(TAG, "Activity created");
        Button pickButton = new Button(this);
        pickButton.setText("Pick Video");
        pickButton.setOnClickListener(v -> openFilePicker());

        addContentView(pickButton, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        //listen to surface creation callbacks
        renderer.setOnSurfaceReadyCallback(this::onSurfaceReady);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickVideoLauncher.launch(intent);
    }

    private void setupActivityResultLauncher() {
        pickVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        if (videoUri != null) {
                            getContentResolver().takePersistableUriPermission(
                                    videoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            if (surfaceReady) {
                                player.start(videoUri);
                            } else {
                                pendingUri = videoUri;
                            }
                        }
                    }
                }
        );
    }
    public void onSurfaceReady(Surface surface) {
        if(player == null){
            player = new MediaCodecPlayer(this, surface);
        } else {
            player.UpdateSurface(surface);
        }
        Log.e(TAG,"SURFACE CREATED!!!!!!!!!!!!");
        surfaceReady = true;
        if (pendingUri != null) {
            player.start(pendingUri);
            pendingUri = null;
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "Activity Paused");
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        if (player != null) {
            player.stop();
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
        if (player != null) {
            player.stop();
        }
    }
}