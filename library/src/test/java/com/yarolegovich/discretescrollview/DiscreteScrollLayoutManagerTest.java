package com.yarolegovich.discretescrollview;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.yarolegovich.discretescrollview.stub.StubRecyclerViewProxy;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

import static com.yarolegovich.discretescrollview.DiscreteScrollLayoutManager.NO_POSITION;
import static com.yarolegovich.discretescrollview.DiscreteScrollLayoutManager.SCROLL_TO_SNAP_TO_ANOTHER_ITEM;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Created by yarolegovich on 10/25/17.
 */
public abstract class DiscreteScrollLayoutManagerTest {

    private static final int RECYCLER_WIDTH = 400;
    private static final int RECYCLER_HEIGHT = 600;
    private static final int CHILD_WIDTH = 130;
    private static final int CHILD_HEIGHT = 600;
    private static final int ADAPTER_ITEM_COUNT = 10;

    private DiscreteScrollLayoutManager layoutManager;
    private DiscreteScrollLayoutManager.ScrollStateListener mockScrollStateListener;
    private StubRecyclerViewProxy stubRecyclerViewProxy;
    private DSVOrientation.Helper stubOrientationHelper;
    private RecyclerView.State stubState;

    @Before
    public void setUp() {
        stubState = mock(RecyclerView.State.class);
        stubOrientationHelper = spy(getOrientationToTest().createHelper());
        mockScrollStateListener = mock(DiscreteScrollLayoutManager.ScrollStateListener.class);

        layoutManager = spy(new DiscreteScrollLayoutManager(
                RuntimeEnvironment.application,
                mockScrollStateListener,
                getOrientationToTest()));

        stubRecyclerViewProxy = spy(new StubRecyclerViewProxy.Builder(layoutManager)
                .withRecyclerDimensions(RECYCLER_WIDTH, RECYCLER_HEIGHT)
                .withChildDimensions(CHILD_WIDTH, CHILD_HEIGHT)
                .withAdapterItemCount(ADAPTER_ITEM_COUNT)
                .create());

        layoutManager.setRecyclerViewProxy(stubRecyclerViewProxy);
        layoutManager.setOrientationHelper(stubOrientationHelper);
    }

    protected abstract DSVOrientation getOrientationToTest();

    @Test
    public void onLayoutChildren_noItems_removesViewsAndResetsState() {
        layoutManager.pendingScroll = 200;
        layoutManager.scrolled = 1000;
        layoutManager.pendingPosition = 1;
        layoutManager.currentPosition = 2;
        when(stubState.getItemCount()).thenReturn(0);

        layoutManager.onLayoutChildren(null, stubState);

        verify(stubRecyclerViewProxy).removeAndRecycleAllViews(nullable(RecyclerView.Recycler.class));
        assertThat(layoutManager.pendingScroll, is(0));
        assertThat(layoutManager.scrolled, is(0));
        assertThat(layoutManager.pendingPosition, is(NO_POSITION));
        assertThat(layoutManager.currentPosition, is(NO_POSITION));
    }

    @Test
    public void onLayoutChildren_whenFirstOrEmptyLayout_childDimensionsAreInitialized() {
        layoutManager.childHalfWidth = 0;
        layoutManager.childHalfHeight = 0;
        when(stubState.getItemCount()).thenReturn(ADAPTER_ITEM_COUNT);

        layoutManager.onLayoutChildren(null, stubState);

        assertThat(layoutManager.childHalfWidth, is(CHILD_WIDTH / 2));
        assertThat(layoutManager.childHalfHeight, is(CHILD_HEIGHT / 2));
    }

    @Test
    public void onLayoutChildren_notFirstOrEmptyLayout_childDimensionsAreNotInitialized() {
        stubRecyclerViewProxy.addChildren(5, 0);

        layoutManager.onLayoutChildren(null, stubState);

        assertThat(layoutManager.childHalfWidth, is(0));
        assertThat(layoutManager.childHalfHeight, is(0));
    }

