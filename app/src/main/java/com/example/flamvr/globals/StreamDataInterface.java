package com.example.flamvr.globals;

public class StreamDataInterface {
    public interface ProgressBarStream {
        void updateSeekBarProgress(long progress);
        void setMaxDuration(long duration);

        void updateFrameCount(long frameCount);
    }

    public interface VideoInfoStream{
        void getVideoDim(int width, int height);
    }
}
