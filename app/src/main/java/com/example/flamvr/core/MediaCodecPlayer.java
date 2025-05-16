package com.example.flamvr.core;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import com.example.flamvr.globals.StreamDataInterface;
import com.example.flamvr.globals.VideoPlaybackContract;

import java.nio.ByteBuffer;

public class MediaCodecPlayer implements VideoPlaybackContract {

    private Surface surface;
    private final Context ctx;
    private MediaExtractor videoExtractor;
    private MediaExtractor audioExtractor;
    private MediaCodec decoder;
    private Thread decodeThread;
    private boolean decoderReady = false;
    private boolean isPlaying;
    private int seekToProgress;
    private boolean performSeek = false;
    private long totalDurationMs;
    private int frameRate;
    private volatile boolean stopRequested = false;
    private StreamDataInterface.ProgressBarStream progressBarStream;
    private StreamDataInterface.VideoInfoStream videoInfoStream;
    public void addStream(StreamDataInterface.ProgressBarStream listener){
        progressBarStream = listener;
    }
    public void addStream(StreamDataInterface.VideoInfoStream listener){
        videoInfoStream = listener;
    }
    public MediaCodecPlayer(Context ctx, Surface surface) {
        this.surface = surface;
        this.ctx = ctx;
        isPlaying = false;
    }
    public void UpdateSurface(Surface surface) {
        this.surface = surface;
    }
    public void start(Uri uri) {
        decodeThread = new Thread(() -> {
            try {
                videoExtractor = new MediaExtractor();
                videoExtractor.setDataSource(ctx, uri, null);

                audioExtractor = new MediaExtractor();
                audioExtractor.setDataSource(ctx, uri, null);

                int videoTrackIndex = -1;
                int audioTrackIndex = -1;
                MediaFormat videoTrackFormat = null;
                MediaFormat audioTrackFormat = null;

                for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                    MediaFormat TrackFormat = videoExtractor.getTrackFormat(i);
                    String mime = TrackFormat.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("video/")) {
                        videoTrackIndex = i;
                        videoTrackFormat = TrackFormat;
                    } else if (mime != null && mime.startsWith("audio/")) {
                        audioTrackIndex = i;
                        audioTrackFormat = TrackFormat;
                    }
                }

                if (videoTrackIndex < 0 || videoTrackFormat == null) {
                    Log.e("MediaCodecPlayer", "No video track found");
                    return;
                }
                if (audioTrackIndex >= 0 && audioTrackFormat != null) {
                    Log.e("MediaCodecPlayer", "Audio track found");
                }

                videoExtractor.selectTrack(videoTrackIndex);
                audioExtractor.selectTrack(audioTrackIndex);
                frameRate = 30; // default
                if (videoTrackFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    frameRate = videoTrackFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                }
                String mime = videoTrackFormat.getString(MediaFormat.KEY_MIME);
                totalDurationMs = videoTrackFormat.getLong(MediaFormat.KEY_DURATION)/1000;
                progressBarStream.setMaxDuration(totalDurationMs);
                videoInfoStream.getVideoDim(videoTrackFormat.getInteger(MediaFormat.KEY_WIDTH), videoTrackFormat.getInteger(MediaFormat.KEY_HEIGHT));
                Log.d("MediaCodecPlayer", "Configuring decoder for: " + mime);

                if (surface == null || !surface.isValid()) {
                    Log.e("MediaCodecPlayer", "Surface is invalid or null");
                    return;
                }

                decoder = MediaCodec.createDecoderByType(mime);
                decoder.configure(videoTrackFormat, surface, null, 0);
                decoder.start();
                decoderReady = true;
                decodeLoop();
            } catch (Exception e) {
                Log.e("MediaCodecPlayer", "Playback error", e);
            }
        });
        decodeThread.start();
        isPlaying = true;
    }

    private void decodeLoop() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean isEOS = false;
        long playbackStartTimeNs = -1;
        while (true) {
            if (!isPlaying) {
                try {
                    Thread.sleep(50); // Sleep briefly while paused
                    playbackStartTimeNs = -1;
                    continue;
                } catch (InterruptedException e) {
                    break;
                }
            }
            if(performSeek){
                videoExtractor.seekTo(seekToProgress*totalDurationMs * 10L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                playbackStartTimeNs = -1;
                decoder.flush();
                performSeek = false;
            }
            if (!isEOS) {
                int inIndex = decoder.dequeueInputBuffer(10000);
                if (inIndex >= 0) {
                    ByteBuffer buffer = decoder.getInputBuffer(inIndex);
                    assert buffer != null;
                    int sampleSize = videoExtractor.readSampleData(buffer, 0);

                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        long pts = videoExtractor.getSampleTime();
                        progressBarStream.updateSeekBarProgress(pts/1000);
                        progressBarStream.updateFrameCount(pts * frameRate / 1_000_000L);
                        decoder.queueInputBuffer(inIndex, 0, sampleSize, pts, 0);
                        videoExtractor.advance();
                    }
                }
            }

            int outIndex = decoder.dequeueOutputBuffer(info, 10000);
            if (outIndex >= 0) {
                long presentationTimeUs = info.presentationTimeUs;
                if (playbackStartTimeNs < 0) {
                    playbackStartTimeNs = System.nanoTime() - presentationTimeUs * 1000;
                }

                long nowNs = System.nanoTime();
                long desiredTimeNs = playbackStartTimeNs + presentationTimeUs * 1000;
                long delayNs = desiredTimeNs - nowNs;

                if (delayNs > 0) {
                    try {
                        Thread.sleep(delayNs / 1_000_000, (int)(delayNs % 1_000_000));
                    } catch (InterruptedException e) {
                        // Optionally handle this
                    }
                }

                decoder.releaseOutputBuffer(outIndex, true);
            }

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("MediaCodecPlayer", "End of stream");
                break;
            }
        }
        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }
        if (videoExtractor != null) {
            videoExtractor.release();
            videoExtractor = null;
        }
        decoderReady = false;
        decodeThread = null;
        stopRequested = false;
        Log.e("MediaCodecPlayer", "Decoder thread ended");

    }
    public void stop() {
        isPlaying = false;
        stopRequested = true;
    }

    @Override
    public void onInitiate(Uri uri) {
        if(isPlaying){
            stop();
        }
        start(uri);
    }

    @Override
    public void onPlay() {
        isPlaying = true;
    }

    @Override
    public void onPause() {
        isPlaying = false;
    }

    @Override
    public void onSeek(int progress) {
        this.seekToProgress = progress;
        this.performSeek = true;
    }
}