    @Test
    public void onLayoutChildren_multipleCallsInLayoutPhase_isFirstOrEmptyLayoutFlagNotCleared() {
        when(stubState.getItemCount()).thenReturn(ADAPTER_ITEM_COUNT);

        layoutManager.onLayoutChildren(null, stubState);
        stubRecyclerViewProxy.addChildren(5, 0);
        layoutManager.onLayoutChildren(null, stubState);

        assertTrue(layoutManager.isFirstOrEmptyLayout);
    }

    @Test
    public void onLayoutCompleted_isFirstOrEmptyLayoutFlagSet_scrollStateListenerIsNotified() {
        layoutManager.isFirstOrEmptyLayout = true;

        layoutManager.onLayoutCompleted(stubState);

        verify(mockScrollStateListener).onCurrentViewFirstLayout();
    }

    @Test
    public void onLayoutCompleted_isFirstOrEmptyLayoutFlagSet_theFlagIsCleared() {
        layoutManager.isFirstOrEmptyLayout = true;

        layoutManager.onLayoutCompleted(stubState);

        assertFalse(layoutManager.isFirstOrEmptyLayout);
    }

    @Test
    public void initChildDimensions_offscreenItemsNotSet_noExtraLayoutSpace() {
        layoutManager.extraLayoutSpace = 1000;

        layoutManager.initChildDimensions(null);

        assertThat(layoutManager.extraLayoutSpace, is(0));
    }

    @Test
    public void initChildDimensions_offscreenItemsSet_extraLayoutSpaceIsCalculated() {
        layoutManager.extraLayoutSpace = 0;
        layoutManager.setOffscreenItems(5);

        layoutManager.initChildDimensions(null);

        assertThat(layoutManager.extraLayoutSpace, is(not(0)));
    }

    @Test
    public void updateRecyclerDimensions_recyclerCenterIsInitialized() {
        layoutManager.recyclerCenter.set(0, 0);

        layoutManager.updateRecyclerDimensions(stubState);

        assertThat(layoutManager.recyclerCenter.x, is(RECYCLER_WIDTH / 2));
        assertThat(layoutManager.recyclerCenter.y, is(RECYCLER_HEIGHT / 2));
    }

    @Test
    public void cacheAndDetachAttachedViews_allRecyclerChildrenArePutToCache() {
        final int childCount = 6;
        stubRecyclerViewProxy.addChildren(6, 0);

        layoutManager.cacheAndDetachAttachedViews();

        assertThat(layoutManager.detachedCache.size(), is(childCount));
        for (int i = 0; i < childCount; i++) {
            int position = stubRecyclerViewProxy.getPosition(stubRecyclerViewProxy.getChildAt(i));
            assertNotNull(layoutManager.detachedCache.get(position));
        }
    }

    @Test
    public void cacheAndDetachAttachedViews_allRecyclerChildrenAreDetached() {
        final int childCount = 5;
        stubRecyclerViewProxy.addChildren(childCount, 0);

        layoutManager.cacheAndDetachAttachedViews();

        for (int i = 0; i < childCount; i++) {
            verify(stubRecyclerViewProxy).detachView(stubRecyclerViewProxy.getChildAt(i));
        }
    }

    @Test
    public void recycleDetachedViewsAndClearCache_cacheIsClearedAndViewsAreRecycled() {
        List<View> views = Arrays.asList(mock(View.class), mock(View.class), mock(View.class));
        for (int i = 0; i < views.size(); i++) layoutManager.detachedCache.put(i, views.get(i));

        layoutManager.recycleDetachedViewsAndClearCache(null);

        assertThat(layoutManager.detachedCache.size(), is(0));
        verify(stubRecyclerViewProxy, times(views.size()))
                .recycleView(argThat(isIn(views)), nullable(RecyclerView.Recycler.class));
    }

