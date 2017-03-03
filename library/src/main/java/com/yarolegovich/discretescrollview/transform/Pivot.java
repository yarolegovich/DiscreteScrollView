package com.yarolegovich.discretescrollview.transform;

import android.support.annotation.IntDef;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by yarolegovich on 03.03.2017.
 */

public class Pivot {

    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;

    private static final int PIVOT_CENTER = -1;
    private static final int PIVOT_MAX = -2;

    private int axis;
    private int pivotPoint;

    public Pivot(@Axis int axis, int pivotPoint) {
        this.axis = axis;
        this.pivotPoint = pivotPoint;
    }

    public void setOn(View view) {
        if (axis == AXIS_X) {
            switch (pivotPoint) {
                case PIVOT_CENTER:
                    view.setPivotX(view.getWidth() * 0.5f);
                    break;
                case PIVOT_MAX:
                    view.setPivotX(view.getWidth());
                    break;
                default:
                    view.setPivotX(pivotPoint);
                    break;
            }
            return;
        }

        if (axis == AXIS_Y) {
            switch (pivotPoint) {
                case PIVOT_CENTER:
                    view.setPivotY(view.getHeight() * 0.5f);
                    break;
                case PIVOT_MAX:
                    view.setPivotY(view.getHeight());
                    break;
                default:
                    view.setPivotY(pivotPoint);
                    break;
            }
        }
    }

    @Axis
    public int getAxis() {
        return axis;
    }

    public enum X {
        LEFT {
            @Override
            public Pivot create() {
                return new Pivot(AXIS_X, 0);
            }
        },
        CENTER {
            @Override
            public Pivot create() {
                return new Pivot(AXIS_X, PIVOT_CENTER);
            }
        },
        RIGHT {
            @Override
            public Pivot create() {
                return new Pivot(AXIS_X, PIVOT_MAX);
            }
        };

        public abstract Pivot create();
    }

    public enum Y {
        TOP {
            @Override
            public Pivot create() {
                return new Pivot(AXIS_Y, 0);
            }
        },
        CENTER {
            @Override
            public Pivot create() {
                return new Pivot(AXIS_Y, PIVOT_CENTER);
            }
        },
        BOTTOM {
            @Override
            public Pivot create() {
                return new Pivot(AXIS_Y, PIVOT_MAX);
            }
        };

        public abstract Pivot create();
    }

    @IntDef({AXIS_X, AXIS_Y})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Axis{
    }
}

