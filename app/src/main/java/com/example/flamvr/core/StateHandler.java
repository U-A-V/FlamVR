package com.example.flamvr.core;

import android.net.Uri;

import com.example.flamvr.globals.FILTERS;
import com.example.flamvr.globals.IOInterfaceContract;
import com.example.flamvr.globals.InputContract;
import com.example.flamvr.globals.StateManagerContract;
import com.example.flamvr.globals.StreamDataInterface;
import com.example.flamvr.globals.VideoPlaybackContract;
/**
 * Central class that manages state and coordinates communication
 * between various components of the video playback system.
 * Implements multiple interfaces to handle user input, state management,
 * and progress bar streaming updates.
 */
public class StateHandler implements InputContract, StateManagerContract, StreamDataInterface.ProgressBarStream {

    // Delegate to forward progress bar updates to the UI or other listener
    StreamDataInterface.ProgressBarStream progressBarStream;
    @Override
    public void updateSeekBarProgress(long progress) {
        // Forward the updated seek bar progress to the registered listener
        progressBarStream.updateSeekBarProgress(progress);
    }

    @Override
    public void setMaxDuration(long duration) {
        // Forward the max duration (e.g., video length) to the listener
        progressBarStream.setMaxDuration(duration);
    }

    @Override
    public void updateFrameCount(long frameCount) {
        // Forward updated frame count info to the listener
        progressBarStream.updateFrameCount(frameCount);
    }
    /**
     * Enum defining different handler types that listen to state events.
     * These correspond to various system components interested in playback state.
     */
    public enum HANDLERS{
        UI_HANDLER, MEDIA_CODEC_HANDLER, IO_HANDLER, OPENGL_HANDLER
    }
    // Array holding VideoPlaybackContract listeners indexed by HANDLERS enum
    private final VideoPlaybackContract[] videoPlayBackListeners = new VideoPlaybackContract[HANDLERS.values().length];

    // IOInterfaceContract listener, expected to handle I/O related events like file picking
    private IOInterfaceContract ioInterfaceListener = null;

    // Count of how many VideoPlaybackContract listeners have been registered
    private int totalVPListeners = 0;

    // Array holding listeners for video info streaming
    private StreamDataInterface.VideoInfoStream[] videoInfoStream = new StreamDataInterface.VideoInfoStream[HANDLERS.values().length];

    // Count of how many video info stream listeners have been registered
    private int totalVSListeners = 0;

    /**
     * Registers a listener to receive video info stream updates (e.g., filters).
     */
    public void addStreamVS(StreamDataInterface.VideoInfoStream listener){
        videoInfoStream[totalVSListeners++] = listener;
    }

    /**
     * Registers a listener to receive video playback state events (play, pause, seek).
     */
    public void addListener(VideoPlaybackContract listener){
        videoPlayBackListeners[totalVPListeners++] = listener;
    }

    /**
     * Registers the IO interface listener to handle file picking etc.
     */
    public void addListener(IOInterfaceContract listener){
        ioInterfaceListener = listener;
    }

    /**
     * Registers a listener to receive progress bar updates.
     */
    public void addStream(StreamDataInterface.ProgressBarStream listener){
        progressBarStream = listener;
    }

    // Playback state flags
    private boolean isPlaying = false;
    private boolean surfaceCreated = false;
    private boolean videoFileAvailable = false;

    // Current selected filter for video playback
    private FILTERS filter = FILTERS.NONE;

    // URI of the currently loaded video file
    private Uri fileUri = null;

    /**
     * Returns the URI of the currently loaded video file.
     */
    public Uri getFileUri(){
        return fileUri;
    }

    /**
     * Called to open the file picker dialog.
     * Delegates to the registered IO interface listener.
     */
    @Override
    public void openFilePicker() {
        ioInterfaceListener.openFilePicker();
    }

    /**
     * Called when the user has picked a video file.
     * Saves the file URI, marks video as available, and initiates playback.
     */
    @Override
    public void onFilePicked(Uri uri) {
        fileUri = uri;
        videoFileAvailable = true;
        onInitiate();
    }

    /**
     * Initiates playback by notifying all registered VideoPlaybackContract listeners
     * that playback is starting, passing the video file URI.
     */
    @Override
    public void onInitiate() {

        isPlaying = true;
        for (int i = 0; i < totalVPListeners; i++){
            videoPlayBackListeners[i].onInitiate(fileUri);
            videoPlayBackListeners[i].onPlay();
        }
    }

    /**
     * Called when playback speed changes.
     * Propagates the speed change to all registered listeners.
     */
    @Override
    public void onPlaybackChanged(String speed) {
        for (int i = 0; i < totalVPListeners; i++){
            videoPlayBackListeners[i].onPlaybackChanged(speed);
        }
    }

    /**
     * Toggles playback between playing and paused states.
     * Notifies all listeners of the updated state.
     */
    @Override
    public void onPlayPauseToggled() {
        isPlaying = !isPlaying;
        if(isPlaying){
            for (int i = 0; i < totalVPListeners; i++){
                videoPlayBackListeners[i].onPlay();
            }
        } else {
            for (int i = 0; i < totalVPListeners; i++) {
                videoPlayBackListeners[i].onPause();
            }

        }
    }

    /**
     * Called when the seek position changes.
     * Forwards the new seek progress to all playback listeners.
     */
    @Override
    public void onSeekChanged(int progress) {
        for (int i = 0; i < totalVPListeners; i++) {
            videoPlayBackListeners[i].onSeek(progress);
        }
    }

    /**
     * Called when the user changes the filter selection.
     * Updates internal filter state and notifies all video info stream listeners.
     */
    @Override
    public void onFilterChange(String filterId) {
        switch(filterId){
            case "NONE":
                filter = FILTERS.NONE;
                break;
            case "FILTER1":
                filter = FILTERS.FILTER1;
                break;
            case "FILTER2":
                filter = FILTERS.FILTER2;
                break;
            case "FILTER3":
                filter = FILTERS.FILTER3;
                break;
                default:
                filter = FILTERS.NONE;
                break;
        }
        for (int i = 0; i < totalVSListeners; i++) {
            videoInfoStream[i].getFilter(filter.ordinal());
        }
    }
}
