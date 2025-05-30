package com.example.flamvr.platform.opengl;

import android.content.Context;
import android.opengl.GLES31;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class OpenGLShader {
    private final Context ctx;

    // Constructor stores the Android Context for asset access
    public OpenGLShader(Context ctx){
        this.ctx = ctx;
    }
    private static final String TAG = OpenGLShader.class.getSimpleName();

    /**
     * Reads a shader source file from the assets folder.
     * @param fileName The name of the shader file (e.g., "simple.vert").
     * @return The shader source code as a String.
     */
    private String readAssetFile(String fileName){
        BufferedReader reader = null;
        try{
            reader = new BufferedReader(new InputStreamReader(ctx.getAssets().open(fileName)));
            StringBuilder sb = new StringBuilder();
            String mLine;
            while((mLine = reader.readLine()) != null){
                sb.append(mLine);
                sb.append("\n");
            }
            return sb.toString();
        } catch(IOException e){
            Log.e(TAG, e.toString());
        }finally {
            if(reader != null){
                try{
                    reader.close();
                }catch (IOException e){
                    Log.e(TAG, e.toString());
                }
            }
        }
        return null;
    }

    /**
     * Compiles a shader from source code stored in the assets folder.
     * @param name The base name of the shader file without extension (e.g., "simple").
     * @param type The type of shader: GLES31.GL_VERTEX_SHADER or GLES31.GL_FRAGMENT_SHADER.
     * @return The OpenGL shader ID or -1 if compilation failed.
     */
    public int compileShader(String name, int type){
        String shaderCode;
        if(type == GLES31.GL_VERTEX_SHADER){
            shaderCode = readAssetFile(name + ".vert");
        } else {
            shaderCode = readAssetFile(name + ".frag");
        }
        // compile the shader
        int shaderID = GLES31.glCreateShader(type);
        if (shaderCode == null) {
            throw new IllegalArgumentException("Shader source is null");
        }
        GLES31.glShaderSource(shaderID, shaderCode);
        GLES31.glCompileShader(shaderID);
        //get shader compile status
        final int[] compileStatus = new int[1];
        GLES31.glGetShaderiv(shaderID, GLES31.GL_COMPILE_STATUS, compileStatus, 0);
        if(compileStatus[0]==0){
            String str = GLES31.glGetShaderInfoLog(shaderID);
            Log.e(TAG, "Error compiling shader: " + str);
            GLES31.glDeleteShader(shaderID);
            return -1;
        }
        return shaderID;
    }

    /**
     * Creates an OpenGL program by linking a vertex shader and a fragment shader.
     * @param vertID The OpenGL ID of the compiled vertex shader.
     * @param fragID The OpenGL ID of the compiled fragment shader.
     * @return The OpenGL program ID.
     */
    public int createProgram(int vertID, int fragID){
        //create program and attach shaders
        int programID = GLES31.glCreateProgram();
        GLES31.glAttachShader(programID, vertID);
        GLES31.glAttachShader(programID, fragID);
        GLES31.glLinkProgram(programID);

        int[] success = new int[1];
        GLES31.glGetProgramiv(programID, GLES31.GL_LINK_STATUS, success, 0);
        if(success[0]==0){
            String str = GLES31.glGetProgramInfoLog(programID);
            Log.e(TAG, "Error Linking shader: " + str);
        }
        return programID;
    }
}
