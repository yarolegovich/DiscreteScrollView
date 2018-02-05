package com.yarolegovich.discretescrollview;

import android.graphics.Point;
import android.view.View;

/**
 * Created by yarolegovich on 16.03.2017.
 */
public enum DSVOrientation {

    HORIZONTAL {
        @Override
        Helper createHelper() {
            return new HorizontalHelper();
        }
    },
    VERTICAL {
        @Override
        Helper createHelper() {
            return new VerticalHelper();
        }
    };

    //Package private
    abstract Helper createHelper();

    interface Helper {

        int getViewEnd(int recyclerWidth, int recyclerHeight);

        int getDistanceToChangeCurrent(int childWidth, int childHeight);

        void setCurrentViewCenter(Point recyclerCenter, int scrolled, Point outPoint);

        void shiftViewCenter(Direction direction, int shiftAmount, Point outCenter);

        int getFlingVelocity(int velocityX, int velocityY);

        int getPendingDx(int pendingScroll);

        int getPendingDy(int pendingScroll);

        void offsetChildren(int amount, RecyclerViewProxy lm);

        float getDistanceFromCenter(Point center, int viewCenterX, int viewCenterY);

        boolean isViewVisible(Point center, int halfWidth, int halfHeight, int endBound, int extraSpace);

        boolean hasNewBecomeVisible(DiscreteScrollLayoutManager lm);

        boolean canScrollVertically();

        boolean canScrollHorizontally();
    }

    protected static class HorizontalHelper implements Helper {

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
            int newX = recyclerCenter.x - scrolled;
            outPoint.set(newX, recyclerCenter.y);
        }

        @Override
        public void shiftViewCenter(Direction direction, int shiftAmount, Point outCenter) {
            int newX = outCenter.x + direction.applyTo(shiftAmount);
            outCenter.set(newX, outCenter.y);
        }

        @Override
        public boolean isViewVisible(
                Point viewCenter, int halfWidth, int halfHeight, int endBound,
                int extraSpace) {
            int viewLeft = viewCenter.x - halfWidth;
            int viewRight = viewCenter.x + halfWidth;
            return viewLeft < (endBound + extraSpace) && viewRight > -extraSpace;
        }

        @Override
        public boolean hasNewBecomeVisible(DiscreteScrollLayoutManager lm) {
            View firstChild = lm.getFirstChild(), lastChild = lm.getLastChild();
            int leftBound = -lm.getExtraLayoutSpace();
            int rightBound = lm.getWidth() + lm.getExtraLayoutSpace();
            boolean isNewVisibleFromLeft = lm.getDecoratedLeft(firstChild) > leftBound
                    && lm.getPosition(firstChild) > 0;
            boolean isNewVisibleFromRight = lm.getDecoratedRight(lastChild) < rightBound
                    && lm.getPosition(lastChild) < lm.getItemCount() - 1;
            return isNewVisibleFromLeft || isNewVisibleFromRight;
        }

        @Override
        public void offsetChildren(int amount, RecyclerViewProxy helper) {
            helper.offsetChildrenHorizontal(amount);
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


    protected static class VerticalHelper implements Helper {

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
            int newY = recyclerCenter.y - scrolled;
            outPoint.set(recyclerCenter.x, newY);
        }

        @Override
        public void shiftViewCenter(Direction direction, int shiftAmount, Point outCenter) {
            int newY = outCenter.y + direction.applyTo(shiftAmount);
            outCenter.set(outCenter.x, newY);
        }

        @Override
        public void offsetChildren(int amount, RecyclerViewProxy helper) {
            helper.offsetChildrenVertical(amount);
        }

        @Override
        public float getDistanceFromCenter(Point center, int viewCenterX, int viewCenterY) {
            return viewCenterY - center.y;
        }

        @Override
        public boolean isViewVisible(
                Point viewCenter, int halfWidth, int halfHeight, int endBound,
                int extraSpace) {
            int viewTop = viewCenter.y - halfHeight;
            int viewBottom = viewCenter.y + halfHeight;
            return viewTop < (endBound + extraSpace) && viewBottom > -extraSpace;
        }

        @Override
        public boolean hasNewBecomeVisible(DiscreteScrollLayoutManager lm) {
            View firstChild = lm.getFirstChild(), lastChild = lm.getLastChild();
            int topBound = -lm.getExtraLayoutSpace();
            int bottomBound = lm.getHeight() + lm.getExtraLayoutSpace();
            boolean isNewVisibleFromTop = lm.getDecoratedTop(firstChild) > topBound
                    && lm.getPosition(firstChild) > 0;
            boolean isNewVisibleFromBottom = lm.getDecoratedBottom(lastChild) < bottomBound
                    && lm.getPosition(lastChild) < lm.getItemCount() - 1;
            return isNewVisibleFromTop || isNewVisibleFromBottom;
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

}
