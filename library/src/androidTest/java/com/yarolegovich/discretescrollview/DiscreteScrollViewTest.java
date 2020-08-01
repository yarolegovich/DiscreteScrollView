package com.yarolegovich.discretescrollview;

import android.view.View;

import androidx.annotation.CallSuper;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.ViewInteraction;
import androidx.test.rule.ActivityTestRule;

import com.yarolegovich.discretescrollview.context.TestActivity;
import com.yarolegovich.discretescrollview.context.TestAdapter;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import java.util.List;

import static com.yarolegovich.discretescrollview.custom.CustomAssertions.currentPositionIs;

/**
 * Created by yarolegovich on 2/3/18.
 */

public abstract class DiscreteScrollViewTest {

    private IdlingResource[] idlingResources;

    protected DiscreteScrollView scrollView;
    protected TestAdapter adapter;

    @Rule
    public ActivityTestRule<TestActivity> testActivity = new ActivityTestRule<>(TestActivity.class);

    @Before
    @CallSuper
    public void setUp() {
        TestActivity activity = testActivity.getActivity();
        scrollView = activity.getScrollView();
        adapter = testActivity.getActivity().getAdapter();

        List<IdlingResource> resources = activity.getIdlingResources();
        idlingResources = resources.toArray(new IdlingResource[resources.size()]);
        IdlingRegistry.getInstance().register(idlingResources);
    }

    @After
    @CallSuper
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResources);
    }

    protected ViewInteraction onScrollView() {
        return Espresso.onView(Matchers.<View>is(scrollView));
    }

    protected void waitUntilScrollEnd() {
        testActivity.getActivity().incrementExpectedScrollEndCalls();
    }

    protected void ensurePositionIs(final int position) {
        onUiThread(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollToPosition(position);
            }
        });
        onScrollView().check(currentPositionIs(position));
    }

    protected void onUiThread(Runnable runnable) {
        try {
            testActivity.runOnUiThread(runnable);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

}
