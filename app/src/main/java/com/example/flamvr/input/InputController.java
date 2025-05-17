package com.example.flamvr.input;

import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Toast;

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
        binding.filterSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                listener.onFilterChange(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                listener.onFilterChange("NONE");
            }
        });
    }

    public void updateSeekBarProgress(int progress) {
        binding.seekBar.setProgress(progress);
    }

    public void setMaxDuration(int duration) {
        binding.seekBar.setMax(duration);
    }
}