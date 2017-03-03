package com.yarolegovich.discretescrollview;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.yarolegovich.discretescrollview.transform.DiscreteScrollItemTransformer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by yarolegovich on 17.02.2017.
 */
class DiscreteScrollLayoutManager extends RecyclerView.LayoutManager {

    private static final int DIRECTION_START = -1;
    private static final int DIRECTION_END = 1;

    private static final int NO_POSITION = -1;

    private static final int DEFAULT_TIME_FOR_ITEM_SETTLE = 250;

    private int childViewWidth;
    private int childViewHeight;

    private int scrollToChangeTheCurrent;
    private int scrolled;
    private int pendingScroll;

    private int currentPosition;
    private int pendingPosition;

    private Context context;

    private int timeForItemSettle;

    private DiscreteScrollItemTransformer itemTransformer;
    private BoundReachedFlagListener boundReachedListener;

    public DiscreteScrollLayoutManager(Context c) {
        this.context = c;
        this.timeForItemSettle = DEFAULT_TIME_FOR_ITEM_SETTLE;
        this.pendingPosition = NO_POSITION;
        setAutoMeasureEnabled(true);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            removeAllViews();
            return;
        }

        boolean isFirstOrEmptyLayout = getChildCount() == 0;

        if (isFirstOrEmptyLayout) {
            initChildWidthAndHeight(recycler);
        }

        detachAndScrapAttachedViews(recycler);

        fill(recycler, state);

        applyItemTransformToChildren(state);

