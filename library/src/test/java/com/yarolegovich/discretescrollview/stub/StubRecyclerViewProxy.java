package com.yarolegovich.discretescrollview.stub;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.yarolegovich.discretescrollview.RecyclerViewProxy;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Created by yarolegovich on 10/28/17.
 */

public class StubRecyclerViewProxy extends RecyclerViewProxy {

    private int width, height;
    private int childWidth, childHeight;
    private List<StubChildInfo> children;
    private int adapterItemCount;

    public StubRecyclerViewProxy(@NonNull RecyclerView.LayoutManager layoutManager) {
        super(layoutManager);
        children = new ArrayList<>();
    }

    @Override
    public void removeAndRecycleAllViews(RecyclerView.Recycler recycler) {
        for (StubChildInfo childInfo : children) {
            recycleView(childInfo.view, recycler);
        }
        removeAllViews();
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public int getItemCount() {
        return adapterItemCount;
    }

    @Override
    public View getMeasuredChildForAdapterPosition(int position, RecyclerView.Recycler recycler) {
        if (position < adapterItemCount) {
            return new StubChildInfo(0, position).view;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public View getChildAt(int index) {
        return children.get(index).view;
    }

    @Override
    public int getPosition(View view) {
        for (StubChildInfo info : children) {
            if (info.view == view) return info.adapterPosition;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public int getMeasuredWidthWithMargin(View child) {
        return childWidth;
    }

    @Override
    public int getMeasuredHeightWithMargin(View child) {
        return childHeight;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void removeAllViews() {
        children.clear();
    }

    @Override
    public void offsetChildrenHorizontal(int amount) {
        //NOP
    }

    @Override
    public void offsetChildrenVertical(int amount) {
        //NOP
    }

    @Override
    public void attachView(View view) {
        //NOP
    }

    @Override
    public void detachView(View view) {
        //NOP
    }

    @Override
    public void detachAndScrapView(View view, RecyclerView.Recycler recycler) {
        //NOP
    }

    @Override
    public void detachAndScrapAttachedViews(RecyclerView.Recycler recycler) {
        //NOP
    }

    @Override
    public void recycleView(View view, RecyclerView.Recycler recycler) {
        //NOP
    }

    @Override
    public void layoutDecoratedWithMargins(View v, int left, int top, int right, int bottom) {
        //NOP
    }

    @Override
    public void requestLayout() {
        //NOP
    }

    @Override
    public void startSmoothScroll(RecyclerView.SmoothScroller smoothScroller) {
        //NOP
    }

    public void addChildren(int childCount, int firstChildAdapterPosition) {
        for (int i = 0; i < childCount; i++) {
            children.add(new StubChildInfo(i, firstChildAdapterPosition + i));
        }
    }

    public void setAdapterItemCount(int adapterItemCount) {
        this.adapterItemCount = adapterItemCount;
    }

    private static class StubChildInfo {
        public final View view;
        public final int recyclerChildIndex;
        public final int adapterPosition;

        private StubChildInfo(int recyclerChildIndex, int adapterPosition) {
            this.view = mock(View.class);
            this.recyclerChildIndex = recyclerChildIndex;
            this.adapterPosition = adapterPosition;
        }
    }

    public static class Builder {
        StubRecyclerViewProxy target;

        public Builder(RecyclerView.LayoutManager lm) {
            target = new StubRecyclerViewProxy(lm);
        }

        public Builder withAdapterItemCount(int count) {
            target.adapterItemCount = count;
            return this;
        }

        public Builder withRecyclerDimensions(int width, int height) {
            target.width = width;
            target.height = height;
            return this;
        }

        public Builder withChildDimensions(int width, int height) {
            target.childWidth = width;
            target.childHeight = height;
            return this;
        }

        public StubRecyclerViewProxy create() {
            return target;
        }
    }
}
