package com.yarolegovich.discretescrollview;

import android.content.Context;
import android.support.annotation.IntRange;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.yarolegovich.discretescrollview.transform.DiscreteScrollItemTransformer;

/**
 * Created by yarolegovich on 18.02.2017.
 */

public class DiscreteScrollView extends RecyclerView {

    private DiscreteScrollLayoutManager layoutManager;

    private ScrollStateChangeListener scrollStateChangeListener;
    private OnCurrentItemChangedListener currentItemChangeListener;

    public DiscreteScrollView(Context context) {
        super(context);
    }

    public DiscreteScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DiscreteScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        layoutManager = new DiscreteScrollLayoutManager(getContext());
        layoutManager.setScrollStateListener(new DiscreteScrollLayoutManager.ScrollStateListener() {
            @Override
            public void onIsBoundReachedFlagChange(boolean isBoundReached) {
                setOverScrollMode(isBoundReached ? OVER_SCROLL_ALWAYS : OVER_SCROLL_NEVER);
            }

            @Override
            public void onScrollStart() {
                if (scrollStateChangeListener != null) {
                    scrollStateChangeListener.onScrollStart();
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public void onScrollEnd() {
                if (scrollStateChangeListener != null) {
                    scrollStateChangeListener.onScrollEnd();
                }
                if (currentItemChangeListener != null) {
                    int current = layoutManager.getCurrentPosition();
                    ViewHolder holder = getChildViewHolder(layoutManager.findViewByPosition(current));
                    currentItemChangeListener.onItemChanged(holder, current);
                }
            }

            @Override
            public void onScroll(float currentViewPosition) {
                if (scrollStateChangeListener != null) {
                    scrollStateChangeListener.onScroll(currentViewPosition);
                }
            }
        });
        setLayoutManager(layoutManager);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        boolean isFling = super.fling(velocityX, velocityY);
        if (isFling) {
            layoutManager.onFling(velocityX);
        } else {
            layoutManager.returnToCurrentPosition();
        }
        return isFling;
    }


    public void setItemTransformer(DiscreteScrollItemTransformer transformer) {
        layoutManager.setItemTransformer(transformer);
    }

    public void setItemTransitionTimeMillis(@IntRange(from = 10) int millis) {
        layoutManager.setTimeForItemSettle(millis);
    }

    public void setScrollListener(ScrollStateChangeListener scrollStateChangeListener) {
        this.scrollStateChangeListener = scrollStateChangeListener;
    }

    public void setCurrentItemChangeListener(OnCurrentItemChangedListener<?> currentItemChangeListener) {
        this.currentItemChangeListener = currentItemChangeListener;
    }

    public interface ScrollStateChangeListener {
        void onScrollStart();

        void onScrollEnd();

        void onScroll(float position);
    }

    public interface OnCurrentItemChangedListener<T extends ViewHolder> {
        void onItemChanged(T viewHolder, int position);
    }

}