        scrollToChangeTheCurrent = childViewWidth;
    }

    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        final int currentViewCenterX = getWidth() / 2 - scrolled;
        final int viewHalfWidth = childViewWidth / 2;

        final int recyclerCenterY = getHeight() / 2;
        final int viewTop = recyclerCenterY - childViewHeight / 2;
        final int viewBottom = recyclerCenterY + childViewHeight / 2;

        SparseArray<View> detachedCache = new SparseArray<>(getChildCount());
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int position = (int) child.getTag(R.id.key_position);
            detachedCache.put(position, child);
        }

        for (int i = 0; i < detachedCache.size(); i++) {
            detachView(detachedCache.valueAt(i));
        }

        //Layout current item in the center
        layoutView(recycler, state, detachedCache, currentPosition,
                currentViewCenterX - viewHalfWidth, viewTop,
                currentViewCenterX + viewHalfWidth, viewBottom);

        int position;

        //Layout items to the left of the current item
        int viewRight = currentViewCenterX - viewHalfWidth;
        position = currentPosition - 1;
        while (position >= 0 && viewRight > 0) {
            layoutView(recycler, state,
                    detachedCache, position,
                    viewRight - childViewWidth, viewTop,
                    viewRight, viewBottom);
            viewRight -= childViewWidth;
            position--;
        }

        //Layout items to the right of the current item
        int viewLeft = currentViewCenterX + viewHalfWidth;
        position = currentPosition + 1;
        while (position < getItemCount() && viewLeft < getWidth()) {
            layoutView(recycler, state,
                    detachedCache, position,
                    viewLeft, viewTop,
                    viewLeft + childViewWidth, viewBottom);
            viewLeft += childViewWidth;
            position++;
        }

        for (int i = 0; i < detachedCache.size(); i++) {
            recycler.recycleView(detachedCache.valueAt(i));
        }
    }

    private View layoutView(
            RecyclerView.Recycler recycler, RecyclerView.State state,
            SparseArray<View> detachedCache,
            int position, int l, int t, int r, int b) {
        View v = detachedCache.get(position);
        if (v == null) {
            v = recycler.getViewForPosition(position);
            v.setTag(R.id.key_position, position);
            addView(v);
            measureChildWithMargins(v, 0, 0);
            layoutDecoratedWithMargins(v, l, t, r, b);
        } else {
            attachView(v);
            detachedCache.remove(position);
        }
        return v;
    }

    private void initChildWidthAndHeight(RecyclerView.Recycler recycler) {
        View viewToMeasure = recycler.getViewForPosition(0);
        addView(viewToMeasure);
        measureChildWithMargins(viewToMeasure, 0, 0);

        context = viewToMeasure.getContext();

        childViewWidth = getDecoratedMeasuredWidth(viewToMeasure);
        childViewHeight = getDecoratedMeasuredHeight(viewToMeasure);

        detachAndScrapView(viewToMeasure, recycler);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        int direction = dxToDirection(dx);
        int leftToScroll = calculateAllowedScrollIn(direction);
        if (leftToScroll <= 0) {
            return 0;
        }
        int delta = Math.min(leftToScroll, Math.abs(dx)) * direction;

        scrolled += delta;
        if (pendingScroll != 0) {
            pendingScroll -= delta;
        }

        offsetChildrenHorizontal(-delta);

        View firstView = getChildAt(0), lastView = getChildAt(getChildCount() - 1);
        boolean newViewFromLeft = getDecoratedLeft(firstView) > 0
                && ((Integer) firstView.getTag(R.id.key_position) > 0);
        boolean newViewFromRight = getDecoratedRight(lastView) < getWidth()
                && ((Integer) lastView.getTag(R.id.key_position) < getItemCount() - 1);

        if (newViewFromLeft || newViewFromRight) {
            fill(recycler, state);
        }

        applyItemTransformToChildren(state);

        Log.d("scroller", "scrollBy: " + dx + ", true delta: " + delta);

        return delta;
    }

    private void applyItemTransformToChildren(RecyclerView.State state) {
        if (itemTransformer != null) {
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                int viewCenterX = (getDecoratedLeft(v) + getDecoratedRight(v)) / 2;
                int centerX = getWidth() / 2;
                float distanceFromCenter = viewCenterX - centerX;
                float normalizedDistance = Math.min(Math.max(
                        -1f, distanceFromCenter / scrollToChangeTheCurrent), 1f);
                itemTransformer.transformItem(v, normalizedDistance);
            }
        }
    }

    @Override
    public void scrollToPosition(int position) {
        currentPosition = position;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        if (currentPosition == position) {
            return;
        }

        pendingScroll = -scrolled;
        int requiredDx = Math.abs(position - currentPosition)
                * dxToDirection(position - currentPosition)
                * scrollToChangeTheCurrent;
        pendingScroll += requiredDx;

        pendingPosition = position;
        startSmoothPendingScroll();
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }

    @Override
    public void onScrollStateChanged(int state) {
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            if (pendingPosition != NO_POSITION) {
                currentPosition = pendingPosition;
                pendingPosition = NO_POSITION;
                scrolled = 0;
            }

            int scrollDirection = dxToDirection(scrolled);
            if (Math.abs(scrolled) == scrollToChangeTheCurrent) {
                currentPosition += scrollDirection;
                scrolled = 0;
            }

            if (Math.abs(scrolled) >= (scrollToChangeTheCurrent * 0.6f)) {
                pendingScroll = (scrollToChangeTheCurrent - Math.abs(scrolled)) * scrollDirection;
            } else {
                pendingScroll = -scrolled;
            }

            if (pendingScroll != 0) {
                startSmoothPendingScroll();
            }
        } else if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            pendingScroll = 0;
        }
    }

    public void onFling(int velocity) {
        int direction = dxToDirection(velocity);
        int newPosition = currentPosition + direction;
        boolean canFling = newPosition >= 0 && newPosition < getItemCount();
        if (canFling) {
            pendingScroll = (scrollToChangeTheCurrent - Math.abs(scrolled)) * direction;
            if (pendingScroll != 0) {
                startSmoothPendingScroll();
            }
        } else {
            returnToCurrentPosition();
        }
    }

    public void returnToCurrentPosition() {
        pendingScroll = -scrolled;
        if (pendingScroll != 0) {
            startSmoothPendingScroll();
        }
    }

    private int calculateAllowedScrollIn(@Direction int direction) {
        if (pendingScroll != 0) {
            return Math.abs(pendingScroll);
        }
        int allowedScroll;
        boolean isBoundReached;
        boolean isScrollDirectionAsBefore = direction * scrolled > 0;
        if (direction == DIRECTION_START && currentPosition == 0) {
            isBoundReached = scrolled == 0;
            allowedScroll = isBoundReached ? 0 : Math.abs(scrolled);
        } else if (direction == DIRECTION_END && currentPosition == getItemCount() - 1) {
            isBoundReached = scrolled == 0;
            allowedScroll = isBoundReached ? 0 : Math.abs(scrolled);
        } else {
            isBoundReached = false;
            allowedScroll = isScrollDirectionAsBefore ?
                    scrollToChangeTheCurrent - Math.abs(scrolled) :
                    scrollToChangeTheCurrent + Math.abs(scrolled);
        }
        if (boundReachedListener != null) {
            boundReachedListener.onFlagChanged(isBoundReached);
        }
        return allowedScroll;
    }

    @Direction
    private int dxToDirection(int dx) {
        return dx > 0 ? DIRECTION_END : DIRECTION_START;
    }

    private void startSmoothPendingScroll() {
        LinearSmoothScroller scroller = new DiscreteLinearSmoothScroller(context);
        scroller.setTargetPosition(currentPosition);
        startSmoothScroll(scroller);
    }

    public void setItemTransformer(DiscreteScrollItemTransformer itemTransformer) {
        this.itemTransformer = itemTransformer;
    }

    public void setBoundReachedListener(BoundReachedFlagListener boundReachedListener) {
        this.boundReachedListener = boundReachedListener;
    }

    public void setTimeForItemSettle(int timeForItemSettle) {
        this.timeForItemSettle = timeForItemSettle;
    }

    private class DiscreteLinearSmoothScroller extends LinearSmoothScroller {

        public DiscreteLinearSmoothScroller(Context context) {
            super(context);
        }

        @Override
        public int calculateDxToMakeVisible(View view, int snapPreference) {
            return -pendingScroll;
        }

        @Override
        protected int calculateTimeForScrolling(int dx) {
            float dist = Math.min(Math.abs(dx), scrollToChangeTheCurrent);
            return (int) (Math.max(0.01f, dist / scrollToChangeTheCurrent) * timeForItemSettle);
        }

        @Nullable
        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            return new PointF(-pendingScroll, 0);
        }
    }

    public interface BoundReachedFlagListener {
        void onFlagChanged(boolean isBoundReached);
    }

    @IntDef({DIRECTION_START, DIRECTION_END})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Direction {
    }
}
