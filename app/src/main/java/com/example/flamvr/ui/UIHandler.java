package com.example.flamvr.ui;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.flamvr.R;
import com.example.flamvr.globals.StreamDataInterface;
import com.example.flamvr.globals.VideoPlaybackContract;


public class UIHandler implements VideoPlaybackContract, StreamDataInterface.ProgressBarStream, StreamDataInterface.VideoInfoStream {
    private final AppCompatActivity activity;
    private final SeekBar seekBar;
    private final ImageButton btnPlayPause;
    private final TextView tvTotal;
    private final TextView tvCurrent;
    private final TextView tvFrameCount;
    private final Spinner spPlaybackControl;
    private final Spinner spFilterSelection;
    private long totalDurationMs = 0;
    private long progress = 0;
    private long frameCount = 0;
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            tvTotal.setText(formatTime(totalDurationMs));
            tvCurrent.setText(formatTime(progress));
            tvFrameCount.setText(String.valueOf(frameCount));
            handler.postDelayed(this, 1000); // Repeat every 1 second
        }
    };
    public UIHandler(AppCompatActivity activity){
        this.activity = activity;
        seekBar = activity.findViewById(R.id.seekBar);
        btnPlayPause = activity.findViewById(R.id.playPauseButton);
        tvTotal = activity.findViewById(R.id.totalDuration);
        tvCurrent = activity.findViewById(R.id.currDuration);
        tvFrameCount = activity.findViewById(R.id.frameCount);
        spPlaybackControl = activity.findViewById(R.id.plabackControl);


        String[] playBackSpeeds = {"0.5x", "1.0x", "1.5x", "2.0x"};

        // Adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, playBackSpeeds
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spPlaybackControl.setAdapter(adapter);

        spPlaybackControl.setSelection(adapter.getPosition("1.0x"));

        spFilterSelection = activity.findViewById(R.id.filterSelection);

        String[] filters = {"NONE", "FILTER1", "FILTER2", "FILTER3"};
        // Adapter
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, filters
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterSelection.setAdapter(filterAdapter);
        handler.post(updateRunnable);
    }
    @Override
    public void onInitiate(Uri uri) {

    }

    @Override
    public void onPlay() {
        seekBar.setMax(100);
        btnPlayPause.setImageResource(R.drawable.ic_pause);
    }

    @Override
    public void onPause() {
        btnPlayPause.setImageResource(R.drawable.ic_play);

    }

    @Override
    public void onSeek(int progress) {

    }

    @Override
    public void onPlaybackChanged(String speed) {
        Toast.makeText( activity, "Selected: " + speed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updateSeekBarProgress(long progress) {
        int progressPercentage = (int) ((float) progress / totalDurationMs * 100);
        seekBar.setProgress((int) progressPercentage);
        this.progress = progress;
    }

    @Override
    public void setMaxDuration(long duration) {
        totalDurationMs = duration;
    }

    @Override
    public void updateFrameCount(long frameCount) {
        this.frameCount = frameCount;
    }


    private String formatTime(long microseconds) {
        long totalSeconds = microseconds / 1_000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void getVideoDim(int width, int height) {

    }

    @Override
    public void getFilter(int id) {
        Toast.makeText( activity, "Selected: Filter " + id, Toast.LENGTH_SHORT).show();
    }
}
