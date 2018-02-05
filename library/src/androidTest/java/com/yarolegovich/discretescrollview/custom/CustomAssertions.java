package com.yarolegovich.discretescrollview.custom;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.yarolegovich.discretescrollview.DiscreteScrollView;

import static org.hamcrest.Matchers.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;


/**
 * Created by yarolegovich on 2/3/18.
 */

public class CustomAssertions {

    public static ViewAssertion currentPositionIs(final int expectedPosition) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewFoundException) {
                ensureViewFound(noViewFoundException);
                assertThat(view, isAssignableFrom(DiscreteScrollView.class));
                DiscreteScrollView dsv = (DiscreteScrollView) view;
                assertThat(dsv.getCurrentItem(), is(equalTo(expectedPosition)));

                View midChild = findCenteredChildIn(dsv);
                assertThat(midChild, is(notNullValue()));
                RecyclerView.ViewHolder holder = dsv.getChildViewHolder(midChild);
                assertThat(holder.getAdapterPosition(), is(equalTo(expectedPosition)));
            }
        };
    }

    public static ViewAssertion doesNotHaveChildren() {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewFoundException) {
                ensureViewFound(noViewFoundException);
                assertThat(view, isAssignableFrom(ViewGroup.class));
                ViewGroup viewGroup = (ViewGroup) view;
                assertThat(viewGroup.getChildCount(), is(equalTo(0)));
            }
        };
    }

    private static View findCenteredChildIn(DiscreteScrollView dsv) {
        final int centerX = dsv.getWidth() / 2;
        final int centerY = dsv.getHeight() / 2;
        for (int i = 0; i < dsv.getChildCount(); i++) {
            View child = dsv.getChildAt(i);
            if (centerX == (child.getLeft() + child.getWidth() / 2)
                && centerY == (child.getTop() + child.getHeight() / 2)) {
                return child;
            }
        }
        throw new AssertionError("can't find centered child");
    }

    private static boolean isMidpoint(int value, int rangeStart, int rangeEnd) {
        return value == (rangeStart + rangeEnd) / 2;
    }

    private static void ensureViewFound(NoMatchingViewException exception) {
        if (exception != null) {
            throw exception;
        }
    }
}
