package com.saji.grandcity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.opengl.GLSurfaceView;

public class GameActivity extends Activity {
    private GLSurfaceView gameSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = new FrameLayout(this);
        gameSurface = new GLSurfaceView(this);
        gameSurface.setEGLContextClientVersion(2);
        gameSurface.setPreserveEGLContextOnPause(true);

        GameRenderer renderer = new GameRenderer();
        gameSurface.setRenderer(renderer);
        gameSurface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        GameHudView hud = new GameHudView(this, renderer);
        root.addView(gameSurface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(hud, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        hideSystemBars();
    }

    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameSurface != null) gameSurface.onResume();
        hideSystemBars();
    }

    @Override
    protected void onPause() {
        if (gameSurface != null) gameSurface.onPause();
        super.onPause();
    }
}
