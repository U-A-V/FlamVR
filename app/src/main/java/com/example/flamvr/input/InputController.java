package com.example.flamvr.input;

import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;

import com.example.flamvr.globals.InputContract;
import com.example.flamvr.databinding.ActivityMainBinding;

/**
 * Handles all user input events from the UI and forwards them
 * to the InputContract listener for processing.
 * This class binds UI elements to their event listeners.
 */
public class InputController {
    // Listener interface to forward input events (e.g., button clicks, seekbar changes)
    private final InputContract listener;

    // Binding object to access UI elements defined in XML via ViewBinding
    private final ActivityMainBinding binding;

    /**
     * Constructor that sets up input listeners on UI components.
     * @param binding ActivityMainBinding instance for UI element access.
     * @param listener Implementation of InputContract to receive input callbacks.
     */
    public InputController(ActivityMainBinding binding, InputContract listener) {
        this.listener = listener;
        this.binding = binding;
        setupListeners();
    }

    /*
            * Attaches event listeners to UI controls to handle user interactions.
            */
    private void setupListeners() {
        // File picker button: opens the file picker dialog when clicked
        binding.pickButton.setOnClickListener(v -> {
            listener.openFilePicker();
        });
        // Play/pause toggle button: toggles playback state when clicked
        binding.playPauseButton.setOnClickListener(v -> {
            listener.onPlayPauseToggled();
        });

        // SeekBar change listener: updates playback position when user drags the seek bar
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser){
                    listener.onSeekChanged(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        // Playback speed spinner selection listener: notifies listener of new playback speed selection
        binding.plabackControl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                listener.onPlaybackChanged(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                listener.onPlaybackChanged("1.0x");
            }
        });
        // Filter selection spinner listener: notifies listener when filter is changed
        binding.filterSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                listener.onFilterChange(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                listener.onFilterChange("FILTER");
            }
        });
    }

    /**
     * Updates the progress on the seek bar programmatically.
     * @param progress The new progress value.
     */
    public void updateSeekBarProgress(int progress) {
        binding.seekBar.setProgress(progress);
    }

    /**
     * Sets the maximum value for the seek bar (e.g., video duration).
     * @param duration The max duration in milliseconds or frame count.
     */
    public void setMaxDuration(int duration) {
        binding.seekBar.setMax(duration);
    }
}