package com.task.vasskob.dragableframelayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;


public class DraggableFrameLayout extends FrameLayout {
    private static final String TAG = DraggableFrameLayout.class.getSimpleName();
    /**
     * Common durations
     * Large, complex, full-screen transitions may have longer durations, occurring over 375ms
     * Objects entering the screen occur over 225ms
     * Objects leaving the screen occur over 195ms
     */
    private static final int sMARGIN_ANIM_DURATION = 225;
    private static final int sUPPER_MARGIN_LIMIT = 0;
    private static final int sLIST_COUNT = 2;
    private static final String ORIENTATION_HORIZONTAL = "horizontal";
    private static final String ORIENTATION_WRONG_VALUE_WARN = "Orientation must be horizontal or vertical";
    public static final String ORIENTATION_VERTICAL = "vertical";
    private String viewOrientation;


    private boolean mTouch;
    private boolean mConsumeTouch;

    private int mLastEndMargin;

    private float mPrevRawX;
    private float mPrevRawY;

    private float mDistanceX;
    private float mDistanceY;

    private int mEndMarginLimit;

    private int mMarginLimits[];

    private boolean isDraggable = true;
    private ValueAnimator mMarginAnimator;

    private int mTouchSlop;
    private boolean horizontalOrientation;


    public DraggableFrameLayout(Context context) {
        super(context);
        viewOrientation = ORIENTATION_HORIZONTAL;
        init(context);
    }

