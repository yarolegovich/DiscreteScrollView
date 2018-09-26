package com.yarolegovich.discretescrollview.custom;

import android.view.View;
import android.view.ViewGroup;

import com.yarolegovich.discretescrollview.DiscreteScrollView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


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