    @Test
    public void onItemsAdded_afterCurrentPosition_currentPositionIsUnchanged() {
        final int addedItems = 3;
        final int initialCurrent = ADAPTER_ITEM_COUNT / 2;
        layoutManager.currentPosition = initialCurrent;
        stubRecyclerViewProxy.setAdapterItemCount(ADAPTER_ITEM_COUNT + addedItems);

        layoutManager.onItemsAdded(null, initialCurrent + 1, addedItems);

        assertThat(layoutManager.currentPosition, is(initialCurrent));
    }

    @Test
    public void onItemsAdded_beforeCurrentPosition_currentIsShiftedByAmountOfAddedItems() {
        final int addedItems = 3;
        final int initialCurrent = ADAPTER_ITEM_COUNT / 2;
        layoutManager.currentPosition = initialCurrent;
        stubRecyclerViewProxy.setAdapterItemCount(ADAPTER_ITEM_COUNT + addedItems);

        layoutManager.onItemsAdded(null, initialCurrent - 1, addedItems);

        assertThat(layoutManager.currentPosition, is(initialCurrent + addedItems));
    }

    @Test
    public void onItemsRemoved_afterCurrentPosition_currentIsUnchanged() {
        final int initialCurrent = ADAPTER_ITEM_COUNT / 2;
        final int removedItems = initialCurrent / 2;
        layoutManager.currentPosition = initialCurrent;
        stubRecyclerViewProxy.setAdapterItemCount(ADAPTER_ITEM_COUNT - removedItems);

        layoutManager.onItemsRemoved(null, initialCurrent + 1, removedItems);

        assertThat(layoutManager.currentPosition, is(initialCurrent));
    }

    @Test
    public void onItemsRemoved_beforeCurrentPosition_currentIsShiftedByAmountRemoved() {
        final int initialCurrent = ADAPTER_ITEM_COUNT / 2;
        final int removedItems = initialCurrent / 2;
        layoutManager.currentPosition = initialCurrent;
        stubRecyclerViewProxy.setAdapterItemCount(ADAPTER_ITEM_COUNT - removedItems);

        layoutManager.onItemsRemoved(null, 0, removedItems);

        assertThat(layoutManager.currentPosition, is(initialCurrent - removedItems));
    }

    @Test
    public void onItemsRemoved_rangeWhichContainsCurrent_currentIsReset() {
        final int initialCurrent = ADAPTER_ITEM_COUNT / 2;
        layoutManager.currentPosition = initialCurrent;

        layoutManager.onItemsRemoved(null, initialCurrent - 1, 3);

        assertThat(layoutManager.currentPosition, is(0));
    }

    @Test
    public void onItemsChanged_removedItemWhichWasCurrent_currentRemainsInValidRange() {
        layoutManager.currentPosition = ADAPTER_ITEM_COUNT - 1;
        stubRecyclerViewProxy.setAdapterItemCount(ADAPTER_ITEM_COUNT - 3);

        layoutManager.onItemsChanged(null);

        assertThat(layoutManager.currentPosition, is(stubRecyclerViewProxy.getItemCount() - 1));
    }

    @Test
    public void scrollBy_noChildren_noScrollPerformed() {
        doReturn(0).when(layoutManager).getChildCount();

        int scrolled = layoutManager.scrollBy(1000, null);

        assertThat(scrolled, is(0));
    }

    @Test
    public void scrollBy_moreScrollRequestedThanCanPerform_scrollsByAllAvailableAmount() {
        final int requested = 1000, maxAvailable = 333;
        prepareStubsForScrollBy(maxAvailable, 3, false);

        int scrolled = layoutManager.scrollBy(requested, null);

        assertThat(scrolled, both(not(equalTo(requested))).and(is(equalTo(maxAvailable))));
    }

    @Test
    public void scrollBy_offsetsChildrenByNegativeScrollDelta() {
        final int requested = 1000;
        prepareStubsForScrollBy(requested, 3, false);

        int scrolled = layoutManager.scrollBy(requested, null);

        verify(stubOrientationHelper).offsetChildren(-scrolled, stubRecyclerViewProxy);
    }

