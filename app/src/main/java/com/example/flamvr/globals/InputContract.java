package com.example.flamvr.globals;
import android.net.Uri;
public interface InputContract {
    void openFilePicker();
    void onInitiate();
    void onPlaybackChanged(String speed);
    void onPlayPauseToggled();
    void onSeekChanged(int progress);
}
