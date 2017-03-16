package com.yarolegovich.discretescrollview.transform;

import android.support.annotation.FloatRange;
import android.view.View;

/**
 * Created by yarolegovich on 03.03.2017.
 */

public class ScaleTransformer implements DiscreteScrollItemTransformer {

    private Pivot pivotX;
    private Pivot pivotY;
    private float minScale;
    private float maxScale;
    private float maxMinDiff;

    public ScaleTransformer() {
        pivotX = Pivot.X.CENTER.create();
        pivotY = Pivot.Y.CENTER.create();
        minScale = 0.8f;
        maxScale = 1f;
        maxMinDiff = maxScale - minScale;
    }

    @Override
    public void transformItem(View item, float position) {
        pivotX.setOn(item);
        pivotY.setOn(item);
        float closenessToCenter = 1f - Math.abs(position);
        float scale = minScale + maxMinDiff * closenessToCenter;
        item.setScaleX(scale);
        item.setScaleY(scale);
    }

    public static class Builder {

        private ScaleTransformer transformer;

        public Builder() {
            transformer = new ScaleTransformer();
        }

        public Builder setMinScale(@FloatRange(from = 0.01) float scale) {
            transformer.minScale = scale;
            return this;
        }

        public Builder setMaxScale(@FloatRange(from = 0.01) float scale) {
            transformer.maxMinDiff = scale;
            return this;
        }

        public Builder setPivotX(Pivot.X pivotX) {
            return setPivotX(pivotX.create());
        }

        public Builder setPivotX(Pivot pivot) {
            assertAxis(pivot, Pivot.AXIS_X);
            transformer.pivotX = pivot;
            return this;
        }

        public Builder setPivotY(Pivot.Y pivotY) {
            return setPivotY(pivotY.create());
        }

        public Builder setPivotY(Pivot pivot) {
            assertAxis(pivot, Pivot.AXIS_Y);
            transformer.pivotY = pivot;
            return this;
        }

        public ScaleTransformer build() {
            transformer.maxMinDiff = transformer.maxScale - transformer.minScale;
            return transformer;
        }

        private void assertAxis(Pivot pivot, @Pivot.Axis int axis) {
            if (pivot.getAxis() != axis) {
                throw new IllegalArgumentException("You passed a Pivot for wrong axis.");
            }
        }
    }
}