    public DraggableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.DraggableFrameLayout, 0, 0);
        try {
            if (a.getString(R.styleable.DraggableFrameLayout_orientation) != null) {
                setViewOrientation(a.getString(R.styleable.DraggableFrameLayout_orientation));
            } else
                setViewOrientation(ORIENTATION_HORIZONTAL);
        } finally {
            a.recycle();
        }
        init(context);
    }

    public DraggableFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        /**mLowerBottomMarginLimit = (getHeight() / sLIST_COUNT) * 2;*/
        if (horizontalOrientation) {
            mEndMarginLimit = getWidth() / sLIST_COUNT;
        } else {
            mEndMarginLimit = getHeight() / sLIST_COUNT;
        }
        mEndMarginLimit *= -1;
        int mListWidth = getWidth() / sLIST_COUNT;
        mMarginLimits = new int[sLIST_COUNT];/* {sUPPER_MARGIN_LIMIT, mLowerBottomMarginLimit / 2, mLowerBottomMarginLimit};*/
        for (int i = sLIST_COUNT - 1; i >= 0; i--) {
            if (i == 0) {
                mMarginLimits[i] = sUPPER_MARGIN_LIMIT;
                continue;
            }
            mMarginLimits[i] = mEndMarginLimit / i;
        }
        Log.d(TAG, "onSizeChanged: mListHeight: " + mListWidth);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "onInterceptTouchEvent: ACTION_DOWN");
                mTouch = true;
                mPrevRawX = ev.getRawX();
                mPrevRawY = ev.getRawY();
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
                if (horizontalOrientation) {
                    mLastEndMargin = layoutParams.rightMargin;
                } else {
                    mLastEndMargin = layoutParams.topMargin;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onInterceptTouchEvent: ACTION_MOVE");
                if (mTouch) {
                    mConsumeTouch = handleTouch(ev);
                    return mConsumeTouch;
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onInterceptTouchEvent: ACTION_UP");
                mTouch = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onTouchEvent: ");
                if (horizontalOrientation) {
                    return dragContainer(layoutParams, event.getRawX());
                } else {
                    return dragContainer(layoutParams, event.getRawY());
                }

            case MotionEvent.ACTION_UP:
                mTouch = false;
                animateOnActionUp(layoutParams);
                break;
        }
        return false;
    }

    /**
     * if vertical scroll handle itself -> true
     * if horizontal scroll pass to children -> false
     *
     * @param ev motion event of touch
     * @return swipe orientation, vertical=1, horizontal=0
     */
    private boolean handleTouch(MotionEvent ev) {
        boolean result;
        float distanceX = Math.abs(ev.getRawX() - mPrevRawX);
        float distanceY = Math.abs(ev.getRawY() - mPrevRawY);

        result = (distanceY <= distanceX) && (mTouchSlop < distanceX);
        Log.d(TAG, "handleTouch: result: " + result + " mPrevTouchY: " + mPrevRawX);
        if (horizontalOrientation) {
            return result;
        } else {
            return !result;
        }

    }

    private boolean dragContainer(RelativeLayout.LayoutParams layoutParams, float lastTouchXY) {
        int newEndMargin;
        if (isDraggable) {
            mDistanceX = mPrevRawX - lastTouchXY;
            mDistanceY = -(mPrevRawY - lastTouchXY);
            if (!overSlopDistance()) {
                return mConsumeTouch;
            }
            Log.d(TAG, "onTouchEvent: mDistanceY: " + mDistanceX + " mPrevTouchY: " + mPrevRawY + " lastTouchY: " + lastTouchXY);
            if (horizontalOrientation) {
                newEndMargin = (int) (mLastEndMargin + mDistanceX);
            } else {
                newEndMargin = (int) (mLastEndMargin + mDistanceY);
            }
            if (newEndMargin >= mEndMarginLimit && newEndMargin <= sUPPER_MARGIN_LIMIT) {
                if (horizontalOrientation) {
                    layoutParams.rightMargin = newEndMargin;
                } else {
                    layoutParams.topMargin = newEndMargin;
                }
                requestLayout();
            } else {
                Log.d(TAG, "dragContainer: out of limit, should set minimum or maximum margin");
                if (newEndMargin > mEndMarginLimit) {
                    initMarginAnimator(layoutParams, sUPPER_MARGIN_LIMIT);
                } else {
                    initMarginAnimator(layoutParams, mEndMarginLimit);
                }
            }
        }
        return mConsumeTouch;
    }

    public void setDraggable(boolean draggable) {
        isDraggable = draggable;
    }

    public boolean initMarginAnimator(final RelativeLayout.LayoutParams layoutParams, int to) {
        if (horizontalOrientation) {
            mMarginAnimator = ValueAnimator.ofInt(layoutParams.rightMargin, to);
        } else {
            mMarginAnimator = ValueAnimator.ofInt(layoutParams.topMargin, to);
        }

        mMarginAnimator.setDuration(sMARGIN_ANIM_DURATION);
        mMarginAnimator.setInterpolator(new DecelerateInterpolator());
        mMarginAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (horizontalOrientation) {
                    layoutParams.rightMargin = (int) (Integer) valueAnimator.getAnimatedValue();
                } else {
                    layoutParams.topMargin = (int) (Integer) valueAnimator.getAnimatedValue();
                }
                requestLayout();
            }
        });
        mMarginAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                isDraggable = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isDraggable = true;
            }
        });
        mMarginAnimator.start();
        return false;
    }

    private void animateOnActionUp(RelativeLayout.LayoutParams layoutParams) {
        int marginToAnimate;
        if (isDraggable) {
            if (horizontalOrientation) {
                marginToAnimate = getClosestMargin(layoutParams.rightMargin);
            } else {
                marginToAnimate = getClosestMargin(layoutParams.topMargin);
            }
            initMarginAnimator(layoutParams, marginToAnimate);
        }
    }

    private int getClosestMargin(int currentMargin) {
        int firstDifference = Math.abs(mMarginLimits[0] - currentMargin);
        int idOfClosestMarginLimit = 0;
        for (int i = 1; i < mMarginLimits.length; i++) {
            int lastDifference = Math.abs(mMarginLimits[i] - currentMargin);
            if (lastDifference < firstDifference) {
                idOfClosestMarginLimit = i;
                firstDifference = lastDifference;
            }
        }
        return mMarginLimits[idOfClosestMarginLimit];
    }

    public void cancelMarginAnim() {
        if (mMarginAnimator != null) {
            mMarginAnimator.cancel();
        }
    }

    private boolean overSlopDistance() {
        if (horizontalOrientation)
            return mTouchSlop < Math.abs(mDistanceX);
        else return mTouchSlop < Math.abs(mDistanceY);
    }

    private void init(Context context) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        int mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        Log.d(TAG, "init: mTouchSlop: " + mTouchSlop + " mMinFling: " + mMinFlingVelocity);
        horizontalOrientation = viewOrientation.equals(ORIENTATION_HORIZONTAL);


    }

    public void setViewOrientation(String orientation) {

        if (orientation.equals(ORIENTATION_HORIZONTAL) || orientation.equals(ORIENTATION_VERTICAL)) {
            viewOrientation = orientation;
        } else {
            throw new IllegalArgumentException(ORIENTATION_WRONG_VALUE_WARN);

        }
    }


    public String getViewOrientation() {
        return viewOrientation;
    }
}