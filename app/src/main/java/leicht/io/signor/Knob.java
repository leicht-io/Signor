package leicht.io.signor;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

// Knob
public class Knob extends View
        implements View.OnClickListener, GestureDetector.OnGestureListener,
        ValueAnimator.AnimatorUpdateListener {
    private static final int MARGIN = 8;

    private static final float MIN = -400;
    private static final float MAX = 680;
    private static final int SCALE = 50;
    private static final int VELOCITY = 75;

    private int parentWidth;
    private int parentHeight;

    private int width;
    private int height;
    private final int backgroundColour;

    private boolean move;
    private float value;
    private float last;

    private final Matrix matrix;
    private Paint paint;
    private LinearGradient gradient;
    private LinearGradient dimple;
    private final GestureDetector detector;

    private OnKnobChangeListener listener;

    public Knob(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources resources = getResources();

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Signor, 0, 0);
        // TODO: Change way to get resources
        backgroundColour = typedArray.getColor(R.styleable.Signor_BackgroundColour, resources.getColor(android.R.color.white));

        typedArray.recycle();

        matrix = new Matrix();
        detector = new GestureDetector(context, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        View parent = (View) getParent();
        int width = parent.getMeasuredWidth();
        int height = parent.getMeasuredHeight();

        if (width > height) {
            if (parentWidth < width) {
                parentWidth = width;
            }

            if (parentHeight < height) {
                parentHeight = height;
            }
        }

        width = (parentWidth - MARGIN) / 2;
        height = (parentWidth - MARGIN) / 2;

        this.setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        this.width = width;
        this.height = height;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int gradientY0 = -height * 2 / 3;
        int gradientY1 = Math.abs(gradientY0);
        int dimpleX0 = MARGIN / 2;
        int dimpleY0 = -dimpleX0;

        gradient = new LinearGradient(0, gradientY0, 0, gradientY1, backgroundColour, Color.GRAY, Shader.TileMode.CLAMP);
        dimple = new LinearGradient(dimpleX0, dimpleY0, dimpleX0, dimpleX0, Color.GRAY, backgroundColour, Shader.TileMode.CLAMP);
    }

    protected float getValue() {
        return value;
    }

    protected void setValue(float value) {
        this.value = value;

        if (listener != null) {
            listener.onKnobChange(this, this.value);
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(width / 2, height / 2);

        paint.setShader(gradient);
        paint.setStyle(Paint.Style.FILL);

        int radius = Math.min(width, height) / 2;
        canvas.drawCircle(0, 0, radius, paint);

        paint.setShader(null);
        paint.setColor(Color.LTGRAY);
        canvas.drawCircle(0, 0, radius - MARGIN, paint);

        float x = (float) (Math.sin(value * Math.PI / SCALE) * radius * 0.8);
        float y = (float) (-Math.cos(value * Math.PI / SCALE) * radius * 0.8);

        paint.setShader(dimple);
        matrix.setTranslate(x, y);
        dimple.setLocalMatrix(matrix);
        canvas.drawCircle(x, y, MARGIN, paint);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.previous:
                value -= 1.0;

                if (value < MIN) {
                    value = MIN;
                }
                break;
            case R.id.next:
                value += 1.0;

                if (value > MAX) {
                    value = MAX;
                }
                break;
            default:
                return;
        }

        value = Math.round(value);

        if (listener != null) {
            listener.onKnobChange(this, value);
        }

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (detector != null) {
            detector.onTouchEvent(event);
        }

        float x = event.getX() - width / 2;
        float y = event.getY() - height / 2;

        float theta = (float) Math.atan2(x, -y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:

                if (!move) {
                    move = true;
                } else {
                    float delta = theta - last;

                    if (delta > Math.PI) {
                        delta -= 2.0 * Math.PI;
                    }

                    if (delta < -Math.PI) {
                        delta += 2.0 * Math.PI;
                    }

                    value += delta * SCALE / Math.PI;

                    if (value < MIN) {
                        value = MIN;
                    }

                    if (value > MAX) {
                        value = MAX;
                    }

                    if (listener != null) {
                        listener.onKnobChange(this, value);
                    }

                    invalidate();
                }
                last = theta;
                break;

            case MotionEvent.ACTION_UP:
                move = false;
                break;
        }
        return true;
    }

    private float calculateNewFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        float x1 = event1.getX() - width / 2;
        float y1 = event1.getY() - height / 2;
        float x2 = event2.getX() - width / 2;
        float y2 = event2.getY() - height / 2;

        float theta1 = (float) Math.atan2(x1, -y1);
        float theta2 = (float) Math.atan2(x2, -y2);

        float delta = theta2 - theta1;
        float velocity = (float) Math.abs(Math.hypot(velocityX, velocityY));

        if (delta > Math.PI) {
            delta -= 2.0 * Math.PI;
        }

        if (delta < -Math.PI) {
            delta += 2.0 * Math.PI;
        }

        return value + Math.signum(delta) * velocity / VELOCITY;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        ValueAnimator animator = ValueAnimator.ofFloat(value, calculateNewFling(event1, event2, velocityX, velocityY));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(this);
        animator.start();

        return true;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        value = (Float) animation.getAnimatedValue();

        if (value < MIN) {
            animation.cancel();
            value = MIN;
        }

        if (value > MAX) {
            animation.cancel();
            value = MAX;
        }

        if (listener != null) {
            listener.onKnobChange(this, value);
        }

        invalidate();
    }

    protected void setOnKnobChangeListener(OnKnobChangeListener listener) {
        this.listener = listener;
    }

    // A collection of listener callback methods we don't need.
    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    public interface OnKnobChangeListener {
        void onKnobChange(Knob knob, float value);
    }
}
