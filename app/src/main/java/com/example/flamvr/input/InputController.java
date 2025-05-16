package com.example.flamvr.input;

import android.widget.SeekBar;

import com.example.flamvr.core.MediaCodecPlayer;
import com.example.flamvr.databinding.ActivityMainBinding;

public class InputController {
    private ActivityMainBinding binding;
    private boolean isPlaying = false;
    private MediaCodecPlayer player;

    public void init(ActivityMainBinding binding, MediaCodecPlayer player) {
        this.binding = binding;
        this.player = player;
        setupListeners();
    }

    private void setupListeners() {
        binding.playPauseButton.setOnClickListener(v -> {

        });

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

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