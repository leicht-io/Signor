package com.example.signor;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public abstract class DisplayContainer extends View
{
    private static final String TAG = "DisplayContainer";

    protected static final int MARGIN = 8;

    protected int parentWidth;
    protected int parentHeight;

    protected int width;
    protected int height;
    protected int textColour;

    private Rect clipRect;
    private RectF outlineRect;

    protected Paint paint;

    // DisplayContainer
    @SuppressWarnings("deprecation")
    public DisplayContainer(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        Resources resources = getResources();

        final TypedArray typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.Siggen, 0, 0);

        textColour =
            typedArray.getColor(R.styleable.Siggen_TextColour,
                                resources.getColor(android.R.color.black));
        typedArray.recycle();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    // onMeasure
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Get the parent dimensions
        View parent = (View) getParent();
        int w = parent.getMeasuredWidth();
        int h = parent.getMeasuredHeight();

        if (w > h)
        {
            if (parentWidth < w)
                parentWidth = w;

            if (parentHeight < h)
                parentHeight = h;
        }
    }

    // onSizeChanged
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        // Save the new width and height less the clipRect
        width = w - 6;
        height = h - 6;

        // Create some rects for
        // the outline and clipping
        outlineRect = new RectF(1, 1, w - 1, h - 1);
        clipRect = new Rect(3, 3, w - 3, h - 3);
    }

    // onDraw
    @Override
    protected void onDraw(Canvas canvas)
    {
        // Set up the paint and draw the outline
        paint.setShader(null);
        paint.setStrokeWidth(3);
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(outlineRect, 10, 10, paint);

        // Set the clipRect
        canvas.clipRect(clipRect);

        // Translate to the clip rect
        canvas.translate(clipRect.left, clipRect.top);
    }
}