    @Test
    public void scrollBy_noNewViewBecameVisible_fillIsNotCalled() {
        prepareStubsForScrollBy(1000, 10, false);

        layoutManager.scrollBy(200, null);

        verify(layoutManager, never()).fill(any(RecyclerView.Recycler.class));
    }

    @Test
    public void scrollBy_newViewBecomesVisible_fillIsCalled() {
        final int childCount = 5;
        stubRecyclerViewProxy.addChildren(childCount, 0);
        prepareStubsForScrollBy(1000, childCount, true);

        layoutManager.scrollBy(200, null);

        verify(layoutManager).fill(nullable(RecyclerView.Recycler.class));
    }

    @Test
    public void scrollBy_hasPendingScroll_pendingScrollDecreasedByScrolledAmount() {
        final int initialPendingScroll = 1000;
        layoutManager.pendingScroll = initialPendingScroll;
        prepareStubsForScrollBy(300, 3, false);

        int scrolled = layoutManager.scrollBy(200, null);

        assertThat(layoutManager.pendingScroll, is(initialPendingScroll - scrolled));
    }

    @Test
    public void scrollBy_scrollIsAccumulated() {
        int initialScrolled = layoutManager.scrolled;
        prepareStubsForScrollBy(300, 3, false);

        int scrolled = layoutManager.scrollBy(100, null);

        assertThat(layoutManager.scrolled, is(initialScrolled + scrolled));
    }

    @Test
    public void scrollBy_scrolledMoreThanZero_listenerIsNotifiedAboutScroll() {
        prepareStubsForScrollBy(300, 3, false);

        int scrolled = layoutManager.scrollBy(200, null);

        assertThat(scrolled, is(greaterThan(0)));
        verify(mockScrollStateListener).onScroll(anyFloat());
    }

    @Test
    public void scrollBy_scrolledByZero_listenerIsNotNotifiedAboutScroll() {
        prepareStubsForScrollBy(0, 2, false);

        int scrolled = layoutManager.scrollBy(300, null);

        assertThat(scrolled, is(0));
        verify(mockScrollStateListener, never()).onScroll(anyFloat());
    }

    @Test
    public void scrollBy_triesToScrollToTheItemBeforeFirst_onBoundReachedIsTrue() {
        layoutManager.currentPosition = 0;
        stubRecyclerViewProxy.addChildren(5, 0);

        layoutManager.scrollBy(-100, null);

        verify(mockScrollStateListener).onIsBoundReachedFlagChange(true);
    }

    @Test
    public void scrollBy_triesToScrollToTheItemAfterLast_onBoundReachedIsTrue() {
        layoutManager.currentPosition = ADAPTER_ITEM_COUNT - 1;
        stubRecyclerViewProxy.addChildren(5, ADAPTER_ITEM_COUNT - 5);

        layoutManager.scrollBy(100, null);

        verify(mockScrollStateListener).onIsBoundReachedFlagChange(true);
    }

    @Test
    public void scrollBy_scrollsToAllowedElement_onBoundReachedIsFalse() {
        layoutManager.currentPosition = ADAPTER_ITEM_COUNT / 2;
        stubRecyclerViewProxy.addChildren(5, 0);

        layoutManager.scrollBy(100, null);

        verify(mockScrollStateListener).onIsBoundReachedFlagChange(false);
    }

    @Test
    public void onScrollStateChanged_dragStartedWhenWasIdle_listenerNotifiedAboutScrollStart() {
        layoutManager.currentScrollState = RecyclerView.SCROLL_STATE_IDLE;

        layoutManager.onScrollStateChanged(RecyclerView.SCROLL_STATE_DRAGGING);

        verify(mockScrollStateListener).onScrollStart();
    }

    @Test
    public void onScrollStateChanged_settlingStartedWhenWasIdle_listenerNotifiedAboutScrollStart() {
        layoutManager.currentScrollState = RecyclerView.SCROLL_STATE_IDLE;

        layoutManager.onScrollStateChanged(RecyclerView.SCROLL_STATE_SETTLING);

        verify(mockScrollStateListener).onScrollStart();
    }

