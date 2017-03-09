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

/**
 * Created by User on 06.03.2017.
 */

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
    private static final String DEFAULT_ORIENTATION = "horizontal";
    private String viewOrientation;
    private Context mContext;

    private boolean mIsScrolling;
    private boolean mTouch;
    private boolean mConsumeTouch;

    private int mLastEndMargin;

    private float mPrevRawX;
    private float mPrevRawY;

    private float mDistanceX;

    private int mEndMarginLimit;

    private int mListWidth;
    private int mMarginLimits[];

    private boolean isDraggable = true;
    private ValueAnimator mMarginAnimator;

    private int mTouchSlop;
    private int mMinFlingVelocity;


    public DraggableFrameLayout(Context context) {
        super(context);
        viewOrientation = DEFAULT_ORIENTATION;
        init(context);
    }

    public DraggableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.DraggableFrameLayout, 0, 0);
        try {
            viewOrientation = a.getString(R.styleable.DraggableFrameLayout_orientation);
            init(context);
        } finally {
            a.recycle();
        }
    }

    public DraggableFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        /**mLowerBottomMarginLimit = (getHeight() / sLIST_COUNT) * 2;*/
        mEndMarginLimit = getWidth() / sLIST_COUNT;
        mEndMarginLimit *= -1;

        mListWidth = getWidth() / sLIST_COUNT;

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
                mLastEndMargin = layoutParams.rightMargin;
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
                return dragContainer(layoutParams, event.getRawX());
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
     * @param ev
     * @return
     */
    private boolean handleTouch(MotionEvent ev) {
        boolean result;
        float distanceX = Math.abs(ev.getRawX() - mPrevRawX);
        float distanceY = Math.abs(ev.getRawY() - mPrevRawY);

        result = (distanceY <= distanceX) && (mTouchSlop < distanceX);
        Log.d(TAG, "handleTouch: result: " + result + " mPrevTouchY: " + mPrevRawX);
        return result;
    }

    private boolean dragContainer(RelativeLayout.LayoutParams layoutParams, float lastTouchX) {
        if (isDraggable) {
            mDistanceX = mPrevRawX - lastTouchX;
            if (!overSlopDistance()) {
                return mConsumeTouch;
            }
            Log.d(TAG, "onTouchEvent: mDistanceY: " + mDistanceX + " mPrevTouchY: " + mPrevRawY + " lastTouchY: " + lastTouchX);
            int newEndMargin = (int) (mLastEndMargin + mDistanceX);
            if (newEndMargin >= mEndMarginLimit && newEndMargin <= sUPPER_MARGIN_LIMIT) {
                layoutParams.rightMargin = newEndMargin;
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
        mMarginAnimator = ValueAnimator.ofInt(layoutParams.rightMargin, to);
        mMarginAnimator.setDuration(sMARGIN_ANIM_DURATION);
        mMarginAnimator.setInterpolator(new DecelerateInterpolator());
        mMarginAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                layoutParams.rightMargin = (int) (Integer) valueAnimator.getAnimatedValue();
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
        if (isDraggable) {
            int marginToAnimate = getClosestMargin(layoutParams.rightMargin);
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
        return mTouchSlop < Math.abs(mDistanceX);
    }

    private void init(Context context) {
        mContext = context;
        ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        Log.d(TAG, "init: mTouchSlop: " + mTouchSlop + " mMinFling: " + mMinFlingVelocity);


    }

    public void setViewOrientation(String orientation) {
        viewOrientation = orientation;
    }

    public String getViewOrientation() {
        return viewOrientation;
    }
}