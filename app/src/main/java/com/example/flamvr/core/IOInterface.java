package com.example.flamvr.core;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flamvr.globals.IOInterfaceContract;
import com.example.flamvr.globals.StateManagerContract;
/*
 * IOInterface handles the interaction for selecting video files from device storage.
 * It uses Android's modern Activity Result API to launch a file picker and receive the result.
 */
public class IOInterface implements IOInterfaceContract {
    private ActivityResultLauncher<Intent> pickVideoLauncher; // Launcher to start the file picker activity and handle its result
    private Uri fileUri;// Uri of the selected video file
    private final AppCompatActivity activity;  // Reference to the hosting activity, needed for context and registering the launcher
    private final StateManagerContract stateListener; // Reference to state manager to notify when a file is picked

    /*
     * Constructor initializes IOInterface with the hosting activity and a state listener.
     * It also sets up the activity result launcher for the file picker.
     */
    public IOInterface(AppCompatActivity activity, StateManagerContract stateListener) {
        this.activity = activity;
        this.stateListener = stateListener;
        setupActivityResultLauncher();
    }
    /*
     * Triggers the system file picker to select a video file.
     * Uses Intent with ACTION_OPEN_DOCUMENT filtered for video mime types.
     */
    @Override
    public void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickVideoLauncher.launch(intent);
    }
    /*
     * Sets up the ActivityResultLauncher to handle the file picker result asynchronously.
     * On success, obtains the Uri of the selected video and persists permission to access it.
     * Notifies the state listener with the selected file Uri.
     */
    private void setupActivityResultLauncher() {
        pickVideoLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        if (videoUri != null) {
                            activity.getContentResolver().takePersistableUriPermission(videoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            fileUri = videoUri;
                        }
                        stateListener.onFilePicked(fileUri);
                    }
                }
        );
    }
}
