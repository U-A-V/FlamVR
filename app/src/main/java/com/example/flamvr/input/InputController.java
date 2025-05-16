package com.example.flamvr.input;

import android.widget.SeekBar;

import com.example.flamvr.globals.InputContract;
import com.example.flamvr.databinding.ActivityMainBinding;
public class InputController {
    private final InputContract listener;
    private final ActivityMainBinding binding;
    public InputController(ActivityMainBinding binding, InputContract listener) {
        this.listener = listener;
        this.binding = binding;
        setupListeners();
    }

    private void setupListeners() {
        binding.pickButton.setOnClickListener(v -> {
            listener.openFilePicker();
        });
        binding.playPauseButton.setOnClickListener(v -> {
            listener.onPlayPauseToggled();
        });

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser){
                    listener.onSeekChanged(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public void updateSeekBarProgress(int progress) {
        binding.seekBar.setProgress(progress);
    }

    public void setMaxDuration(int duration) {
        binding.seekBar.setMax(duration);
    }
}