package com.task.vasskob.dragableframelayout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by bobyk on 12.12.16.
 */

public class CircularView extends View {

    private static final int DEFAULT_CIRCLE_COLOR = Color.WHITE;

    private int circleColor = DEFAULT_CIRCLE_COLOR;
    private Paint paint;

    public CircularView(Context context) {
        super(context);
        init();
    }

    public CircularView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(6);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        int pl = getPaddingLeft();
        int pr = getPaddingRight();
        int pt = getPaddingTop();
        int pb = getPaddingBottom();

        int usableWidth = w - 6;
        int usableHeight = h - 6;

        int radius = Math.min(usableWidth, usableHeight) / 2;
        int cx = pl + (usableWidth / 2) + 3;
        int cy = pt + (usableHeight / 2) + 3;

        paint.setColor(circleColor);
        canvas.drawCircle(cx, cy, radius, paint);
    }
}
