package com.example.signor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;

public class Scale extends DisplayContainer {
    private static final int SCALE = 500;

    private int value;

    private BitmapShader shader;
    private Matrix matrix;

    public Scale(Context context, AttributeSet attrs) {
        super(context, attrs);

        matrix = new Matrix();
    }

    protected void setValue(int value) {
        this.value = value;

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = (parentWidth - MARGIN) / 2;
        int height = parentHeight / 5;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (shader == null) {
            return;
        }

        paint.setShader(shader);
        paint.setStyle(Paint.Style.FILL);
        matrix.setTranslate(width / 2 + (value * width) / SCALE, 0);
        shader.setLocalMatrix(matrix);
        canvas.drawRect(0, 0, width, height, paint);

        paint.setShader(null);
        paint.setAntiAlias(false);
        paint.setColor(textColour);
        paint.setStrokeWidth(2);
        canvas.drawLine(width / 2, 0, width / 2, height, paint);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        if (width > 0 && height > 0) {
            Bitmap bitmap = Bitmap.createBitmap(this.width * 2, this.height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            paint.setStrokeWidth(2);
            paint.setColor(textColour);
            paint.setAntiAlias(false);
            for (int i = 1; i <= 10; i++) {
                float x = (float) Math.log10(i) * this.width;

                for (int j = 0; j < 2; j++) {
                    canvas.drawLine(x, this.height * 2 / 3, x, this.height - MARGIN, paint);
                    x += this.width;
                }
            }

            for (int i = 3; i < 20; i += 2) {
                float x = (float) (Math.log10(i / 2.0) * this.width);

                for (int j = 0; j < 2; j++) {
                    canvas.drawLine(x, this.height * 5 / 6, x, this.height - MARGIN, paint);
                    x += this.width;
                }
            }

            paint.setTextSize(this.height * 7 / 16);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setTextAlign(Paint.Align.CENTER);

            int a[] = {1, 2, 3, 4, 6, 8};
            for (int n : a) {
                float x = (float) (Math.log10(n) * this.width);

                canvas.drawText(n + "", x, this.height / 2, paint);
                canvas.drawText(n * 10 + "", x + this.width, this.height / 2, paint);
            }

            canvas.drawText("1", this.width * 2, this.height / 2, paint);

            shader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
        }
    }
}
