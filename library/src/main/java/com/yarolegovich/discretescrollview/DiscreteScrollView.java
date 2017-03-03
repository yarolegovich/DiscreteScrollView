package com.yarolegovich.discretescrollview;

import android.content.Context;
import android.support.annotation.IntRange;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;

import com.yarolegovich.discretescrollview.transform.DiscreteScrollItemTransformer;

/**
 * Created by yarolegovich on 18.02.2017.
 */

public class DiscreteScrollView extends RecyclerView {

    private DiscreteScrollLayoutManager layoutManager;

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
        layoutManager.setBoundReachedListener(new DiscreteScrollLayoutManager.BoundReachedFlagListener() {
            @Override
            public void onFlagChanged(boolean isBoundReached) {
                Log.d("tag", "bound reached: " + isBoundReached);
                setOverScrollMode(isBoundReached ? OVER_SCROLL_ALWAYS : OVER_SCROLL_NEVER);
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

    public void setItemTransitionTimeMillis(@IntRange(from = 20) int millis) {

    }

}
