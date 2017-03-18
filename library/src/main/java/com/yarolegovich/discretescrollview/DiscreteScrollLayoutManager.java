package com.yarolegovich.discretescrollview;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.yarolegovich.discretescrollview.transform.DiscreteScrollItemTransformer;

/**
 * Created by yarolegovich on 17.02.2017.
 */
class DiscreteScrollLayoutManager extends RecyclerView.LayoutManager {

    private static final String EXTRA_POSITION = "extra_position";

    private static final int NO_POSITION = -1;
    private static final int DEFAULT_TIME_FOR_ITEM_SETTLE = 150;

    //This field will take value of all visible view's center points during the fill phase
    private Point viewCenterIterator;
    private Point recyclerCenter;
    private Point currentViewCenter;
    private int childHalfWidth, childHalfHeight;

    //Max possible distance a view can travel during one scroll phase
    private int scrollToChangeCurrent;
    private int currentScrollState;

    private Orientation.Helper orientationHelper;

    private int scrolled;
    private int pendingScroll;
    private int currentPosition;
    private int pendingPosition;

    private Context context;

    private int timeForItemSettle;

    private SparseArray<View> detachedCache;

    @NonNull
    private final ScrollStateListener scrollStateListener;
    private DiscreteScrollItemTransformer itemTransformer;

    public DiscreteScrollLayoutManager(
            Context c,
            @NonNull ScrollStateListener scrollStateListener,
            @NonNull Orientation orientation) {
        this.context = c;
        this.timeForItemSettle = DEFAULT_TIME_FOR_ITEM_SETTLE;
        this.pendingPosition = NO_POSITION;
        this.currentPosition = NO_POSITION;
        this.recyclerCenter = new Point();
        this.currentViewCenter = new Point();
        this.viewCenterIterator = new Point();
        this.detachedCache = new SparseArray<>();
        this.scrollStateListener = scrollStateListener;
        this.orientationHelper = orientation.createHelper();
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
    }

    private void initChildDimensions(RecyclerView.Recycler recycler) {
        View viewToMeasure = recycler.getViewForPosition(0);
        addView(viewToMeasure);
        measureChildWithMargins(viewToMeasure, 0, 0);

        int childViewWidth = getDecoratedMeasuredWidth(viewToMeasure);
        int childViewHeight = getDecoratedMeasuredHeight(viewToMeasure);

        childHalfWidth = childViewWidth / 2;
        childHalfHeight = childViewHeight / 2;

        scrollToChangeCurrent = orientationHelper.getDistanceToChangeCurrent(
                childViewWidth,
                childViewHeight);

        detachAndScrapView(viewToMeasure, recycler);
    }

    private void updateRecyclerDimensions() {
        recyclerCenter.set(getWidth() / 2, getHeight() / 2);
    }

    private void fill(RecyclerView.Recycler recycler) {
        cacheAndDetachAttachedViews();

        orientationHelper.setCurrentViewCenter(recyclerCenter, scrolled, currentViewCenter);

        final int endBound = orientationHelper.getViewEnd(getWidth(), getHeight());

        //Layout current
        if (isViewVisible(currentViewCenter, endBound)) {
            layoutView(recycler, currentPosition, currentViewCenter);
        }

        //Layout items before the current item
        layoutViews(recycler, Direction.START, endBound);

        //Layout items after the current item
        layoutViews(recycler, Direction.END, endBound);

        recycleViewsAndClearCache(recycler);
    }

    private void layoutViews(RecyclerView.Recycler recycler, Direction direction, int endBound) {
        final int positionStep = direction.applyTo(1);

        viewCenterIterator.set(currentViewCenter.x, currentViewCenter.y);
        for (int i = currentPosition + positionStep; isInBounds(i); i += positionStep) {
            orientationHelper.shiftViewCenter(direction, scrollToChangeCurrent, viewCenterIterator);
            if (isViewVisible(viewCenterIterator, endBound)) {
                layoutView(recycler, i, viewCenterIterator);
            }
        }
    }

    private void layoutView(RecyclerView.Recycler recycler, int position, Point viewCenter) {
        View v = detachedCache.get(position);
        if (v == null) {
            v = recycler.getViewForPosition(position);
            addView(v);
            measureChildWithMargins(v, 0, 0);
            layoutDecoratedWithMargins(v,
                    viewCenter.x - childHalfWidth, viewCenter.y - childHalfHeight,
                    viewCenter.x + childHalfWidth, viewCenter.y + childHalfHeight);
        } else {
            attachView(v);
            detachedCache.remove(position);
        }
    }

