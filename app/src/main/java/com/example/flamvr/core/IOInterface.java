package com.example.flamvr.core;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flamvr.globals.IOInterfaceContract;
import com.example.flamvr.globals.StateManagerContract;

public class IOInterface implements IOInterfaceContract {
    private ActivityResultLauncher<Intent> pickVideoLauncher;
    private Uri fileUri;
    private final AppCompatActivity activity;
    private final StateManagerContract stateListener;

    public IOInterface(AppCompatActivity activity, StateManagerContract stateListener) {
        this.activity = activity;
        this.stateListener = stateListener;
        setupActivityResultLauncher();
    }
    @Override
    public void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickVideoLauncher.launch(intent);
    }

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
