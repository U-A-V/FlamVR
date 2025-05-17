package com.example.flamvr.core;

import android.net.Uri;

import com.example.flamvr.globals.FILTERS;
import com.example.flamvr.globals.IOInterfaceContract;
import com.example.flamvr.globals.InputContract;
import com.example.flamvr.globals.StateManagerContract;
import com.example.flamvr.globals.StreamDataInterface;
import com.example.flamvr.globals.VideoPlaybackContract;

public class StateHandler implements InputContract, StateManagerContract, StreamDataInterface.ProgressBarStream {

    StreamDataInterface.ProgressBarStream progressBarStream;
    @Override
    public void updateSeekBarProgress(long progress) {
        progressBarStream.updateSeekBarProgress(progress);
    }

    @Override
    public void setMaxDuration(long duration) {
        progressBarStream.setMaxDuration(duration);
    }

    @Override
    public void updateFrameCount(long frameCount) {
        progressBarStream.updateFrameCount(frameCount);
    }

    public enum HANDLERS{
        UI_HANDLER, MEDIA_CODEC_HANDLER, IO_HANDLER, OPENGL_HANDLER
    }
    private final VideoPlaybackContract[] videoPlayBackListeners = new VideoPlaybackContract[HANDLERS.values().length];
    private IOInterfaceContract ioInterfaceListener = null;
    private int totalVPListeners = 0;
    private StreamDataInterface.VideoInfoStream[] videoInfoStream = new StreamDataInterface.VideoInfoStream[HANDLERS.values().length];
    private int totalVSListeners = 0;

    public void addStreamVS(StreamDataInterface.VideoInfoStream listener){
        videoInfoStream[totalVSListeners++] = listener;
    }
    public void addListener(VideoPlaybackContract listener){
        videoPlayBackListeners[totalVPListeners++] = listener;
    }
    public void addListener(IOInterfaceContract listener){
        ioInterfaceListener = listener;
    }
    public void addStream(StreamDataInterface.ProgressBarStream listener){
        progressBarStream = listener;
    }
    private boolean isPlaying = false;
    private boolean surfaceCreated = false;
    private boolean videoFileAvailable = false;
    private FILTERS filter = FILTERS.NONE;
    private Uri fileUri = null;
    @Override
    public void openFilePicker() {
        ioInterfaceListener.openFilePicker();
    }
    @Override
    public void onFilePicked(Uri uri) {
        fileUri = uri;
        videoFileAvailable = true;
        onInitiate();
    }
    @Override
    public void onInitiate() {

        isPlaying = true;
        for (int i = 0; i < totalVPListeners; i++){
            videoPlayBackListeners[i].onInitiate(fileUri);
            videoPlayBackListeners[i].onPlay();
        }
    }

    @Override
    public void onPlaybackChanged(String speed) {
        for (int i = 0; i < totalVPListeners; i++){
            videoPlayBackListeners[i].onPlaybackChanged(speed);
        }
    }

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
    @Override
    public void onSeekChanged(int progress) {
        for (int i = 0; i < totalVPListeners; i++) {
            videoPlayBackListeners[i].onSeek(progress);
        }
    }

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
