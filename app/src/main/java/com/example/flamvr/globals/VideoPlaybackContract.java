package com.example.flamvr.globals;

import android.net.Uri;

public interface VideoPlaybackContract {
    public void onInitiate(Uri uri);
    public void onPlay();
    public void onPause();
    public void onSeek(int progress);
    void onPlaybackChanged(String speed);
}
