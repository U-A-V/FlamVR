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

/**
 * Handles UI updates for video playback.
 * Implements interfaces to receive callbacks for playback state and stream data.
 */
public class UIHandler implements VideoPlaybackContract, StreamDataInterface.ProgressBarStream, StreamDataInterface.VideoInfoStream {

    // References to activity and UI components
    private final AppCompatActivity activity;
    private final SeekBar seekBar;
    private final ImageButton btnPlayPause;
    private final TextView tvTotal;
    private final TextView tvCurrent;
    private final TextView tvFrameCount;
    private final Spinner spPlaybackControl;
    private final Spinner spFilterSelection;

    // Playback state variables
    private long totalDurationMs = 0;
    private long progress = 0;
    private long frameCount = 0;

    // Handler to update UI periodically on the main thread
    Handler handler = new Handler(Looper.getMainLooper());

    // Runnable that updates time and frame count every second
    Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            tvTotal.setText(formatTime(totalDurationMs));
            tvCurrent.setText(formatTime(progress));
            tvFrameCount.setText(String.valueOf(frameCount));
            handler.postDelayed(this, 1000); // Repeat every 1 second
        }
    };

    /**
     * Constructor initializes UI components and dropdowns.
     * Starts periodic UI updates.
     */
    public UIHandler(AppCompatActivity activity){
        this.activity = activity;
        seekBar = activity.findViewById(R.id.seekBar);
        btnPlayPause = activity.findViewById(R.id.playPauseButton);
        tvTotal = activity.findViewById(R.id.totalDuration);
        tvCurrent = activity.findViewById(R.id.currDuration);
        tvFrameCount = activity.findViewById(R.id.frameCount);
        spPlaybackControl = activity.findViewById(R.id.plabackControl);

        // Set up playback speed spinner
        String[] playBackSpeeds = {"0.5x", "1.0x", "1.5x", "2.0x"};

        // Adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, playBackSpeeds
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spPlaybackControl.setAdapter(adapter);

        spPlaybackControl.setSelection(adapter.getPosition("1.0x"));

        // Set up filter spinner
        spFilterSelection = activity.findViewById(R.id.filterSelection);

        String[] filters = {"FILTER", "FILTER1", "FILTER2", "FILTER3"};
        // Adapter
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, filters
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterSelection.setAdapter(filterAdapter);
        handler.post(updateRunnable);
    }

    // Triggered when the video is initialized with a URI
    @Override
    public void onInitiate(Uri uri) {

    }

    // Triggered when playback starts
    @Override
    public void onPlay() {
        seekBar.setMax(100);
        btnPlayPause.setImageResource(R.drawable.ic_pause);
    }

    // Triggered when playback pauses
    @Override
    public void onPause() {
        btnPlayPause.setImageResource(R.drawable.ic_play);

    }

    // Triggered when user seeks to a position
    @Override
    public void onSeek(int progress) {

    }

    // Triggered when playback speed changes
    @Override
    public void onPlaybackChanged(String speed) {
        Toast.makeText( activity, "Selected: " + speed, Toast.LENGTH_SHORT).show();
    }

    // Called to update the seek bar position
    @Override
    public void updateSeekBarProgress(long progress) {
        int progressPercentage = (int) ((float) progress / totalDurationMs * 100);
        seekBar.setProgress((int) progressPercentage);
        this.progress = progress;
    }

    // Called to set the total duration of the video
    @Override
    public void setMaxDuration(long duration) {
        totalDurationMs = duration;
    }

    // Called to update frame count on the UI
    @Override
    public void updateFrameCount(long frameCount) {
        this.frameCount = frameCount;
    }

    // Utility to convert microseconds to MM:SS format
    private String formatTime(long microseconds) {
        long totalSeconds = microseconds / 1_000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    // Called to get video dimensions (placeholder)
    @Override
    public void getVideoDim(int width, int height) {

    }

    // Called when a filter is selected
    @Override
    public void getFilter(int id) {
        Toast.makeText( activity, "Selected: Filter " + id, Toast.LENGTH_SHORT).show();
    }
}
