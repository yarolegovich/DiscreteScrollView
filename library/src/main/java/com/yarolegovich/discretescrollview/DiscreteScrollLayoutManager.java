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

import java.util.Locale;

/**
 * Created by yarolegovich on 17.02.2017.
 */
class DiscreteScrollLayoutManager extends RecyclerView.LayoutManager {

    static final int NO_POSITION = -1;

    private static final String EXTRA_POSITION = "extra_position";
    private static final int DEFAULT_TIME_FOR_ITEM_SETTLE = 300;
    private static final int DEFAULT_FLING_THRESHOLD = 2100; //Decrease to increase sensitivity.
    private static final int DEFAULT_TRANSFORM_CLAMP_ITEM_COUNT = 1;

    protected static final float SCROLL_TO_SNAP_TO_ANOTHER_ITEM = 0.6f;

    //This field will take value of all visible view's center points during the fill phase
    protected Point viewCenterIterator;
    protected Point recyclerCenter;
    protected Point currentViewCenter;
    protected int childHalfWidth, childHalfHeight;
    protected int extraLayoutSpace;

    //Max possible distance a view can travel during one scroll phase
    protected int scrollToChangeCurrent;
    protected int currentScrollState;

    protected int scrolled;
    protected int pendingScroll;
    protected int currentPosition;
    protected int pendingPosition;

    protected SparseArray<View> detachedCache;

    private DSVOrientation.Helper orientationHelper;

    protected boolean isFirstOrEmptyLayout;

    private Context context;

    private int timeForItemSettle;
    private int offscreenItems;
    private int transformClampItemCount;

    private boolean dataSetChangeShiftedPosition;

    private int flingThreshold;
    private boolean shouldSlideOnFling;

    private int viewWidth, viewHeight;

    @NonNull
    private final ScrollStateListener scrollStateListener;
    private DiscreteScrollItemTransformer itemTransformer;

    private RecyclerViewProxy recyclerViewProxy;

    public DiscreteScrollLayoutManager(
            @NonNull Context c,
            @NonNull ScrollStateListener scrollStateListener,
            @NonNull DSVOrientation orientation) {
        this.context = c;
        this.timeForItemSettle = DEFAULT_TIME_FOR_ITEM_SETTLE;
        this.pendingPosition = NO_POSITION;
        this.currentPosition = NO_POSITION;
        this.flingThreshold = DEFAULT_FLING_THRESHOLD;
        this.shouldSlideOnFling = false;
        this.recyclerCenter = new Point();
        this.currentViewCenter = new Point();
        this.viewCenterIterator = new Point();
        this.detachedCache = new SparseArray<>();
        this.scrollStateListener = scrollStateListener;
        this.orientationHelper = orientation.createHelper();
        this.recyclerViewProxy = new RecyclerViewProxy(this);
        this.transformClampItemCount = DEFAULT_TRANSFORM_CLAMP_ITEM_COUNT;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            recyclerViewProxy.removeAndRecycleAllViews(recycler);
            currentPosition = pendingPosition = NO_POSITION;
            scrolled = pendingScroll = 0;
            return;
        }

        ensureValidPosition(state);

        updateRecyclerDimensions(state);

        //onLayoutChildren may be called multiple times and this check is required so that the flag
        //won't be cleared until onLayoutCompleted
        if (!isFirstOrEmptyLayout) {
            isFirstOrEmptyLayout = recyclerViewProxy.getChildCount() == 0;
            if (isFirstOrEmptyLayout) {
                initChildDimensions(recycler);
            }
        }

        recyclerViewProxy.detachAndScrapAttachedViews(recycler);

        fill(recycler);

