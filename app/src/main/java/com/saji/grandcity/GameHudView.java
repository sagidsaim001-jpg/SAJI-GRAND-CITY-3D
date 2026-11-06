package com.saji.grandcity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class GameHudView extends View {
    private final GameRenderer renderer;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int joystickPointer = -1;
    private int pedalPointer = -1;
    private float joystickX;
    private float joystickY;
    private boolean go;
    private boolean stop;
    private boolean drivingMode;
    private float width;
    private float height;

    public GameHudView(Context context, GameRenderer renderer) {
        super(context);
        this.renderer = renderer;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setFocusable(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        width = getWidth();
        height = getHeight();

        paint.setColor(0x990B1725);
        canvas.drawRoundRect(new RectF(24, 20, 330, 94), 22, 22, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(27f);
        paint.setFakeBoldText(true);
        canvas.drawText("SAJI GRAND CITY 3D", 44, 54, paint);
        paint.setTextSize(15f);
        paint.setFakeBoldText(false);
        canvas.drawText("FREE ROAM  •  CITY EXPLORATION", 45, 78, paint);

        // Virtual joystick.
        float baseX = width * 0.17f;
        float baseY = height * 0.78f;
        float radius = Math.min(width, height) * 0.105f;
        paint.setColor(0x665B6B7B);
        canvas.drawCircle(baseX, baseY, radius, paint);
        paint.setColor(0xAAE8F2FA);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        canvas.drawCircle(baseX, baseY, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        float knobX = baseX + joystickX * radius * 0.72f;
        float knobY = baseY - joystickY * radius * 0.72f;
        paint.setColor(0xDDE8F2FA);
        canvas.drawCircle(knobX, knobY, radius * 0.38f, paint);

        // Driving mode toggle plus accelerator and brake.
        float buttonRadius = Math.min(width, height) * 0.075f;
        drawButton(canvas, width * 0.86f, height * 0.36f, buttonRadius * 0.82f,
                drivingMode ? "WALK" : "DRIVE", drivingMode);
        drawButton(canvas, width * 0.86f, height * 0.70f, buttonRadius, "GO", go);
        drawButton(canvas, width * 0.86f, height * 0.89f, buttonRadius, "STOP", stop);

        paint.setColor(0xCCFFFFFF);
        paint.setTextSize(16f);
        canvas.drawText("Move", width * 0.17f - 22, height * 0.94f, paint);
        paint.setTextSize(17f);
        paint.setFakeBoldText(true);
        String speedLabel = drivingMode ? String.format(java.util.Locale.US, "SPEED  %02d", Math.round(renderer.getSpeed() * 3.6f)) : "WALK MODE";
        canvas.drawText(speedLabel, width * 0.70f, height * 0.13f, paint);
        paint.setFakeBoldText(false);

        // Compact live mini-map: main roads, side streets and player heading.
        float mapX = width * 0.86f;
        float mapY = height * 0.15f;
        float mapR = Math.min(width, height) * 0.105f;
        paint.setColor(0xCC101D2A);
        canvas.drawCircle(mapX, mapY, mapR, paint);
        paint.setColor(0xFF62717C);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        canvas.drawCircle(mapX, mapY, mapR, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.clipRect(mapX-mapR+4, mapY-mapR+4, mapX+mapR-4, mapY+mapR-4);
        paint.setColor(0xFF52665B);
        canvas.drawRect(mapX-mapR, mapY-mapR, mapX+mapR, mapY+mapR, paint);
        paint.setColor(0xFFB9B5A5);
        canvas.drawRect(mapX-mapR*0.12f, mapY-mapR, mapX+mapR*0.12f, mapY+mapR, paint);
        canvas.drawRect(mapX-mapR, mapY-mapR*0.12f, mapX+mapR, mapY+mapR*0.12f, paint);
        canvas.drawRect(mapX-mapR*0.68f, mapY-mapR, mapX-mapR*0.58f, mapY+mapR, paint);
        canvas.drawRect(mapX+mapR*0.58f, mapY-mapR, mapX+mapR*0.68f, mapY+mapR, paint);
        canvas.drawRect(mapX-mapR, mapY-mapR*0.68f, mapX+mapR, mapY-mapR*0.58f, paint);
        canvas.drawRect(mapX-mapR, mapY+mapR*0.58f, mapX+mapR, mapY+mapR*0.68f, paint);
        float px = mapX + Math.max(-1f, Math.min(1f, renderer.getPlayerX()/60f)) * mapR * 0.82f;
        float py = mapY + Math.max(-1f, Math.min(1f, renderer.getPlayerZ()/75f)) * mapR * 0.82f;
        canvas.rotate((float)Math.toDegrees(renderer.getFacing()), px, py);
        paint.setColor(0xFFFFD45C);
        Path arrow = new Path();
        arrow.moveTo(px, py-mapR*0.20f);
        arrow.lineTo(px-mapR*0.13f, py+mapR*0.14f);
        arrow.lineTo(px+mapR*0.13f, py+mapR*0.14f);
        arrow.close();
        canvas.drawPath(arrow, paint);
        canvas.restore();
        paint.setColor(0xCCFFFFFF);
        paint.setTextSize(13f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("MAP", mapX, mapY+mapR+18f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(13f);
        canvas.drawText("Prototype 0.2  |  Touch controls", width * 0.40f, height - 24, paint);
    }

    private void drawButton(Canvas canvas, float x, float y, float radius, String label, boolean active) {
        paint.setColor(active ? 0xE65CBF88 : 0xAA142536);
        canvas.drawCircle(x, y, radius, paint);
        paint.setColor(0xDDEAF4FA);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        canvas.drawCircle(x, y, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(label.equals("STOP") ? 14f : 18f);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, x, y + 6f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int id = event.getPointerId(index);

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            float x = event.getX(index);
            float y = event.getY(index);
            if (x > width * 0.72f && y > height * 0.20f && y < height * 0.50f) {
                drivingMode = !drivingMode;
                renderer.setDriving(drivingMode);
            } else if (joystickPointer == -1 && x < width * 0.48f && y > height * 0.42f) {
                joystickPointer = id;
                updateJoystick(x, y);
            } else if (pedalPointer == -1 && x > width * 0.68f && y > height * 0.52f) {
                pedalPointer = id;
                updatePedal(y);
            }
            pushControls();
            invalidate();
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            for (int p = 0; p < event.getPointerCount(); p++) {
                int pointerId = event.getPointerId(p);
                if (pointerId == joystickPointer) updateJoystick(event.getX(p), event.getY(p));
                if (pointerId == pedalPointer) updatePedal(event.getY(p));
            }
            pushControls();
            invalidate();
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP
                || action == MotionEvent.ACTION_CANCEL) {
            if (id == joystickPointer || action == MotionEvent.ACTION_CANCEL) {
                joystickPointer = -1;
                joystickX = 0f;
                joystickY = 0f;
            }
            if (id == pedalPointer || action == MotionEvent.ACTION_CANCEL) {
                pedalPointer = -1;
                go = false;
                stop = false;
            }
            pushControls();
            invalidate();
            return true;
        }
        return true;
    }

    private void updateJoystick(float x, float y) {
        float cx = width * 0.17f;
        float cy = height * 0.78f;
        float radius = Math.min(width, height) * 0.105f;
        float dx = (x - cx) / radius;
        float dy = (cy - y) / radius;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length > 1f) {
            dx /= length;
            dy /= length;
        }
        joystickX = dx;
        joystickY = dy;
    }

    private void updatePedal(float y) {
        go = y < height * 0.80f;
        stop = !go;
    }

    private void pushControls() {
        renderer.setControls(joystickX, joystickY, go, stop);
    }
}
