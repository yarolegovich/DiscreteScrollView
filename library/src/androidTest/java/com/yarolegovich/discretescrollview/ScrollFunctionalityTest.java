package com.yarolegovich.discretescrollview;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static com.yarolegovich.discretescrollview.custom.CustomAssertions.currentPositionIs;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

/**
 * Created by yarolegovich on 2/5/18.
 */
@RunWith(AndroidJUnit4.class)
public class ScrollFunctionalityTest extends DiscreteScrollViewTest {

    @Test
    public void scrollToPosition_afterCurrent_changesPosition() {
        final int initialPosition = scrollView.getCurrentItem();
        assertThat(initialPosition, is(lessThan(adapter.getItemCount() - 1)));
        onUiThread(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollToPosition(initialPosition + 1);
            }
        });
        onScrollView().check(currentPositionIs(initialPosition + 1));
    }

    @Test
    public void scrollToPosition_beforeCurrent_changesPosition() {
        assertThat(adapter.getItemCount(), is(greaterThan(1)));
        final int initialPosition = adapter.getItemCount() / 2;
        ensurePositionIs(initialPosition);
        onUiThread(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollToPosition(initialPosition - 1);
            }
        });
        onScrollView().check(currentPositionIs(initialPosition - 1));
    }

    @Test
    public void smoothScrollToPosition_afterCurrent_changesPosition() {
        final int initialPosition = scrollView.getCurrentItem();
        assertThat(initialPosition, is(lessThan(adapter.getItemCount() - 1)));
        waitUntilScrollEnd();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                scrollView.setItemTransitionTimeMillis(10);
                scrollView.smoothScrollToPosition(initialPosition + 1);
            }
        });
        onScrollView().check(currentPositionIs(initialPosition + 1));
    }

    @Test
    public void smoothScrollToPosition_beforeCurrent_changesPosition() {
        assertThat(adapter.getItemCount(), is(greaterThan(1)));
        final int initialPosition = adapter.getItemCount() / 2;
        ensurePositionIs(initialPosition);
        waitUntilScrollEnd();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                scrollView.setItemTransitionTimeMillis(10);
                scrollView.smoothScrollToPosition(initialPosition - 1);
            }
        });
        onScrollView().check(currentPositionIs(initialPosition - 1));
    }

    @Test
    public void smoothScrollToPosition_throughSeveralPositionsAfterCurrent_changesPosition() {
        final int initialPosition = scrollView.getCurrentItem();
        final int targetPosition = adapter.getItemCount() - 1;
        assertThat(targetPosition - initialPosition, is(greaterThan(1)));
        waitUntilScrollEnd();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                scrollView.setItemTransitionTimeMillis(10);
                scrollView.smoothScrollToPosition(targetPosition);
            }
        });
        onScrollView().check(currentPositionIs(targetPosition));
    }

    @Test
    public void smoothScrollToPosition_throughSeveralPositionsBeforeCurrent_changesPosition() {
        final int initialPosition = adapter.getItemCount() - 1;
        final int targetPosition = 0;
        ensurePositionIs(initialPosition);
        assertThat(initialPosition, is(greaterThan(0)));
        waitUntilScrollEnd();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                scrollView.setItemTransitionTimeMillis(10);
                scrollView.smoothScrollToPosition(targetPosition);
            }
        });
        onScrollView().check(currentPositionIs(targetPosition));
    }
}
