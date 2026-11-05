package com.saji.grandcity;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameRenderer implements GLSurfaceView.Renderer {
    private static final String VERTEX_SHADER =
            "attribute vec3 aPosition; uniform mat4 uMvp; void main(){ gl_Position=uMvp*vec4(aPosition,1.0); }";
    private static final String FRAGMENT_SHADER =
            "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }";

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] vp = new float[16];
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];

    private FloatBuffer cubeBuffer;
    private int program;
    private int positionHandle;
    private int mvpHandle;
    private int colorHandle;
    private volatile float stickX;
    private volatile float stickY;
    private volatile boolean throttle;
    private volatile boolean brake;

    private float playerX = 0f;
    private float playerZ = 8f;
    private float facing = 0f;
    private float cameraX = 0f;
    private float cameraZ = 16f;
    private long lastFrame;
    private float gameTime;
    private float speed;

    private static final float[] CUBE = {
        -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f,
        -0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
         0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
         0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
        -0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
        -0.5f,-0.5f,-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,-0.5f,
         0.5f,-0.5f, 0.5f,  0.5f,-0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
         0.5f,-0.5f, 0.5f,  0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, 0.5f,  0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f,
        -0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
        -0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f,
        -0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f, -0.5f,-0.5f, 0.5f
    };

    public void setControls(float x, float y, boolean go, boolean stop) {
        stickX = clamp(x, -1f, 1f);
        stickY = clamp(y, -1f, 1f);
        throttle = go;
        brake = stop;
    }

    public float getSpeed() { return speed; }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.50f, 0.72f, 0.88f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        mvpHandle = GLES20.glGetUniformLocation(program, "uMvp");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");

        ByteBuffer bb = ByteBuffer.allocateDirect(CUBE.length * 4);
        bb.order(ByteOrder.nativeOrder());
        cubeBuffer = bb.asFloatBuffer();
        cubeBuffer.put(CUBE);
        cubeBuffer.position(0);
        lastFrame = System.nanoTime();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / Math.max(1, height);
        Matrix.perspectiveM(projection, 0, 60f, ratio, 0.1f, 260f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        gameTime += dt;
        updatePlayer(dt);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(positionHandle);
        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, cubeBuffer);

        float followX = playerX + (float)Math.sin(facing) * 8.5f;
        float followZ = playerZ + (float)Math.cos(facing) * 8.5f;
        cameraX += (followX - cameraX) * Math.min(1f, dt * 4f);
        cameraZ += (followZ - cameraZ) * Math.min(1f, dt * 4f);
        Matrix.setLookAtM(view, 0, cameraX, 5.4f, cameraZ,
                playerX, 1.0f, playerZ - 2.2f, 0f, 1f, 0f);
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0);

        drawCity();
        drawTraffic();
        drawPlayer();
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void updatePlayer(float dt) {
        float x = stickX;
        float y = stickY;
        float forward = Math.max(0f, y);
        if (throttle) forward = Math.max(forward, 1f);
        if (brake) forward = 0f;

        float magnitude = Math.min(1f, (float)Math.sqrt(x*x + forward*forward));
        speed = magnitude * (throttle ? 15f : 9f);
        if (brake) speed = 0f;

        if (magnitude > 0.08f) {
            float dx = x;
            float dz = -forward;
            facing = (float)Math.atan2(dx, -dz);
            playerX += dx * speed * dt;
            playerZ += dz * speed * dt;
        }
        playerX = clamp(playerX, -48f, 48f);
        playerZ = clamp(playerZ, -60f, 60f);
    }

    private void drawCity() {
        // Grass and the two main roads.
        box(0f, -0.42f, 0f, 125f, 0.3f, 150f, 0.20f, 0.34f, 0.22f, 1f);
        box(0f, -0.20f, 0f, 13f, 0.12f, 150f, 0.16f, 0.17f, 0.19f, 1f);
        box(0f, -0.19f, 0f, 125f, 0.12f, 12f, 0.16f, 0.17f, 0.19f, 1f);
        box(-7.2f, -0.10f, 0f, 1.2f, 0.18f, 150f, 0.52f, 0.52f, 0.49f, 1f);
        box(7.2f, -0.10f, 0f, 1.2f, 0.18f, 150f, 0.52f, 0.52f, 0.49f, 1f);
        box(0f, -0.08f, -6.8f, 125f, 0.18f, 1.2f, 0.52f, 0.52f, 0.49f, 1f);
        box(0f, -0.08f, 6.8f, 125f, 0.18f, 1.2f, 0.52f, 0.52f, 0.49f, 1f);

        for (int z = -70; z <= 70; z += 8) {
            box(0f, -0.125f, z, 0.18f, 0.025f, 3.4f, 0.88f, 0.84f, 0.68f, 1f);
        }
        for (int x = -60; x <= 60; x += 8) {
            box(x, -0.12f, 0f, 3.4f, 0.025f, 0.18f, 0.88f, 0.84f, 0.68f, 1f);
        }

        // Repeating city blocks, varied heights and window strips.
        for (int side = -1; side <= 1; side += 2) {
            for (int i = -3; i <= 3; i++) {
                float z = i * 18f;
                float x = side * (15.5f + (i % 2 == 0 ? 0f : 1.5f));
                float height = 9f + ((i + 8) % 4) * 3.3f;
                float r = 0.32f + ((i + 8) % 3) * 0.055f;
                float g = 0.36f + ((i + 8) % 2) * 0.06f;
                box(x, height / 2f, z, 9f, height, 12f, r, g, 0.38f, 1f);
                for (int row = 1; row < height / 2.2f; row++) {
                    for (int col = -1; col <= 1; col++) {
                        float wx = x - side * 4.56f;
                        box(wx, row * 2.15f, z + col * 3.2f, 0.07f, 0.92f, 1.35f,
                                0.35f, 0.65f, 0.78f, 1f);
                    }
                }
                box(x, 0.18f, z, 9.4f, 0.32f, 12.4f, 0.24f, 0.25f, 0.27f, 1f);
            }
        }

        // Smaller houses farther from the main avenue.
        for (int side = -1; side <= 1; side += 2) {
            for (int i = -2; i <= 2; i++) {
                float z = i * 24f + 8f;
                float x = side * 31f;
                box(x, 2.4f, z, 12f, 4.8f, 12f, 0.66f, 0.49f, 0.36f, 1f);
                box(x, 5.1f, z, 12.8f, 0.6f, 12.8f, 0.30f, 0.22f, 0.20f, 1f);
                box(x - side * 6.08f, 2.4f, z, 0.08f, 1.3f, 2.0f,
                        0.25f, 0.55f, 0.72f, 1f);
            }
        }

        // Trees and street lamps along the sidewalks.
        for (int i = -4; i <= 4; i++) {
            float z = i * 14f;
            for (int side = -1; side <= 1; side += 2) {
                float x = side * 9.1f;
                box(x, 1.0f, z, 0.45f, 2.0f, 0.45f, 0.34f, 0.22f, 0.12f, 1f);
                box(x, 2.65f, z, 2.1f, 2.3f, 2.1f, 0.12f, 0.38f, 0.18f, 1f);
                float lampX = side * 6.4f;
                box(lampX, 2.4f, z + 5f, 0.12f, 4.8f, 0.12f, 0.12f, 0.13f, 0.15f, 1f);
                box(lampX, 4.8f, z + 5f, 0.8f, 0.16f, 0.5f, 0.95f, 0.83f, 0.53f, 1f);
            }
        }
    }

    private void drawTraffic() {
        for (int i = 0; i < 3; i++) {
            float z = ((gameTime * (4.0f + i) + i * 34f) % 120f) - 60f;
            float x = (i % 2 == 0) ? -3.8f : 3.8f;
            float bodyR = i == 0 ? 0.62f : (i == 1 ? 0.16f : 0.72f);
            float bodyG = i == 1 ? 0.42f : 0.20f;
            float bodyB = i == 2 ? 0.18f : 0.25f;
            box(x, 0.52f, z, 2.0f, 0.72f, 3.8f, bodyR, bodyG, bodyB, 1f);
            box(x, 1.02f, z - 0.25f, 1.45f, 0.52f, 1.75f, 0.24f, 0.38f, 0.48f, 1f);
            for (int wheel = -1; wheel <= 1; wheel += 2) {
                box(x + 1.02f * wheel, 0.28f, z - 1.15f, 0.22f, 0.48f, 0.7f, 0.06f, 0.06f, 0.07f, 1f);
                box(x + 1.02f * wheel, 0.28f, z + 1.15f, 0.22f, 0.48f, 0.7f, 0.06f, 0.06f, 0.07f, 1f);
            }
        }
    }

    private void drawPlayer() {
        float x = playerX;
        float z = playerZ;
        box(x, 1.12f, z, 0.82f, 1.05f, 0.48f, 0.10f, 0.22f, 0.42f, 1f);
        box(x, 1.92f, z, 0.48f, 0.48f, 0.46f, 0.72f, 0.52f, 0.38f, 1f);
        box(x - 0.24f, 0.42f, z, 0.28f, 0.82f, 0.32f, 0.12f, 0.13f, 0.16f, 1f);
        box(x + 0.24f, 0.42f, z, 0.28f, 0.82f, 0.32f, 0.12f, 0.13f, 0.16f, 1f);
        box(x - 0.56f, 1.12f, z, 0.25f, 0.88f, 0.28f, 0.72f, 0.52f, 0.38f, 1f);
        box(x + 0.56f, 1.12f, z, 0.25f, 0.88f, 0.28f, 0.72f, 0.52f, 0.38f, 1f);
    }

    private void box(float x, float y, float z, float sx, float sy, float sz,
                     float r, float g, float b, float a) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.scaleM(model, 0, sx, sy, sz);
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0);
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0);
        GLES20.glUniform4f(colorHandle, r, g, b, a);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    }

    private int createProgram(String vertex, String fragment) {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, vertex);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, vs);
        GLES20.glAttachShader(p, fs);
        GLES20.glLinkProgram(p);
        int[] status = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String error = GLES20.glGetProgramInfoLog(p);
            GLES20.glDeleteProgram(p);
            throw new RuntimeException("Shader link failed: " + error);
        }
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        return p;
    }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String error = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Shader compile failed: " + error);
        }
        return shader;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
