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

public class OpenGLRenderer implements GLSurfaceView.Renderer, StreamDataInterface.VideoInfoStream {
    OpenGLShader GLShader;
    private long startTime;
    public OpenGLRenderer(Context ctx){
        GLShader = new OpenGLShader(ctx);
        startTime = System.nanoTime();
    }
    private static final String TAG = OpenGLRenderer.class.getSimpleName();
    static final int BYTES_PER_FLOAT = 4;
    static final int BYTES_PER_SHORT = 2;
    static final int FLOATS_PER_VERTEX = 5;
    static float edgeX = 1.0f;
    static float edgeY = 1.0f;
    static float screenAspect = 1.0f;
    static float videoAspect = 1.0f;
    float[] fvVerticesData = new float[]{
            edgeX, edgeY, 0.0f, 1.0f, 0.0f,
            edgeX, -edgeY, 0.0f, 1.0f, 1.0f,
            -edgeX, -edgeY, 0.0f, 0.0f, 1.0f,
            -edgeX, edgeY, 0.0f, 0.0f, 0.0f,
    };
    short[] svIndexData = new short[]{
            0, 1, 2,
            2, 3, 0
    };
    private int[] glProgram = new int[FILTERS.values().length];
    private int glVAO, glVBO, glEBO;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private int textureId;
    private int textureHandle;
    private OnSurfaceReadyCallback surfaceReadyCallback;
    private final float[] transformMatrix = new float[16];
    private int transformMatrixHandle;
    private boolean changeAspect = false;
    private boolean changeFilter = false;
    private int filterID = FILTERS.NONE.ordinal();
    static int glPosition;
    static int glTexCoord;
    public interface OnSurfaceReadyCallback {
        void onSurfaceReady(Surface surface);
    }

    public void setOnSurfaceReadyCallback(OnSurfaceReadyCallback callback) {
        this.surfaceReadyCallback = callback;
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.e(TAG, "RENDERER: CREATED");
        textureId = createExternalTexture();
        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setDefaultBufferSize(1920, 1080); // can adjust later

        surface = new Surface(surfaceTexture);

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
    private void SetFilter(int filterID){
        GLES31.glUseProgram(glProgram[filterID]);
        glPosition = GLES31.glGetAttribLocation(glProgram[filterID], "aPosition");
        glTexCoord = GLES31.glGetAttribLocation(glProgram[filterID], "aTexCoord");
        textureHandle = GLES31.glGetUniformLocation(glProgram[filterID], "uTexture");
        transformMatrixHandle = GLES31.glGetUniformLocation(glProgram[filterID], "uTransform");
        GLES31.glUniform1i(textureHandle, 0); // bind texture unit 0
    }
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
    private FloatBuffer convertToFloatBuffer(float[] data){
        ByteBuffer bufferDataByte = ByteBuffer.allocateDirect(data.length*BYTES_PER_FLOAT).order(ByteOrder.nativeOrder());
        FloatBuffer bufferData = bufferDataByte.asFloatBuffer();
        bufferData.put(data).position(0);
        return bufferData;
    }
    private ShortBuffer convertToShortBuffer(short[] data){
        ByteBuffer bufferDataByte = ByteBuffer.allocateDirect(data.length*BYTES_PER_SHORT).order(ByteOrder.nativeOrder());
        ShortBuffer bufferData = bufferDataByte.asShortBuffer();
        bufferData.put(data).position(0);
        return bufferData;
    }
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

    @Override
    public void getFilter(int id) {
        filterID = id;
        changeFilter = true;
    }

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
