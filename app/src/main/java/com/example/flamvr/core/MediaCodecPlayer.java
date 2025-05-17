package com.example.flamvr.core;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.example.flamvr.globals.StreamDataInterface;
import com.example.flamvr.globals.VideoPlaybackContract;

import java.nio.ByteBuffer;

public class MediaCodecPlayer implements VideoPlaybackContract {

    private Surface surface; // Surface where video frames will be rendered
    private final Context ctx;

    // MediaExtractor instances for extracting video and audio tracks
    private MediaExtractor videoExtractor;
    private MediaExtractor audioExtractor;

    // MediaCodec instances for decoding video and audio streams

    private MediaCodec videoDecoder;
    private MediaCodec audioDecoder;
    private AudioTrack audioTrack; // AudioTrack for PCM audio playback

    // Threads for decoding video and audio streams concurrently
    private Thread videodecodeThread;
    private Thread audiodecodeThread;

    // Flags to indicate if decoders are ready
    private boolean videodecoderReady = false;
    private boolean audiodecoderReady = false;

    // Playback control variables
    public boolean isPlaying;
    private int seekToProgress;
    private boolean performSeek = false;
    private long totalDurationMs;
    private int frameRate;

    // Playback timing variables
    private long playbackStartTimeNs = -1;
    private float playBackSpeed = 1.0f;
    private volatile boolean stopRequested = false;

