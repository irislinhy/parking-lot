package com.example.aliyunapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class ParkingGuideView extends View {
    private Paint paint;
    private Paint linePaint;
    private Paint textPaint;
    private int mode = 0; // 0:入场, 1:出场
    private int targetSlot = -1;

    private float entranceX = 0.1f, entranceY = 0.5f;
    private float exitX = 0.9f, exitY = 0.5f;
    private float slot1X = 0.3f, slot1Y = 0.3f;
    private float slot2X = 0.7f, slot2Y = 0.7f;

    public ParkingGuideView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.RED);
        linePaint.setStrokeWidth(8);
        linePaint.setStyle(Paint.Style.STROKE);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setMode(int mode, int targetSlot) {
        this.mode = mode;
        this.targetSlot = targetSlot;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        canvas.drawColor(Color.LTGRAY);

        paint.setColor(Color.DKGRAY);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(0, height/2, width, height/2, paint);
        canvas.drawLine(width/2, 0, width/2, height, paint);

        // 入口
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);
        float ex = entranceX * width;
        float ey = entranceY * height;
        canvas.drawCircle(ex, ey, 30, paint);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("入口", ex, ey+10, textPaint);

        // 出口
        paint.setColor(Color.RED);
        float exitXp = exitX * width;
        float exitYp = exitY * height;
        canvas.drawCircle(exitXp, exitYp, 30, paint);
        canvas.drawText("出口", exitXp, exitYp+10, textPaint);

        // 车位1
        paint.setColor(Color.BLUE);
        float s1x = slot1X * width;
        float s1y = slot1Y * height;
        canvas.drawRect(s1x-50, s1y-30, s1x+50, s1y+30, paint);
        canvas.drawText("车位1", s1x, s1y+10, textPaint);

        // 车位2
        float s2x = slot2X * width;
        float s2y = slot2Y * height;
        canvas.drawRect(s2x-50, s2y-30, s2x+50, s2y+30, paint);
        canvas.drawText("车位2", s2x, s2y+10, textPaint);

        // 绘制路线
        if (mode == 0 && targetSlot != -1) {
            float endX = (targetSlot == 1) ? s1x : s2x;
            float endY = (targetSlot == 1) ? s1y : s2y;
            drawPath(canvas, ex, ey, endX, endY);
        } else if (mode == 1 && targetSlot != -1) {
            float startX = (targetSlot == 1) ? s1x : s2x;
            float startY = (targetSlot == 1) ? s1y : s2y;
            drawPath(canvas, startX, startY, exitXp, exitYp);
        }
    }

    private void drawPath(Canvas canvas, float startX, float startY, float endX, float endY) {
        Path path = new Path();
        path.moveTo(startX, startY);
        if (Math.abs(startX - endX) > Math.abs(startY - endY)) {
            path.lineTo(endX, startY);
            path.lineTo(endX, endY);
        } else {
            path.lineTo(startX, endY);
            path.lineTo(endX, endY);
        }
        canvas.drawPath(path, linePaint);
    }
}