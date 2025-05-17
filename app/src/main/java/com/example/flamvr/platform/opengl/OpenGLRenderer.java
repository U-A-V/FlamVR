package com.example.flamvr.platform.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;
import android.opengl.GLES11Ext;

import com.example.flamvr.globals.FILTERS;
import com.example.flamvr.globals.StreamDataInterface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * OpenGLRenderer handles rendering video frames using OpenGL ES 3.1.
 * It supports different shader filters and manages video aspect ratio.
 */
public class OpenGLRenderer implements GLSurfaceView.Renderer, StreamDataInterface.VideoInfoStream {

    // Shader utility class for compiling shaders and creating programs
    OpenGLShader GLShader;
    private long startTime;

    public OpenGLRenderer(Context ctx){
        GLShader = new OpenGLShader(ctx);
        startTime = System.nanoTime();
    }
    private static final String TAG = OpenGLRenderer.class.getSimpleName();

    // Constants for byte size of data types
    static final int BYTES_PER_FLOAT = 4;
    static final int BYTES_PER_SHORT = 2;
    static final int FLOATS_PER_VERTEX = 5;
    static float edgeX = 1.0f;
    static float edgeY = 1.0f;
    static float screenAspect = 1.0f;
    static float videoAspect = 1.0f;

    // Vertex data array (4 vertices with 3 position + 2 texture coords each
    float[] fvVerticesData = new float[]{
            edgeX, edgeY, 0.0f, 1.0f, 0.0f,
            edgeX, -edgeY, 0.0f, 1.0f, 1.0f,
            -edgeX, -edgeY, 0.0f, 0.0f, 1.0f,
            -edgeX, edgeY, 0.0f, 0.0f, 0.0f,
    };

    // Indices for two triangles forming the rectangle
    short[] svIndexData = new short[]{
            0, 1, 2,
            2, 3, 0
    };

    // One OpenGL program per filter type
    private int[] glProgram = new int[FILTERS.values().length];

    // OpenGL handles for Vertex Array Object, Vertex Buffer Object, Element Buffer Object
    private int glVAO, glVBO, glEBO;

    // SurfaceTexture and Surface for external texture rendering
    private SurfaceTexture surfaceTexture;
    private Surface surface;

    // Texture ID and uniform/attribute handles
    private int textureId;
    private int textureHandle;

    // Callback interface to notify when Surface is ready
    private OnSurfaceReadyCallback surfaceReadyCallback;

    // Matrix to hold texture transform for proper video orientation
    private final float[] transformMatrix = new float[16];

    // Flags to trigger updates on aspect ratio or filter change
    private int transformMatrixHandle;
    private boolean changeAspect = false;
    private boolean changeFilter = false;

    // Current filter ID from FILTERS enum
    private int filterID = FILTERS.NONE.ordinal();

    // Attribute locations for shader inputs
    static int glPosition;
    static int glTexCoord;

    /**
     * Interface for callback when the Surface is ready.
     */
    public interface OnSurfaceReadyCallback {
        void onSurfaceReady(Surface surface);
    }

    /**
     * Set the callback to be notified when the Surface is ready.
     */

    public void setOnSurfaceReadyCallback(OnSurfaceReadyCallback callback) {
        this.surfaceReadyCallback = callback;
    }

    /**
     * Returns the SurfaceTexture used to update video frames.
     */
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    /**
     * Called once when the surface is created. Initializes GL objects and shaders.
     */
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.e(TAG, "RENDERER: CREATED");
        // Create an external texture for video frames
        textureId = createExternalTexture();

        // Create SurfaceTexture from texture ID for receiving frames from MediaCodec
        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setDefaultBufferSize(1920, 1080); // can adjust later

        // Create Surface from SurfaceTexture to pass to MediaCodec or other producers
        surface = new Surface(surfaceTexture);


        // Notify listener surface is ready to be used
        if (surfaceReadyCallback != null) {
            surfaceReadyCallback.onSurfaceReady(surface);
        }
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        //compile vert shader
        int vertShaderID = GLShader.compileShader("simple", GLES31.GL_VERTEX_SHADER);

        //compile frag shader
        int fragShaderID = GLShader.compileShader("simple", GLES31.GL_FRAGMENT_SHADER);

        //create program and attach shaders
        glProgram[FILTERS.NONE.ordinal()] = GLShader.createProgram(vertShaderID, fragShaderID);

        fragShaderID = GLShader.compileShader("Luminance", GLES31.GL_FRAGMENT_SHADER);
        glProgram[FILTERS.FILTER1.ordinal()] = GLShader.createProgram(vertShaderID, fragShaderID);

        fragShaderID = GLShader.compileShader("Dither", GLES31.GL_FRAGMENT_SHADER);
        glProgram[FILTERS.FILTER2.ordinal()] = GLShader.createProgram(vertShaderID, fragShaderID);

        fragShaderID = GLShader.compileShader("Sketch", GLES31.GL_FRAGMENT_SHADER);
        glProgram[FILTERS.FILTER3.ordinal()] = GLShader.createProgram(vertShaderID, fragShaderID);

        //bind the program with default filter
        SetFilter(filterID);