        applyItemTransformToChildren();
    }

    private void ensureValidPosition(RecyclerView.State state) {
        if (currentPosition == NO_POSITION || currentPosition >= state.getItemCount()) {
            //currentPosition might have been assigned in onRestoreInstanceState()
            //which can lead to a crash (position out of bounds) when data set
            //is not persisted across rotations
            currentPosition = 0;
        }
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        if (isFirstOrEmptyLayout) {
            scrollStateListener.onCurrentViewFirstLayout();
            isFirstOrEmptyLayout = false;
        } else if (dataSetChangeShiftedPosition) {
            scrollStateListener.onDataSetChangeChangedPosition();
            dataSetChangeShiftedPosition = false;
        }
    }

    protected void initChildDimensions(RecyclerView.Recycler recycler) {
        View viewToMeasure = recyclerViewProxy.getMeasuredChildForAdapterPosition(0, recycler);

        int childViewWidth = recyclerViewProxy.getMeasuredWidthWithMargin(viewToMeasure);
        int childViewHeight = recyclerViewProxy.getMeasuredHeightWithMargin(viewToMeasure);

        childHalfWidth = childViewWidth / 2;
        childHalfHeight = childViewHeight / 2;

        scrollToChangeCurrent = orientationHelper.getDistanceToChangeCurrent(
                childViewWidth,
                childViewHeight);

        extraLayoutSpace = scrollToChangeCurrent * offscreenItems;

        recyclerViewProxy.detachAndScrapView(viewToMeasure, recycler);
    }

    protected void updateRecyclerDimensions(RecyclerView.State state) {
        boolean dimensionsChanged = !state.isMeasuring()
                && (recyclerViewProxy.getWidth()  != viewWidth
                ||  recyclerViewProxy.getHeight() != viewHeight);
        if (dimensionsChanged) {
            viewWidth = recyclerViewProxy.getWidth();
            viewHeight = recyclerViewProxy.getHeight();
            recyclerViewProxy.removeAllViews();
        }
        recyclerCenter.set(
                recyclerViewProxy.getWidth() / 2,
                recyclerViewProxy.getHeight() / 2);
    }

    protected void fill(RecyclerView.Recycler recycler) {
        cacheAndDetachAttachedViews();

        orientationHelper.setCurrentViewCenter(recyclerCenter, scrolled, currentViewCenter);

        final int endBound = orientationHelper.getViewEnd(
                recyclerViewProxy.getWidth(),
                recyclerViewProxy.getHeight());

        //Layout current
        if (isViewVisible(currentViewCenter, endBound)) {
            layoutView(recycler, currentPosition, currentViewCenter);
        }

        //Layout items before the current item
        layoutViews(recycler, Direction.START, endBound);

        //Layout items after the current item
        layoutViews(recycler, Direction.END, endBound);

        recycleDetachedViewsAndClearCache(recycler);
    }

    private void layoutViews(RecyclerView.Recycler recycler, Direction direction, int endBound) {
        final int positionStep = direction.applyTo(1);

        //Predictive layout is required when we are doing smooth fast scroll towards pendingPosition
        boolean noPredictiveLayoutRequired = pendingPosition == NO_POSITION
                || !direction.sameAs(pendingPosition - currentPosition);

        viewCenterIterator.set(currentViewCenter.x, currentViewCenter.y);
        for (int pos = currentPosition + positionStep; isInBounds(pos); pos += positionStep) {
            if (pos == pendingPosition) {
                noPredictiveLayoutRequired = true;
            }
            orientationHelper.shiftViewCenter(direction, scrollToChangeCurrent, viewCenterIterator);
            if (isViewVisible(viewCenterIterator, endBound)) {
                layoutView(recycler, pos, viewCenterIterator);
            } else if (noPredictiveLayoutRequired) {
                break;
            }
        }
    }

    protected void layoutView(RecyclerView.Recycler recycler, int position, Point viewCenter) {
        if (position < 0) return;
        View v = detachedCache.get(position);
        if (v == null) {
            v = recyclerViewProxy.getMeasuredChildForAdapterPosition(position, recycler);
            recyclerViewProxy.layoutDecoratedWithMargins(v,
                    viewCenter.x - childHalfWidth, viewCenter.y - childHalfHeight,
                    viewCenter.x + childHalfWidth, viewCenter.y + childHalfHeight);
        } else {
            recyclerViewProxy.attachView(v);
            detachedCache.remove(position);
        }
    }

    protected void cacheAndDetachAttachedViews() {
        detachedCache.clear();
        for (int i = 0; i < recyclerViewProxy.getChildCount(); i++) {
            View child = recyclerViewProxy.getChildAt(i);
            detachedCache.put(recyclerViewProxy.getPosition(child), child);
        }

        for (int i = 0; i < detachedCache.size(); i++) {
            recyclerViewProxy.detachView(detachedCache.valueAt(i));
        }
    }

    protected void recycleDetachedViewsAndClearCache(RecyclerView.Recycler recycler) {
        for (int i = 0; i < detachedCache.size(); i++) {
            View viewToRemove = detachedCache.valueAt(i);
            recyclerViewProxy.recycleView(viewToRemove, recycler);
        }
        detachedCache.clear();
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        int newPosition = currentPosition;
        if (currentPosition == NO_POSITION) {
            newPosition = 0;
        } else if (currentPosition >= positionStart) {
            newPosition = Math.min(currentPosition + itemCount, recyclerViewProxy.getItemCount() - 1);
        }
        onNewPosition(newPosition);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        int newPosition = currentPosition;
        if (recyclerViewProxy.getItemCount() == 0) {
            newPosition = NO_POSITION;
        } else if (currentPosition >= positionStart) {
            if (currentPosition < positionStart + itemCount) {
                //If currentPosition is in the removed items, then the new item became current
                currentPosition = NO_POSITION;
            }
            newPosition = Math.max(0, currentPosition - itemCount);
        }
        onNewPosition(newPosition);
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        //notifyDataSetChanged() was called. We need to ensure that currentPosition is not out of bounds
        currentPosition = Math.min(Math.max(0, currentPosition), recyclerViewProxy.getItemCount() - 1);
        dataSetChangeShiftedPosition = true;
    }

    private void onNewPosition(int position) {
        if (currentPosition != position) {
            currentPosition = position;
            dataSetChangeShiftedPosition = true;
        }
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scrollBy(dx, recycler);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scrollBy(dy, recycler);
    }

    protected int scrollBy(int amount, RecyclerView.Recycler recycler) {
        if (recyclerViewProxy.getChildCount() == 0) {
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

        orientationHelper.offsetChildren(-delta, recyclerViewProxy);

        if (orientationHelper.hasNewBecomeVisible(this)) {
            fill(recycler);
        }

        notifyScroll();

        applyItemTransformToChildren();

        return delta;
    }

    protected void applyItemTransformToChildren() {
        if (itemTransformer != null) {
            int clampAfterDistance = scrollToChangeCurrent * transformClampItemCount;
            for (int i = 0; i < recyclerViewProxy.getChildCount(); i++) {
                View child = recyclerViewProxy.getChildAt(i);
                float position = getCenterRelativePositionOf(child, clampAfterDistance);
                itemTransformer.transformItem(child, position);
            }
        }
    }

    @Override
    public void scrollToPosition(int position) {
        if (currentPosition == position) {
            return;
        }

        currentPosition = position;
        recyclerViewProxy.requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        if (currentPosition == position || pendingPosition != NO_POSITION) {
            return;
        }
        checkTargetPosition(state, position);
        if (currentPosition == NO_POSITION) {
            //Layout not happened yet
            currentPosition = position;
        } else {
            startSmoothPendingScroll(position);
        }
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
        int throttleValue = shouldSlideOnFling ? Math.abs(velocity / flingThreshold) : 1;
        int newPosition = currentPosition + Direction.fromDelta(velocity).applyTo(throttleValue);
        newPosition = checkNewOnFlingPositionIsInBounds(newPosition);
        boolean isInScrollDirection = velocity * scrolled >= 0;
        boolean canFling = isInScrollDirection && isInBounds(newPosition);
        if (canFling) {
            startSmoothPendingScroll(newPosition);
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

    protected int calculateAllowedScrollIn(Direction direction) {
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
        } else if (direction == Direction.END && currentPosition == recyclerViewProxy.getItemCount() - 1) {
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
        recyclerViewProxy.startSmoothScroll(scroller);
    }

    private void startSmoothPendingScroll(int position) {
        if (currentPosition == position) return;
        pendingScroll = -scrolled;
        Direction direction = Direction.fromDelta(position - currentPosition);
        int distanceToScroll = Math.abs(position - currentPosition) * scrollToChangeCurrent;
        pendingScroll += direction.applyTo(distanceToScroll);
        pendingPosition = position;
        startSmoothPendingScroll();
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return true;
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        return computeScrollRange(state);
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        return computeScrollOffset(state);
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return computeScrollExtent(state);
    }

    @Override
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        return computeScrollRange(state);
    }

    @Override
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        return computeScrollOffset(state);
    }

    @Override
    public int computeHorizontalScrollExtent(RecyclerView.State state) {
        return computeScrollExtent(state);
    }

    private int computeScrollOffset(RecyclerView.State state) {
        int scrollbarSize = computeScrollExtent(state);
        int offset = (int) ((scrolled / (float) scrollToChangeCurrent) * scrollbarSize);
        return (currentPosition * scrollbarSize) + offset;
    }

    private int computeScrollExtent(RecyclerView.State state) {
        if (getItemCount() == 0) {
            return 0;
        } else {
            return (int) (computeScrollRange(state) / (float) getItemCount());
        }
    }

    private int computeScrollRange(RecyclerView.State state) {
        if (getItemCount() == 0) {
            return 0;
        } else {
            return scrollToChangeCurrent * (getItemCount() - 1);
        }
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        pendingPosition = NO_POSITION;
        scrolled = pendingScroll = 0;
        if (newAdapter instanceof InitialPositionProvider) {
            currentPosition = ((InitialPositionProvider) newAdapter).getInitialPosition();
        } else {
            currentPosition = 0;
        }
        recyclerViewProxy.removeAllViews();
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

    public int getNextPosition() {
        if (scrolled == 0) {
            return currentPosition;
        } else if (pendingPosition != NO_POSITION) {
            return pendingPosition;
        } else {
            return currentPosition + Direction.fromDelta(scrolled).applyTo(1);
        }
    }

    public void setItemTransformer(DiscreteScrollItemTransformer itemTransformer) {
        this.itemTransformer = itemTransformer;
    }

    public void setTimeForItemSettle(int timeForItemSettle) {
        this.timeForItemSettle = timeForItemSettle;
    }

    public void setOffscreenItems(int offscreenItems) {
        this.offscreenItems = offscreenItems;
        extraLayoutSpace = scrollToChangeCurrent * offscreenItems;
        recyclerViewProxy.requestLayout();
    }

    public void setTransformClampItemCount(int transformClampItemCount) {
        this.transformClampItemCount = transformClampItemCount;
        applyItemTransformToChildren();
    }

    public void setOrientation(DSVOrientation orientation) {
        orientationHelper = orientation.createHelper();
        recyclerViewProxy.removeAllViews();
        recyclerViewProxy.requestLayout();
    }

    public void setShouldSlideOnFling(boolean result) {
        shouldSlideOnFling = result;
    }

    public void setSlideOnFlingThreshold(int threshold) {
        flingThreshold = threshold;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (recyclerViewProxy.getChildCount() > 0) {
            final AccessibilityRecordCompat record = AccessibilityEventCompat.asRecord(event);
            record.setFromIndex(getPosition(getFirstChild()));
            record.setToIndex(getPosition(getLastChild()));
        }
    }

    private float getCenterRelativePositionOf(View v, int maxDistance) {
        float distanceFromCenter = orientationHelper.getDistanceFromCenter(recyclerCenter,
                getDecoratedLeft(v) + childHalfWidth,
                getDecoratedTop(v) + childHalfHeight);
        return Math.min(Math.max(-1f, distanceFromCenter / maxDistance), 1f);
    }

    private int checkNewOnFlingPositionIsInBounds(int position) {
        final int itemCount = recyclerViewProxy.getItemCount();
        //The check is required in case slide through multiple items is turned on
        if (currentPosition != 0 && position < 0) {
            //If currentPosition == 0 && position < 0 we forbid scroll to the left,
            //but if currentPosition != 0 we can slide to the first item
            return 0;
        } else if (currentPosition != itemCount - 1 && position >= itemCount) {
            return itemCount - 1;
        }
        return position;
    }

    private int getHowMuchIsLeftToScroll(int dx) {
        return Direction.fromDelta(dx).applyTo(scrollToChangeCurrent - Math.abs(scrolled));
    }

    private boolean isAnotherItemCloserThanCurrent() {
        return Math.abs(scrolled) >= scrollToChangeCurrent * SCROLL_TO_SNAP_TO_ANOTHER_ITEM;
    }

    public View getFirstChild() {
        return recyclerViewProxy.getChildAt(0);
    }

    public View getLastChild() {
        return recyclerViewProxy.getChildAt(recyclerViewProxy.getChildCount() - 1);
    }

    public int getExtraLayoutSpace() {
        return extraLayoutSpace;
    }

    private void notifyScroll() {
        float amountToScroll = pendingPosition != NO_POSITION ?
                Math.abs(scrolled + pendingScroll) :
                scrollToChangeCurrent;
        float position = -Math.min(Math.max(-1f, scrolled / amountToScroll), 1f);
        scrollStateListener.onScroll(position);
    }

    private boolean isInBounds(int itemPosition) {
        return itemPosition >= 0 && itemPosition < recyclerViewProxy.getItemCount();
    }

    private boolean isViewVisible(Point viewCenter, int endBound) {
        return orientationHelper.isViewVisible(
                viewCenter, childHalfWidth, childHalfHeight,
                endBound, extraLayoutSpace);
    }

    private void checkTargetPosition(RecyclerView.State state, int targetPosition) {
        if (targetPosition < 0 || targetPosition >= state.getItemCount()) {
            throw new IllegalArgumentException(String.format(Locale.US,
                    "target position out of bounds: position=%d, itemCount=%d",
                    targetPosition, state.getItemCount()));
        }
    }

    protected void setRecyclerViewProxy(RecyclerViewProxy recyclerViewProxy) {
        this.recyclerViewProxy = recyclerViewProxy;
    }

    protected void setOrientationHelper(DSVOrientation.Helper orientationHelper) {
        this.orientationHelper = orientationHelper;
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

        void onDataSetChangeChangedPosition();
    }

    public interface InitialPositionProvider {
        int getInitialPosition();
    }
}
