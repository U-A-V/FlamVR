package com.example.flamvr.core;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;

public class MediaCodecPlayer {
    private Surface surface;
    private final Context ctx;
    private MediaExtractor extractor;
    private MediaCodec decoder;
    private Thread decodeThread;
    private boolean isPlaying = false;
    public MediaCodecPlayer(Context ctx, Surface surface) {
        this.surface = surface;
        this.ctx = ctx;
    }
    public void UpdateSurface(Surface surface) {
        this.surface = surface;
    }
    public void start(Uri uri) {
        decodeThread = new Thread(() -> {
            try {
                extractor = new MediaExtractor();
                extractor.setDataSource(ctx, uri, null);

                int videoTrackIndex = -1;
                MediaFormat format = null;

                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("video/")) {
                        videoTrackIndex = i;
                        break;
                    }
                }

                if (videoTrackIndex < 0 || format == null) {
                    Log.e("MediaCodecPlayer", "No video track found");
                    return;
                }

                extractor.selectTrack(videoTrackIndex);

                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.d("MediaCodecPlayer", "Configuring decoder for: " + mime);

                if (surface == null || !surface.isValid()) {
                    Log.e("MediaCodecPlayer", "Surface is invalid or null");
                    return;
                }

                decoder = MediaCodec.createDecoderByType(mime);
                decoder.configure(format, surface, null, 0);
                decoder.start();

                isPlaying = true;
                decodeLoop();

            } catch (Exception e) {
                Log.e("MediaCodecPlayer", "Playback error", e);
            }
        });
        decodeThread.start();
    }

    private void decodeLoop() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean isEOS = false;
        long playbackStartTimeNs = -1;
        while (isPlaying) {
            if (!isEOS) {
                int inIndex = decoder.dequeueInputBuffer(10000);
                if (inIndex >= 0) {
                    ByteBuffer buffer = decoder.getInputBuffer(inIndex);
                    assert buffer != null;
                    int sampleSize = extractor.readSampleData(buffer, 0);

                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        long pts = extractor.getSampleTime();
                        decoder.queueInputBuffer(inIndex, 0, sampleSize, pts, 0);
                        extractor.advance();
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

        decoder.stop();
        decoder.release();
        extractor.release();
    }

    public void stop() {
        isPlaying = false;
    }
}
