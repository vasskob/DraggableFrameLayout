package com.task.vasskob.dragableframelayout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;

/**
 * Created by bobyk on 12.12.16.
 */


public class IncreasingRecyclerView extends RecyclerView {

    private Context mContext;

    /**
     * Ambient light intensity
     */
    private static final int AMBIENT_LIGHT = 255;
    /**
     * Diffuse light intensity
     */
    private static final int DIFFUSE_LIGHT = 255;
    /**
     * Specular light intensity
     */
    private static final float SPECULAR_LIGHT = 150;
    /**
     * Shininess constant
     */
    private static final float SHININESS = 100;
    /**
     * The max intensity of the light
     */
    private int mMaxAlpha = 255;

    private int mMinAlpha = 80;

    private static final int MAX_INTENSITY = 0xFF;
    private static final String TAG = "IncreasingRecyclerView";

    private int mCurrentPosition = 0;

    private CoverFlowItemListener mCoverFlowItemListener;
    private LinearLayoutManager mLinearLayoutManager;

    private final Camera mCamera = new Camera();
    private final Matrix mMatrix = new Matrix();
    /**
     * Paint object to draw with
     */
    private final Paint mPaint = new Paint(Paint.LINEAR_TEXT_FLAG);
    private int mLastPosition;

    public interface CoverFlowItemListener {
        void onItemChanged(int position);

        void onItemSelected(int position);
    }

    public IncreasingRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public IncreasingRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public IncreasingRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mPaint.setAntiAlias(true);
        initOrientation();
        this.setChildrenDrawingOrderEnabled(true);
        this.addOnScrollListener(new CoverFlowScrollListener());
    }


    @Override
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        // TODO: 05/03/17 too much memory allocating, fix this
        Bitmap bitmap = getChildDrawingCache(child);
        // (top,left) is the pixel position of the child inside the list
        final int top = child.getTop();
        final int left = child.getLeft();

        // center point of child
        final int childCenterY = child.getHeight() / 2;
        final int childCenterX = child.getWidth() / 2;

        //center of list
        final int parentCenterX = getWidth() / 2;
        final int parentCenterY = getHeight() / 2;

        //center point of child relative to list
        final int absChildCenterX = child.getLeft() + childCenterX;
        final int absChildCenterY = top + childCenterY;
        //distance of child center to the list center

        final int distanceX = parentCenterX - absChildCenterX;
        final int distanceY = parentCenterY - absChildCenterY;



//        if (parentCenterX == absChildCenterX)
//            mPaint.setAlpha(mMaxAlpha);
//        else
//            mPaint.setAlpha(mMinAlpha);

//        if (parentCenterY == absChildCenterY) {
//            mPaint.setAlpha(mMaxAlpha);
//        } else {
//            mPaint.setAlpha(mMinAlpha);
//        }


        prepareMatrix(mMatrix, distanceY, getHeight() / 2);

        mMatrix.preTranslate(-childCenterX, -childCenterY);
        mMatrix.postTranslate(childCenterX, childCenterY);
        mMatrix.postTranslate(left, top);
        canvas.drawBitmap(bitmap, mMatrix, mPaint);
        return false;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        mCurrentPosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition() - 1;
        if (mLastPosition != mCurrentPosition) {
            mLastPosition = mCurrentPosition;
            mCoverFlowItemListener.onItemChanged(mCurrentPosition);
        }
        return i;
    }

    public void setCoverFlowItemListener(CoverFlowItemListener coverFlowItemListener) {
        this.mCoverFlowItemListener = coverFlowItemListener;
    }

    public class CoverFlowScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                Log.i(TAG, "onScrollStateChanged");
                mCoverFlowItemListener.onItemSelected(mCurrentPosition);
                Log.i(TAG, "mCurrentPosition:" + mCurrentPosition);
            }
        }
    }

    @Override
    public LinearLayoutManager getLayoutManager() {
        return mLinearLayoutManager;
    }

    public void scrollToCenter(int position) {
        int first_position = mLinearLayoutManager.findFirstVisibleItemPosition();
        mCurrentPosition = position - first_position;
        View targetChild = this.getChildAt(mCurrentPosition);
        int[] location = new int[2];
        targetChild.getLocationInWindow(location);
        final int targetItemX = location[0] + targetChild.getWidth() / 2;
        final int targetItemY = location[1] + targetChild.getHeight() / 2;


        Display display = getDisplay();
        final Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        final int centerX = width / 2;
        final int centerY = height / 2;


        //move on click
        IncreasingRecyclerView.this.smoothScrollBy(0, targetItemY - centerY);
    }

    private void initOrientation() {
        mLinearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        setLayoutManager(mLinearLayoutManager);
    }

    private void prepareMatrix(final Matrix outMatrix, int distanceY, int r) {
        //clip the distance
        final int d = Math.min(r, Math.abs(distanceY + distanceY / 2));
        //use circle formula
        final float translateZ = (float) Math.sqrt((r * r) - (d * d));

        mCamera.save();
        //makes last items disappear
        mPaint.setAlpha(Math.max(30, 255 - Math.abs(distanceY*2) * 200 / 400));
        mCamera.translate(0, 0, r - translateZ);
        mCamera.getMatrix(outMatrix);
        mCamera.restore();
    }

    private Bitmap getChildDrawingCache(final View child) {
        Bitmap bitmap = child.getDrawingCache();
        if (bitmap == null) {
            child.setDrawingCacheEnabled(true);
            child.buildDrawingCache();
            bitmap = child.getDrawingCache();
        }
        return bitmap;
    }

}
