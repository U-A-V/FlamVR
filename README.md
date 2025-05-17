# FlamVR

**FlamVR** is an prototype Android application designed for immersive video playback with a focus on performance and low-level rendering control.

---

##  Setup Instructions

1. **Clone or unzip the project:**
   ```bash
   git clone https://github.com/yourname/FlamVR.git
   ```

2. **Open in Android Studio:**
   - File > Open > Select `FlamVR` root directory

3. **Build the project:**
   - Ensure you have Android SDK 21+ installed
   - Gradle will sync and resolve dependencies automatically

4. **Run:**
   - Connect an Android device (API 21+ recommended)
   - Press Play in Android Studio or run:
     ```bash
     ./gradlew installDebug
     ```

---

##  Architecture Overview

```
FlamVR/
├── app/
│   ├── java/com/yourdomain/flamvr/
│   │   ├── MainActivity.java         # Entry point
│   │   ├── GLRenderer.java           # Handles OpenGL ES rendering
│   │   ├── MediaCodecPlayer.java     # Decodes video/audio using MediaCodec
│   │   ├── InputHandler.java         # Manages user interaction (tap, drag)
│   │   ├── UIController.java         # Controls playback UI elements
│   │   └── StateManager.java         # Syncs app states and UI
│   └── res/
│       └── layout/                   # UI XMLs
│
└── build.gradle                      # Project config
```

## Key components
### [Model]
- `GLRenderer`: Displays video frames via OpenGL ES textures.
- `MediaCodecPlayer`: Handles video and audio decoding in sync.
### [View-Model]
- `StateManager`: Communicates app state to UI and codec player and renderer.
### [View]
- `InputHandler`: Deals with user control.
- `UIHandler`: Deals with UI based on app state and user input.

---

##  Libraries Used and Rationale

| Library           | Purpose                                | Rationale                                    |
|-------------------|----------------------------------------|----------------------------------------------|
| `MediaCodec`      | Hardware video/audio decoding          | Low-level, performant, no 3rd-party libs     |
| `OpenGL ES 3.1`   | GPU-accelerated rendering              | High-efficiency frame display                |
| `GLSurfaceView`   | Surface for OpenGL drawing             | Lifecycle-aware rendering surface            |
| Android SDK APIs  | Input, UI, threading, etc.             | No external libraries used                   |

---

##  Known Limitations / Issues

-  App may crash on devices without OpenGL ES 3.1 support.
-  No in-app settings.
-  Lacks error handling for `MediaCodec` exceptions.
-  Video stops when orientatin is changed

---

##  Future Improvements

- Add gesture controls (zoom, pan, seek)
- Add advanced playback controls (subtitles, resolution switch)
- Consider HDR video rendering support
- Add support for better Audio Streamer
- support for more filters in filter library
- custom playback speed
- inhancement in UI/UX

---
