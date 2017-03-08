package com.yarolegovich.discretescrollview;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcelable;
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

    private static final String EXTRA_POSITION = "extra_position";

    private static final int DIRECTION_START = -1;
    private static final int DIRECTION_END = 1;
    private static final int NO_POSITION = -1;
    private static final int DEFAULT_TIME_FOR_ITEM_SETTLE = 150;

    private int childViewWidth;
    private int childHalfWidth, childHalfHeight;
    private int recyclerCenterX, recyclerCenterY;

    private int currentScrollState;
    private int scrollToChangeTheCurrent;

    private int scrolled;
    private int pendingScroll;
    private int currentPosition;
    private int pendingPosition;

    private Context context;

    private int timeForItemSettle;

    private SparseArray<View> detachedCache;

    private DiscreteScrollItemTransformer itemTransformer;
    private ScrollStateListener scrollStateListener;

    public DiscreteScrollLayoutManager(Context c) {
        this.context = c;
        this.timeForItemSettle = DEFAULT_TIME_FOR_ITEM_SETTLE;
        this.pendingPosition = NO_POSITION;
        this.currentPosition = NO_POSITION;
        this.detachedCache = new SparseArray<>();
        setAutoMeasureEnabled(true);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            currentPosition = pendingPosition = NO_POSITION;
            scrolled = pendingScroll = 0;
            return;
        }

        boolean isFirstOrEmptyLayout = getChildCount() == 0;
        if (isFirstOrEmptyLayout) {
            initChildDimensions(recycler);
        }

        updateRecyclerDimensions();

        detachAndScrapAttachedViews(recycler);

        fill(recycler);

        applyItemTransformToChildren();

        if (isFirstOrEmptyLayout) {
            scrollStateListener.onCurrentViewFirstLayout();
        }

        scrollToChangeTheCurrent = childViewWidth;
    }

    private void initChildDimensions(RecyclerView.Recycler recycler) {
        View viewToMeasure = recycler.getViewForPosition(0);
        addView(viewToMeasure);
        measureChildWithMargins(viewToMeasure, 0, 0);

        childViewWidth = getDecoratedMeasuredWidth(viewToMeasure);
        childHalfWidth = childViewWidth / 2;
        childHalfHeight = getDecoratedMeasuredHeight(viewToMeasure) / 2;

        detachAndScrapView(viewToMeasure, recycler);
    }

    private void updateRecyclerDimensions() {
        recyclerCenterX = getWidth() / 2;
        recyclerCenterY = getHeight() / 2;
    }

    private void fill(RecyclerView.Recycler recycler) {
        cacheAndDetachAttachedViews();

        final int currentViewCenterX = recyclerCenterX - scrolled;
        final int childTop = recyclerCenterY - childHalfHeight;
        final int childBottom = recyclerCenterY + childHalfHeight;

        //Layout current
        layoutView(recycler, currentPosition,
                currentViewCenterX - childHalfWidth, childTop,
                currentViewCenterX + childHalfWidth, childBottom);

        int position;

        //Layout items to the left of the current item
        int viewRight = currentViewCenterX - childHalfWidth;
        position = currentPosition - 1;
        while (position >= 0 && viewRight > 0) {
            layoutView(recycler, position,
                    viewRight - childViewWidth, childTop,
                    viewRight, childBottom);
            viewRight -= childViewWidth;
            position--;
        }

        //Layout items to the right of the current item
        int viewLeft = currentViewCenterX + childHalfWidth;
        position = currentPosition + 1;
        while (position < getItemCount() && viewLeft < getWidth()) {
            layoutView(recycler, position,
                    viewLeft, childTop,
                    viewLeft + childViewWidth, childBottom);
            viewLeft += childViewWidth;
            position++;
        }

        removeAndRecycleCachedViews(recycler);
    }

    private void layoutView(
            RecyclerView.Recycler recycler,
            int position, int l, int t, int r, int b) {
        View v = detachedCache.get(position);
        if (v == null) {
            v = recycler.getViewForPosition(position);
            v.setTag(R.id.dsv_key_position, position);
            addView(v);
            measureChildWithMargins(v, 0, 0);
            layoutDecoratedWithMargins(v, l, t, r, b);
        } else {
            attachView(v);
            detachedCache.remove(position);
        }
    }

    private void cacheAndDetachAttachedViews() {
        detachedCache.clear();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int position = (int) child.getTag(R.id.dsv_key_position);
            detachedCache.put(position, child);
        }

        for (int i = 0; i < detachedCache.size(); i++) {
            detachView(detachedCache.valueAt(i));
        }
    }

    private void removeAndRecycleCachedViews(RecyclerView.Recycler recycler) {
        for (int i = 0; i < detachedCache.size(); i++) {
            recycler.recycleView(detachedCache.valueAt(i));
        }
        detachedCache.clear();
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        if (currentPosition == NO_POSITION) {
            currentPosition = 0;
        } else if (currentPosition >= positionStart) {
            currentPosition += itemCount;
        }
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        if (getItemCount() == 0) {
            currentPosition = NO_POSITION;
        } else if (currentPosition >= positionStart) {
            currentPosition = Math.max(0, currentPosition - itemCount);
        }
    }


    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        currentPosition = Math.min(Math.max(0, currentPosition), getItemCount() - 1);
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
                && ((Integer) firstView.getTag(R.id.dsv_key_position) > 0);
        boolean newViewFromRight = getDecoratedRight(lastView) < getWidth()
                && ((Integer) lastView.getTag(R.id.dsv_key_position) < getItemCount() - 1);

        if (newViewFromLeft || newViewFromRight) {
            fill(recycler);
        }

        if (scrollStateListener != null) {
            View current = findViewByPosition(currentPosition);
            if (current != null) {
                scrollStateListener.onScroll(getCenterRelativePositionOf(current));
            }
        }

        applyItemTransformToChildren();

        return delta;
    }

    private void applyItemTransformToChildren() {
        if (itemTransformer != null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                itemTransformer.transformItem(child, getCenterRelativePositionOf(child));
            }
        }
    }

    @Override
    public void scrollToPosition(int position) {
        if (currentPosition == position) {
            return;
        }

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
        if (currentScrollState == RecyclerView.SCROLL_STATE_IDLE && currentScrollState != state) {
            if (scrollStateListener != null) {
                scrollStateListener.onScrollStart();
            }
        }
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            //Scroll is not finished until current view is centered
            boolean isScrollStillInProgress = !onScrollEnd();
            if (isScrollStillInProgress) {
                return;
            }
        } else if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            onDragStart();
        }
        currentScrollState = state;
    }

    private boolean onScrollEnd() {
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

        if (isAnotherItemCloserThanCurrent()) {
            pendingScroll = getHowMuchIsLeftToScroll(scrolled);
        } else {
            pendingScroll = -scrolled;
        }

        if (pendingScroll == 0) {
            if (scrollStateListener != null) {
                scrollStateListener.onScrollEnd();
            }
            return true;
        } else {
            startSmoothPendingScroll();
            return false;
        }
    }

    private void onDragStart() {
        //Here we need to:
        //1. Stop any pending pending scroll
        //2. Set currentPosition to position of the item that is closest to the center
        boolean isScrollingThroughMultiplePositions = Math.abs(scrolled) > scrollToChangeTheCurrent;
        if (isScrollingThroughMultiplePositions) {
            int scrolledPositions = scrolled / scrollToChangeTheCurrent;
            currentPosition += scrolledPositions;
            scrolled -= scrolledPositions * scrollToChangeTheCurrent;
        }
        if (isAnotherItemCloserThanCurrent()) {
            int direction = dxToDirection(scrolled);
            currentPosition += direction;
            scrolled = -getHowMuchIsLeftToScroll(scrolled);
        }
        pendingPosition = NO_POSITION;
        pendingScroll = 0;
    }

    public void onFling(int velocity) {
        int direction = dxToDirection(velocity);
        int newPosition = currentPosition + direction;
        boolean canFling = newPosition >= 0 && newPosition < getItemCount();
        if (canFling) {
            pendingScroll = getHowMuchIsLeftToScroll(velocity);
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
        if (scrollStateListener != null) {
            scrollStateListener.onIsBoundReachedFlagChange(isBoundReached);
        }
        return allowedScroll;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        if (newAdapter.getItemCount() > 0) {
            pendingPosition = NO_POSITION;
            scrolled = pendingScroll = 0;
            currentPosition = 0;
        }
        removeAllViews();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        if (pendingPosition != NO_POSITION) {
            currentPosition = pendingPosition;
        }
        bundle.putInt(EXTRA_POSITION, currentPosition);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        currentPosition = bundle.getInt(EXTRA_POSITION);
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

    public void setScrollStateListener(ScrollStateListener boundReachedListener) {
        this.scrollStateListener = boundReachedListener;
    }

    public void setTimeForItemSettle(int timeForItemSettle) {
        this.timeForItemSettle = timeForItemSettle;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public View findViewByPosition(int position) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (((Integer) v.getTag(R.id.dsv_key_position)) == position) {
                return v;
            }
        }
        return null;
    }

    private int getHowMuchIsLeftToScroll(int dx) {
        return (scrollToChangeTheCurrent - Math.abs(scrolled)) * dxToDirection(dx);
    }

    private boolean isAnotherItemCloserThanCurrent() {
        return Math.abs(scrolled) >= scrollToChangeTheCurrent * 0.6f;
    }

    private float getCenterRelativePositionOf(View v) {
        int viewCenterX = (getDecoratedLeft(v) + getDecoratedRight(v)) / 2;
        int centerX = getWidth() / 2;
        float distanceFromCenter = viewCenterX - centerX;
        return Math.min(Math.max(-1f, distanceFromCenter / scrollToChangeTheCurrent), 1f);
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

    public interface ScrollStateListener {
        void onIsBoundReachedFlagChange(boolean isBoundReached);

        void onScrollStart();

        void onScrollEnd();

        void onScroll(float currentViewPosition);

        void onCurrentViewFirstLayout();
    }

    @IntDef({DIRECTION_START, DIRECTION_END})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Direction {
    }
}