        // gen VAO
        int[] tmp = new int[2];
        GLES31.glGenVertexArrays(1, tmp, 0);
        glVAO = tmp[0];
        //gen VBO
        GLES31.glGenBuffers(2, tmp, 0);
        glVBO = tmp[0];
        glEBO = tmp[1];
        updateVertexArray();
    }

    /**
     * Bind shader program and set attribute/uniform locations for current filter.
     */
    private void SetFilter(int filterID){
        GLES31.glUseProgram(glProgram[filterID]);
        glPosition = GLES31.glGetAttribLocation(glProgram[filterID], "aPosition");
        glTexCoord = GLES31.glGetAttribLocation(glProgram[filterID], "aTexCoord");
        textureHandle = GLES31.glGetUniformLocation(glProgram[filterID], "uTexture");
        transformMatrixHandle = GLES31.glGetUniformLocation(glProgram[filterID], "uTransform");
        GLES31.glUniform1i(textureHandle, 0); // bind texture unit 0
    }

    /**
     * Uploads vertex and index data to GPU buffers and sets vertex attribute pointers.
     */
    private void updateVertexArray(){
        //bind VAO
        GLES31.glBindVertexArray(glVAO);

        //upload vertex data to VBO
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, glVBO);
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER,fvVerticesData.length*BYTES_PER_FLOAT, convertToFloatBuffer(fvVerticesData), GLES31.GL_STATIC_DRAW);
        GLES31.glBindBuffer(GLES31.GL_ELEMENT_ARRAY_BUFFER, glEBO);
        GLES31.glBufferData(GLES31.GL_ELEMENT_ARRAY_BUFFER,svIndexData.length*BYTES_PER_SHORT, convertToShortBuffer(svIndexData), GLES31.GL_STATIC_DRAW);

        GLES31.glEnableVertexAttribArray(glPosition);
        GLES31.glVertexAttribPointer(glPosition, 3, GLES31.GL_FLOAT, false, BYTES_PER_FLOAT*FLOATS_PER_VERTEX, 0);
        GLES31.glEnableVertexAttribArray(glTexCoord);
        GLES31.glVertexAttribPointer(glTexCoord,2, GLES31.GL_FLOAT, false, BYTES_PER_FLOAT*FLOATS_PER_VERTEX, 3*BYTES_PER_FLOAT);
    }

    /**
     * Utility to convert float array to FloatBuffer for OpenGL.
     */
    private FloatBuffer convertToFloatBuffer(float[] data){
        ByteBuffer bufferDataByte = ByteBuffer.allocateDirect(data.length*BYTES_PER_FLOAT).order(ByteOrder.nativeOrder());
        FloatBuffer bufferData = bufferDataByte.asFloatBuffer();
        bufferData.put(data).position(0);
        return bufferData;
    }

    /**
     * Utility to convert short array to ShortBuffer for OpenGL.
     */
    private ShortBuffer convertToShortBuffer(short[] data){
        ByteBuffer bufferDataByte = ByteBuffer.allocateDirect(data.length*BYTES_PER_SHORT).order(ByteOrder.nativeOrder());
        ShortBuffer bufferData = bufferDataByte.asShortBuffer();
        bufferData.put(data).position(0);
        return bufferData;
    }

    /**
     * Called on surface size or orientation changes.
     * Adjusts viewport and vertex coordinates for proper aspect ratio.
     */
    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        Log.e(TAG, "RENDERER: Changed");
        GLES31.glViewport(0, 0, i, i1);
        screenAspect = i/(float)i1;
        edgeX = 1.0f;
        edgeY = 1.0f;

        if (videoAspect > screenAspect) {
            // Video is wider than screen (letterbox)
            edgeY = screenAspect / videoAspect;
        } else {
            // Video is taller than screen (pillarbox)
            edgeX = videoAspect / screenAspect;
        }
        fvVerticesData[0] = edgeX;
        fvVerticesData[5] = edgeX;
        fvVerticesData[10] = -edgeX;
        fvVerticesData[15] = -edgeX;

        fvVerticesData[1] = edgeY;
        fvVerticesData[6] = -edgeY;
        fvVerticesData[11] = -edgeY;
        fvVerticesData[16] = edgeY;
        changeAspect = true;
    }
    /**
     * Receives video dimensions from stream and recalculates vertex coordinates accordingly.
     */
    @Override
    public void getVideoDim(int width, int height) {
        videoAspect = width/(float)height;
        edgeX = 1.0f;
        edgeY = 1.0f;

        if (videoAspect > screenAspect) {
            // Video is wider than screen (letterbox)
            edgeY = screenAspect / videoAspect;
        } else {
            // Video is taller than screen (pillarbox)
            edgeX = videoAspect / screenAspect;
        }
        fvVerticesData[0] = edgeX;
        fvVerticesData[5] = edgeX;
        fvVerticesData[10] = -edgeX;
        fvVerticesData[15] = -edgeX;

        fvVerticesData[1] = edgeY;
        fvVerticesData[6] = -edgeY;
        fvVerticesData[11] = -edgeY;
        fvVerticesData[16] = edgeY;
        changeAspect = true;
    }

    /**
     * Receives filter id from stream and assign it to program.
     */
    @Override
    public void getFilter(int id) {
        filterID = id;
        changeFilter = true;
    }

    /**
     * called every frame to draw
     */
    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES31.glUseProgram(glProgram[filterID]);
        if(changeAspect || changeFilter){
            SetFilter(filterID);
            updateVertexArray();
            changeAspect = false;
            changeFilter = false;
        }
        if (surfaceTexture != null) {
            surfaceTexture.updateTexImage();
        }
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT|GLES31.GL_DEPTH_BUFFER_BIT);

        surfaceTexture.getTransformMatrix(transformMatrix);
        GLES31.glUniformMatrix4fv(transformMatrixHandle, 1, false, transformMatrix, 0);
        // Bind external texture to unit 0
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES31.glBindVertexArray(glVAO);
        GLES31.glDrawElements(GLES31.GL_TRIANGLES,svIndexData.length, GLES31.GL_UNSIGNED_SHORT,0);
    }

    /**
     * creates external texture for surface
     */
    private int createExternalTexture() {
        int[] textures = new int[1];
        GLES31.glGenTextures(1, textures, 0);
        int texture = textures[0];
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
        return texture;
    }
}