    @Test
    public void onScrollStateChanged_newState_layoutManagerUpdatesItsState() {
        final int newState = RecyclerView.SCROLL_STATE_DRAGGING;
        layoutManager.currentScrollState = RecyclerView.SCROLL_STATE_IDLE;
        assertThat(layoutManager.currentScrollState, is(not(newState)));

        layoutManager.onScrollStateChanged(newState);

        assertThat(layoutManager.currentScrollState, is(newState));
    }

    @Test
    public void onScrollStateChanged_scrolledEnoughToChangeCurrent_listenerNotifiedAboutScrollEnd() {
        layoutManager.currentScrollState = RecyclerView.SCROLL_STATE_DRAGGING;
        layoutManager.scrolled = layoutManager.scrollToChangeCurrent;

        layoutManager.onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE);

        verify(mockScrollStateListener).onScrollEnd();
    }

    @Test
    public void onScrollStateChanged_scrolledNotEnoughToChangeCurrent_listenerNotNotifiedAboutScrollEnd() {
        layoutManager.currentScrollState = RecyclerView.SCROLL_STATE_DRAGGING;
        layoutManager.scrollToChangeCurrent = stubOrientationHelper.getDistanceToChangeCurrent(CHILD_WIDTH, CHILD_HEIGHT);
        layoutManager.scrolled = layoutManager.scrollToChangeCurrent / 2;

        layoutManager.onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE);

        verify(mockScrollStateListener, never()).onScrollEnd();
    }

    @Test
    public void onScrollStateChanged_draggedLessThanScrollToSnapToAnotherItem_settlesToCurrentPosition() {
        layoutManager.currentScrollState = RecyclerView.SCROLL_STATE_DRAGGING;
        layoutManager.currentPosition = ADAPTER_ITEM_COUNT / 2;
        layoutManager.scrollToChangeCurrent = stubOrientationHelper.getDistanceToChangeCurrent(CHILD_WIDTH, CHILD_HEIGHT);
        layoutManager.pendingScroll = 0;
        layoutManager.scrolled = (int) (layoutManager.scrollToChangeCurrent * (SCROLL_TO_SNAP_TO_ANOTHER_ITEM - 0.01f));

        layoutManager.onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE);

        assertThat(layoutManager.pendingScroll, is(-layoutManager.scrolled));
        verify(stubRecyclerViewProxy).startSmoothScroll(any(RecyclerView.SmoothScroller.class));
    }

    @Test
    public void onScrollStateChanged_draggedMoreThanOrScrollToSnapToAnotherItem_settlesToClosestItem() {
        layoutManager.currentScrollState = RecyclerView.SCROLL_STATE_DRAGGING;
        layoutManager.currentPosition = ADAPTER_ITEM_COUNT / 2;
        layoutManager.scrollToChangeCurrent = stubOrientationHelper.getDistanceToChangeCurrent(CHILD_WIDTH, CHILD_HEIGHT);
        layoutManager.pendingScroll = 0;
        layoutManager.scrolled = (int) (layoutManager.scrollToChangeCurrent * SCROLL_TO_SNAP_TO_ANOTHER_ITEM);
        int scrollLeftToAnotherItem = layoutManager.scrollToChangeCurrent - layoutManager.scrolled;

        layoutManager.onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE);

        assertThat(layoutManager.pendingScroll, is(scrollLeftToAnotherItem));
        verify(stubRecyclerViewProxy).startSmoothScroll(any(RecyclerView.SmoothScroller.class));
    }

    @Test
    public void onScrollStateChanged_whenSettlingDragIsStarted_settlingStops() {
        layoutManager.currentScrollState = RecyclerView.SCROLL_STATE_SETTLING;
        layoutManager.pendingScroll = 1000;

        layoutManager.onScrollStateChanged(RecyclerView.SCROLL_STATE_DRAGGING);

        assertThat(layoutManager.pendingScroll, is(0));
    }

    @Test
    public void onScrollStateChanged_whenSettlingDragIsStarted_closestPositionBecomesCurrent() {
        final int initialPosition = 5;
        layoutManager.currentScrollState = RecyclerView.SCROLL_STATE_SETTLING;
        layoutManager.scrollToChangeCurrent = stubOrientationHelper.getDistanceToChangeCurrent(CHILD_WIDTH, CHILD_HEIGHT);
        final int scrolled = (int) (layoutManager.scrollToChangeCurrent * SCROLL_TO_SNAP_TO_ANOTHER_ITEM);
        layoutManager.scrolled = scrolled;
        layoutManager.currentPosition = initialPosition;

        layoutManager.onScrollStateChanged(RecyclerView.SCROLL_STATE_DRAGGING);

        assertThat(layoutManager.currentPosition, is(initialPosition + 1));
        assertThat(layoutManager.scrolled, is(scrolled - layoutManager.scrollToChangeCurrent));
    }

    @Test
    public void onFling_velocitiesWithDifferentSignsOnDifferentAxis_correctFlingDirection() {
        final int velocityX = 100, velocityY = -100;
        final int velocityToUse = stubOrientationHelper.getFlingVelocity(velocityX, velocityY);
        Direction direction = Direction.fromDelta(velocityToUse);
        layoutManager.currentPosition = ADAPTER_ITEM_COUNT / 2;
        layoutManager.scrollToChangeCurrent = stubOrientationHelper.getDistanceToChangeCurrent(CHILD_WIDTH, CHILD_HEIGHT);
        layoutManager.pendingScroll = 0;

        layoutManager.onFling(velocityX, velocityY);

        assertThat(layoutManager.pendingScroll, is(direction.applyTo(layoutManager.scrollToChangeCurrent)));
    }

    @Test
    public void onFling_toTheOppositeToScrollDirection_returnsToPosition() {
        layoutManager.scrollToChangeCurrent = stubOrientationHelper.getDistanceToChangeCurrent(CHILD_WIDTH, CHILD_HEIGHT);
        int scrolled = layoutManager.scrollToChangeCurrent / 2;
        layoutManager.pendingScroll = 0;
        layoutManager.scrolled = scrolled;

        layoutManager.onFling(-scrolled, -scrolled);

        assertThat(layoutManager.pendingScroll, is(-scrolled));
    }

    @Test
    public void onFling_toTheSameDirectionAsScrolled_changesPosition() {
        layoutManager.scrollToChangeCurrent = stubOrientationHelper.getDistanceToChangeCurrent(CHILD_WIDTH, CHILD_HEIGHT);
        int scrolled = layoutManager.scrollToChangeCurrent / 3;
        int leftToScroll = layoutManager.scrollToChangeCurrent - scrolled;
        layoutManager.pendingScroll = 0;
        layoutManager.scrolled = scrolled;

        layoutManager.onFling(scrolled, scrolled);

        assertThat(layoutManager.pendingScroll, is(leftToScroll));
    }

    @Test
    public void onFling_toItemBeforeTheFirst_isImpossible() {
        layoutManager.currentPosition = 0;

        layoutManager.onFling(-100, -100);

        assertThat(layoutManager.pendingScroll, is(0));
    }

    @Test
    public void onFling_toItemAfterTheLast_isImpossible() {
        layoutManager.currentPosition = ADAPTER_ITEM_COUNT - 1;

        layoutManager.onFling(100, 100);

        assertThat(layoutManager.pendingScroll, is(0));
    }

    private void prepareStubsForScrollBy(int allowedScroll, int childCount, boolean hasNewBecomeVisible) {
        doReturn(allowedScroll).when(layoutManager).calculateAllowedScrollIn(any(Direction.class));
        stubRecyclerViewProxy.addChildren(childCount, 0);
        doReturn(hasNewBecomeVisible).when(stubOrientationHelper).hasNewBecomeVisible(any(DiscreteScrollLayoutManager.class));
    }

}