    // Listeners for updating UI or playback progress
    private StreamDataInterface.ProgressBarStream progressBarStream;
    private StreamDataInterface.VideoInfoStream videoInfoStream;
    // Add listener to receive progress bar updates
    public void addStream(StreamDataInterface.ProgressBarStream listener){
        progressBarStream = listener;
    }
    // Add listener to receive video dimension info
    public void addStream(StreamDataInterface.VideoInfoStream listener){
        videoInfoStream = listener;
    }
    // Constructor takes app context and Surface for video rendering
    public MediaCodecPlayer(Context ctx, Surface surface) {
        this.surface = surface;
        this.ctx = ctx;
        isPlaying = false;
    }
    // Update the rendering Surface (e.g. if SurfaceView recreated)
    public void UpdateSurface(Surface surface) {
        this.surface = surface;
    }
    // Starts playback of a video from the given URI
    public void start(Uri uri) {
        // Start video decoding thread
        videodecodeThread = new Thread(() -> {
            try {
                // Setup video extractor and find video track
                videoExtractor = new MediaExtractor();
                videoExtractor.setDataSource(ctx, uri, null);

                int videoTrackIndex = -1;
                MediaFormat videoTrackFormat = null;

                for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                    MediaFormat TrackFormat = videoExtractor.getTrackFormat(i);
                    String mime = TrackFormat.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("video/")) {
                        videoTrackIndex = i;
                        videoTrackFormat = TrackFormat;
                    }
                }

                if (videoTrackIndex < 0 || videoTrackFormat == null) {
                    Log.e("MediaCodecPlayer", "No video track found");
                    return;
                }
                // Select the video track for extraction
                videoExtractor.selectTrack(videoTrackIndex);

                // Get frame rate if available, default to 30 fps
                frameRate = 30; // default
                if (videoTrackFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    frameRate = videoTrackFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                }

                // Get MIME type and total duration (in microseconds)
                String mime = videoTrackFormat.getString(MediaFormat.KEY_MIME);
                totalDurationMs = videoTrackFormat.getLong(MediaFormat.KEY_DURATION)/1000;

                // Update progress bar max duration and video info listeners
                progressBarStream.setMaxDuration(totalDurationMs);
                videoInfoStream.getVideoDim(videoTrackFormat.getInteger(MediaFormat.KEY_WIDTH), videoTrackFormat.getInteger(MediaFormat.KEY_HEIGHT));
                Log.d("MediaCodecPlayer", "Configuring decoder for: " + mime);

                // Ensure the surface is valid for rendering
                if (surface == null || !surface.isValid()) {
                    Log.e("MediaCodecPlayer", "Surface is invalid or null");
                    return;
                }

                // Create and configure video decoder with output surface
                videoDecoder = MediaCodec.createDecoderByType(mime);
                videoDecoder.configure(videoTrackFormat, surface, null, 0);
                videoDecoder.start();

                // Enter the main video decode loop
                videodecoderReady = true;
                videodecodeLoop();
            } catch (Exception e) {
                Log.e("MediaCodecPlayer", "Playback error", e);
            }
        });
        // Start audio decoding thread
        audiodecodeThread = new Thread(() -> {
            try {
                // Setup audio extractor and find audio track
                audioExtractor = new MediaExtractor();
                audioExtractor.setDataSource(ctx, uri, null);

                int audioTrackIndex = -1;
                MediaFormat audioTrackFormat = null;

                for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                    MediaFormat TrackFormat = audioExtractor.getTrackFormat(i);
                    String mime = TrackFormat.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("audio/")) {
                        audioTrackIndex = i;
                        audioTrackFormat = TrackFormat;
                    }
                }
                if (audioTrackIndex >= 0 && audioTrackFormat != null) {
                    Log.e("MediaCodecPlayer", "Audio track found");
                }
                // Select the audio track for extraction
                audioExtractor.selectTrack(audioTrackIndex);

                // Audio decoder doesn't require a surface
                String mime = audioTrackFormat.getString(MediaFormat.KEY_MIME);
                Log.d("MediaCodecPlayer", "Configuring decoder for: " + mime);

                if (surface == null || !surface.isValid()) {
                    Log.e("MediaCodecPlayer", "Surface is invalid or null");
                    return;
                }
                // Setup AudioTrack for PCM output
                audioDecoder = MediaCodec.createDecoderByType(audioTrackFormat.getString(MediaFormat.KEY_MIME));
                audioDecoder.configure(audioTrackFormat, null, null, 0);
                audioDecoder.start();
                int sampleRate = audioTrackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int channelCount = audioTrackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                int channelConfig = channelCount == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;

                // Calculate minimum buffer size and create AudioTrack in streaming mode
                int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                audioTrack.play();
                // Enter the main audio decode loop
                audiodecoderReady = true;
                audiodecodeLoop();
            } catch (Exception e) {
                Log.e("MediaCodecPlayer", "Playback error", e);
            }
        });

        // Start both decode threads concurrently
        videodecodeThread.start();
        audiodecodeThread.start();
        isPlaying = true;
        Log.e("MediaCodecPlayer", "PLAY STARTED!!!");
    }

    // Main loop for decoding and rendering video frames
    private void videodecodeLoop() {
        MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
        boolean videoEOS = false;
        playbackStartTimeNs = -1; // reset playback start time
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
                // Seek video extractor to desired progress time (converted to microseconds)
                videoExtractor.seekTo(seekToProgress*totalDurationMs * 10L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                playbackStartTimeNs = -1;
                videoDecoder.flush(); // Flush decoder buffers
                performSeek = false;
            }
            // Feed video input
            if (!videoEOS) {
                int inIndex = videoDecoder.dequeueInputBuffer(10000);
                if (inIndex >= 0) {
                    ByteBuffer buffer = videoDecoder.getInputBuffer(inIndex);
                    assert buffer != null;
                    int sampleSize = videoExtractor.readSampleData(buffer, 0);

                    if (sampleSize < 0) {
                        // End of stream signaled by negative sample size
                        videoDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        videoEOS = true;
                    } else {
                        long pts = videoExtractor.getSampleTime();
                        progressBarStream.updateSeekBarProgress(pts/1000);
                        progressBarStream.updateFrameCount(pts * frameRate / 1_000_000L);
                        videoDecoder.queueInputBuffer(inIndex, 0, sampleSize, pts, 0);
                        videoExtractor.advance();
                    }
                }
            }
            // Handle video output
            int videoOutIndex = videoDecoder.dequeueOutputBuffer(videoInfo, 10000);
            if (videoOutIndex >= 0) {
                // Calculate target render time and sleep to sync playback speed
                long presentationTimeUs = (long) (videoInfo.presentationTimeUs / playBackSpeed);
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
                // Release decoded frame to Surface for rendering
                videoDecoder.releaseOutputBuffer(videoOutIndex, true);
            }

            if ((videoInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("MediaCodecPlayer", "End of stream");
                break;
            }
        }
        // Cleanup video decoder and extractor on exit
        if (videoDecoder != null) {
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
        }
        if (videoExtractor != null) {
            videoExtractor.release();
            videoExtractor = null;
        }
        videodecoderReady = false;
        videodecodeThread = null;
        stopRequested = false;
        Log.e("MediaCodecPlayer", "video Decoder thread ended");

    }

    // Main loop for decoding audio and playing PCM via AudioTrack
    private void audiodecodeLoop() {
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        boolean audioEOS = false;
        playbackStartTimeNs = -1;
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
                // Seek audio extractor to desired progress time
                audioExtractor.seekTo(seekToProgress*totalDurationMs * 10L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                playbackStartTimeNs = -1;
                audioDecoder.flush();
                performSeek = false;
            }
            if(playbackStartTimeNs == -1){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        PlaybackParams params = audioTrack.getPlaybackParams();
                        params.setSpeed(playBackSpeed);
                        params.setPitch(1.0f);
                        audioTrack.setPlaybackParams(params);
                    } catch (Exception e) {
                        Log.e("AudioTrack", "Failed to set playback speed", e);
                    }
                }
            }
            // Feed audio input
            if (!audioEOS) {
                int inIndex = audioDecoder.dequeueInputBuffer(10000);
                if (inIndex >= 0) {
                    ByteBuffer buffer = audioDecoder.getInputBuffer(inIndex);
                    assert buffer != null;
                    int size = audioExtractor.readSampleData(buffer, 0);
                    if (size < 0) {
                        audioDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        audioEOS = true;
                    } else {
                        long pts = audioExtractor.getSampleTime();
                        audioDecoder.queueInputBuffer(inIndex, 0, size, pts, 0);
                        audioExtractor.advance();
                    }
                }
            }
            // Get decoded PCM output from audio decoder
            int audioOutIndex = audioDecoder.dequeueOutputBuffer(audioInfo, 10000);
            if (audioOutIndex >= 0) {
                ByteBuffer outBuffer = audioDecoder.getOutputBuffer(audioOutIndex);
                byte[] audioData = new byte[audioInfo.size];
                outBuffer.get(audioData);
                outBuffer.clear();
                audioTrack.write(audioData, 0, audioData.length);
                audioDecoder.releaseOutputBuffer(audioOutIndex, false);
            }
        }
        // Cleanup audio decoder, extractor and audio track
        if (audioDecoder != null) {
            audioDecoder.stop();
            audioDecoder.release();
            audioDecoder = null;
        }
        if (audioExtractor != null) {
            audioExtractor.release();
            audioExtractor = null;
        }
        audiodecoderReady = false;
        audiodecodeThread = null;
        stopRequested = false;
        Log.e("MediaCodecPlayer", "audio Decoder thread ended");

    }

    // Stop playback and cleanup resources
    public void stop() {
        isPlaying = false;
        stopRequested = true;
        if (videodecodeThread != null) {
            videodecodeThread.interrupt();
        }
        if (audiodecodeThread != null) {
            audiodecodeThread.interrupt();
        }
    }
    //Loads a video file and starts playback
    @Override
    public void onInitiate(Uri uri) {
        start(uri);
    }

    // Resume playback from paused state
    @Override
    public void onPlay() {
        isPlaying = true;
    }

    // Pause playback (video and audio decode loops sleep)
    @Override
    public void onPause() {
        isPlaying = false;
    }

    // Request a seek to a specific progress (percentage or relative position)
    @Override
    public void onSeek(int progress) {
        this.seekToProgress = progress;
        this.performSeek = true;
    }

    // Change playback speed multiplier (e.g. 0.5x, 1x, 2x)
    @Override
    public void onPlaybackChanged(String speed) {
        switch (speed){
            case "0.5x":
                playBackSpeed = 0.5f;
                break;
            case "1.0x":
                playBackSpeed = 1.0f;
                break;
                case "1.5x":
                playBackSpeed = 1.5f;
                break;
            case "2.0x":
                playBackSpeed = 2.0f;
                break;
            default:
                playBackSpeed = 1.0f;
                break;
        }
        playbackStartTimeNs = -1;
    }
}
