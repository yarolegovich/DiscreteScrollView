package com.yarolegovich.discretescrollview;

import android.graphics.Point;
import android.support.v7.widget.RecyclerView;

/**
 * Created by yarolegovich on 16.03.2017.
 */
public enum Orientation {

    HORIZONTAL {
        @Override
        OrientationHelper createHelper() {
            return new HorizontalHelper();
        }
    },
    VERTICAL {
        @Override
        OrientationHelper createHelper() {
            return new VerticalHelper();
        }
    };

    //Package private
    abstract OrientationHelper createHelper();

    interface OrientationHelper {

        int getViewEnd(int recyclerWidth, int recyclerHeight);

        int getDistanceToChangeCurrent(int childWidth, int childHeight);

        void setCurrentViewCenter(Point recyclerCenter, int scrolled, Point outPoint);

        void shiftViewCenter(Direction direction, int shiftAmount, Point outCenter);

        int getFlingVelocity(int velocityX, int velocityY);

        int getPendingDx(int pendingScroll);

        int getPendingDy(int pendingScroll);

        void offsetChildren(int amount, RecyclerView.LayoutManager lm);

        float getDistanceFromCenter(Point center, int viewCenterX, int viewCenterY);

        boolean isViewVisible(Point center, int halfWidth, int halfHeight, int endBound);

        boolean canScrollVertically();

        boolean canScrollHorizontally();
    }

    protected static class HorizontalHelper implements OrientationHelper {

        @Override
        public int getViewEnd(int recyclerWidth, int recyclerHeight) {
            return recyclerWidth;
        }

        @Override
        public int getDistanceToChangeCurrent(int childWidth, int childHeight) {
            return childWidth;
        }

        @Override
        public void setCurrentViewCenter(Point recyclerCenter, int scrolled, Point outPoint) {
            outPoint.set(
                    recyclerCenter.x - scrolled,
                    recyclerCenter.y);
        }

        @Override
        public void shiftViewCenter(Direction direction, int shiftAmount, Point outCenter) {
            outCenter.set(
                    outCenter.x + direction.applyTo(shiftAmount),
                    outCenter.y);
        }

        @Override
        public boolean isViewVisible(Point viewCenter, int halfWidth, int halfHeight, int endBound) {
            int viewLeft = viewCenter.x - halfWidth;
            int viewRight = viewCenter.y + halfWidth;
            return viewLeft < endBound || viewRight > 0;
        }

        @Override
        public void offsetChildren(int amount, RecyclerView.LayoutManager lm) {
            lm.offsetChildrenHorizontal(amount);
        }

        @Override
        public float getDistanceFromCenter(Point center, int viewCenterX, int viewCenterY) {
            return viewCenterX - center.x;
        }

        @Override
        public int getFlingVelocity(int velocityX, int velocityY) {
            return velocityX;
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
        public int getPendingDx(int pendingScroll) {
            return pendingScroll;
        }

        @Override
        public int getPendingDy(int pendingScroll) {
            return 0;
        }
    }


    protected static class VerticalHelper implements OrientationHelper {

        @Override
        public int getViewEnd(int recyclerWidth, int recyclerHeight) {
            return recyclerHeight;
        }

        @Override
        public int getDistanceToChangeCurrent(int childWidth, int childHeight) {
            return childHeight;
        }

        @Override
        public void setCurrentViewCenter(Point recyclerCenter, int scrolled, Point outPoint) {
            outPoint.set(
                    recyclerCenter.x,
                    recyclerCenter.y - scrolled);
        }

        @Override
        public void shiftViewCenter(Direction direction, int shiftAmount, Point outCenter) {
            outCenter.set(
                    outCenter.x,
                    outCenter.y + direction.applyTo(shiftAmount));
        }


        @Override
        public void offsetChildren(int amount, RecyclerView.LayoutManager lm) {
            lm.offsetChildrenVertical(amount);
        }

        @Override
        public float getDistanceFromCenter(Point center, int viewCenterX, int viewCenterY) {
            return viewCenterY - center.y;
        }

        @Override
        public boolean isViewVisible(Point viewCenter, int halfWidth, int halfHeight, int endBound) {
            int viewTop = viewCenter.x - halfHeight;
            int viewBottom = viewCenter.y + halfHeight;
            return viewTop < endBound || viewBottom > 0;
        }

        @Override
        public int getFlingVelocity(int velocityX, int velocityY) {
            return velocityY;
        }

        @Override
        public boolean canScrollHorizontally() {
            return false;
        }

        @Override
        public boolean canScrollVertically() {
            return true;
        }

        @Override
        public int getPendingDx(int pendingScroll) {
            return 0;
        }

        @Override
        public int getPendingDy(int pendingScroll) {
            return pendingScroll;
        }
    }

    ;
}
