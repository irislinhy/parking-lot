//车位地图自定义view
package com.example.aliyunapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;

public class ParkingMapView extends View {
    private List<ParkingSlot> slots;
    private Paint paint;
    private Paint textPaint;

    public ParkingMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setSlots(List<ParkingSlot> slots) {
        this.slots = slots;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (slots == null || slots.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();
        int cols = 2; // 两个车位并排
        int rows = 1;
        int slotWidth = width / cols;
        int slotHeight = height;

        for (int i = 0; i < slots.size(); i++) {
            ParkingSlot slot = slots.get(i);
            int col = i % cols;
            int left = col * slotWidth;
            int top = 0;
            int right = left + slotWidth;
            int bottom = slotHeight;

            // 根据状态设置颜色
            switch (slot.status) {
                case ParkingSlot.STATUS_EMPTY:
                    paint.setColor(Color.GREEN);
                    break;
                case ParkingSlot.STATUS_RESERVED:
                    paint.setColor(Color.YELLOW);
                    break;
                case ParkingSlot.STATUS_OCCUPIED:
                    paint.setColor(Color.RED);
                    break;
                case ParkingSlot.STATUS_PAY:
                    paint.setColor(Color.BLUE);
                    break;
                default:
                    paint.setColor(Color.GRAY);
            }
            canvas.drawRect(left, top, right, bottom, paint);
            // 绘制边框
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            canvas.drawRect(left, top, right, bottom, paint);
            paint.setStyle(Paint.Style.FILL);

            // 绘制车位编号和状态文字
            String text = slot.title + "\n" + slot.statusText();
            float x = left + slotWidth / 2f;
            float y = top + slotHeight / 2f;
            canvas.drawText(text, x, y, textPaint);
        }
    }
}