    private void cacheAndDetachAttachedViews() {
        detachedCache.clear();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            detachedCache.put(getPosition(child), child);
        }

        for (int i = 0; i < detachedCache.size(); i++) {
            detachView(detachedCache.valueAt(i));
        }
    }

    private void recycleViewsAndClearCache(RecyclerView.Recycler recycler) {
        for (int i = 0; i < detachedCache.size(); i++) {
            View viewToRemove = detachedCache.valueAt(i);
            recycler.recycleView(viewToRemove);
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
        //notifyDataSetChanged() was called. We need to ensure that currentPosition is not out of bounds
        currentPosition = Math.min(Math.max(0, currentPosition), getItemCount() - 1);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scrollBy(dx, recycler);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scrollBy(dy, recycler);
    }

    private int scrollBy(int amount, RecyclerView.Recycler recycler) {
        if (getChildCount() == 0) {
            return 0;
        }

        Direction direction = Direction.fromDelta(amount);
        int leftToScroll = calculateAllowedScrollIn(direction);
        if (leftToScroll <= 0) {
            return 0;
        }

        int delta = direction.applyTo(Math.min(leftToScroll, Math.abs(amount)));
        scrolled += delta;
        if (pendingScroll != 0) {
            pendingScroll -= delta;
        }

        orientationHelper.offsetChildren(-delta, this);

        if (orientationHelper.hasNewBecomeVisible(this)) {
            fill(recycler);
        }

        notifyScroll();

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
        Direction direction = Direction.fromDelta(position - currentPosition);
        int distanceToScroll = Math.abs(position - currentPosition) * scrollToChangeCurrent;
        pendingScroll += direction.applyTo(distanceToScroll);

        pendingPosition = position;
        startSmoothPendingScroll();
    }

    @Override
    public boolean canScrollHorizontally() {
        return orientationHelper.canScrollHorizontally();
    }

    @Override
    public boolean canScrollVertically() {
        return orientationHelper.canScrollVertically();
    }

    @Override
    public void onScrollStateChanged(int state) {
        if (currentScrollState == RecyclerView.SCROLL_STATE_IDLE && currentScrollState != state) {
            scrollStateListener.onScrollStart();
        }

        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            //Scroll is not finished until current view is centered
            boolean isScrollEnded = onScrollEnd();
            if (isScrollEnded) {
                scrollStateListener.onScrollEnd();
            } else {
                //Scroll continues and we don't want to set currentScrollState to STATE_IDLE,
                //because this will then trigger .scrollStateListener.onScrollStart()
                return;
            }
        } else if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            onDragStart();
        }
        currentScrollState = state;
    }

    /**
     * @return true if scroll is ended and we don't need to settle items
     */
    private boolean onScrollEnd() {
        if (pendingPosition != NO_POSITION) {
            currentPosition = pendingPosition;
            pendingPosition = NO_POSITION;
            scrolled = 0;
        }

        Direction scrollDirection = Direction.fromDelta(scrolled);
        if (Math.abs(scrolled) == scrollToChangeCurrent) {
            currentPosition += scrollDirection.applyTo(1);
            scrolled = 0;
        }

        if (isAnotherItemCloserThanCurrent()) {
            pendingScroll = getHowMuchIsLeftToScroll(scrolled);
        } else {
            pendingScroll = -scrolled;
        }

        if (pendingScroll == 0) {
            return true;
        } else {
            startSmoothPendingScroll();
            return false;
        }
    }

    private void onDragStart() {
        //Here we need to:
        //1. Stop any pending scroll
        //2. Set currentPosition to position of the item that is closest to the center
        boolean isScrollingThroughMultiplePositions = Math.abs(scrolled) > scrollToChangeCurrent;
        if (isScrollingThroughMultiplePositions) {
            int scrolledPositions = scrolled / scrollToChangeCurrent;
            currentPosition += scrolledPositions;
            scrolled -= scrolledPositions * scrollToChangeCurrent;
        }
        if (isAnotherItemCloserThanCurrent()) {
            Direction direction = Direction.fromDelta(scrolled);
            currentPosition += direction.applyTo(1);
            scrolled = -getHowMuchIsLeftToScroll(scrolled);
        }
        pendingPosition = NO_POSITION;
        pendingScroll = 0;
    }

    public void onFling(int velocityX, int velocityY) {
        int velocity = orientationHelper.getFlingVelocity(velocityX, velocityY);
        int newPosition = currentPosition + Direction.fromDelta(velocity).applyTo(1);
        boolean isInScrollDirection = velocity * scrolled >= 0;
        boolean canFling = isInScrollDirection && newPosition >= 0 && newPosition < getItemCount();
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

    private int calculateAllowedScrollIn(Direction direction) {
        if (pendingScroll != 0) {
            return Math.abs(pendingScroll);
        }
        int allowedScroll;
        boolean isBoundReached;
        boolean isScrollDirectionAsBefore = direction.applyTo(scrolled) > 0;
        if (direction == Direction.START && currentPosition == 0) {
            //We can scroll to the left when currentPosition == 0 only if we scrolled to the right before
            isBoundReached = scrolled == 0;
            allowedScroll = isBoundReached ? 0 : Math.abs(scrolled);
        } else if (direction == Direction.END && currentPosition == getItemCount() - 1) {
            //We can scroll to the right when currentPosition == last only if we scrolled to the left before
            isBoundReached = scrolled == 0;
            allowedScroll = isBoundReached ? 0 : Math.abs(scrolled);
        } else {
            isBoundReached = false;
            allowedScroll = isScrollDirectionAsBefore ?
                    scrollToChangeCurrent - Math.abs(scrolled) :
                    scrollToChangeCurrent + Math.abs(scrolled);
        }
        scrollStateListener.onIsBoundReachedFlagChange(isBoundReached);
        return allowedScroll;
    }

    private void startSmoothPendingScroll() {
        LinearSmoothScroller scroller = new DiscreteLinearSmoothScroller(context);
        scroller.setTargetPosition(currentPosition);
        startSmoothScroll(scroller);
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

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void setItemTransformer(DiscreteScrollItemTransformer itemTransformer) {
        this.itemTransformer = itemTransformer;
    }

    public void setTimeForItemSettle(int timeForItemSettle) {
        this.timeForItemSettle = timeForItemSettle;
    }

    public void setOrientation(Orientation orientation) {
        orientationHelper = orientation.createHelper();
        removeAllViews();
        requestLayout();
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (getChildCount() > 0) {
            final AccessibilityRecordCompat record = AccessibilityEventCompat.asRecord(event);
            record.setFromIndex(getPosition(getFirstChild()));
            record.setToIndex(getPosition(getLastChild()));
        }
    }

    private float getCenterRelativePositionOf(View v) {
        float distanceFromCenter = orientationHelper.getDistanceFromCenter(recyclerCenter,
                getDecoratedLeft(v) + childHalfWidth,
                getDecoratedTop(v) + childHalfHeight);
        return Math.min(Math.max(-1f, distanceFromCenter / scrollToChangeCurrent), 1f);
    }

    private int getHowMuchIsLeftToScroll(int dx) {
        return Direction.fromDelta(dx).applyTo(scrollToChangeCurrent - Math.abs(scrolled));
    }

    private boolean isAnotherItemCloserThanCurrent() {
        return Math.abs(scrolled) >= scrollToChangeCurrent * 0.6f;
    }

    public View getFirstChild() {
        return getChildAt(0);
    }

    public View getLastChild() {
        return getChildAt(getChildCount() - 1);
    }

    private void notifyScroll() {
        float position = -Math.min(Math.max(-1f, scrolled / (float) scrollToChangeCurrent), 1f);
        scrollStateListener.onScroll(position);
    }

    private boolean isInBounds(int itemPosition) {
        return itemPosition >= 0 && itemPosition < getItemCount();
    }

    private boolean isViewVisible(Point viewCenter, int endBound) {
        return orientationHelper.isViewVisible(
                viewCenter, childHalfWidth, childHalfHeight,
                endBound);
    }

    private class DiscreteLinearSmoothScroller extends LinearSmoothScroller {

        public DiscreteLinearSmoothScroller(Context context) {
            super(context);
        }

        @Override
        public int calculateDxToMakeVisible(View view, int snapPreference) {
            return orientationHelper.getPendingDx(-pendingScroll);
        }

        @Override
        public int calculateDyToMakeVisible(View view, int snapPreference) {
            return orientationHelper.getPendingDy(-pendingScroll);
        }

        @Override
        protected int calculateTimeForScrolling(int dx) {
            float dist = Math.min(Math.abs(dx), scrollToChangeCurrent);
            return (int) (Math.max(0.01f, dist / scrollToChangeCurrent) * timeForItemSettle);
        }

        @Nullable
        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            return new PointF(
                    orientationHelper.getPendingDx(pendingScroll),
                    orientationHelper.getPendingDy(pendingScroll));
        }
    }

    public interface ScrollStateListener {
        void onIsBoundReachedFlagChange(boolean isBoundReached);

        void onScrollStart();

        void onScrollEnd();

        void onScroll(float currentViewPosition);

        void onCurrentViewFirstLayout();
    }

}
