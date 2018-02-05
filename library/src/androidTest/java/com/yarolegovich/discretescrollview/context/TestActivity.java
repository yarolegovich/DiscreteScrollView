package com.yarolegovich.discretescrollview.context;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.R;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Created by yarolegovich on 2/4/18.
 */

public class TestActivity extends AppCompatActivity implements DiscreteScrollView.ScrollStateChangeListener {

    private DiscreteScrollView scrollView;
    private TestAdapter adapter;

    private CountingIdlingResource expectedScrollEndCalls = new CountingIdlingResource(
            "scrollEndCalls" + hashCode(),
            true);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);

        super.onCreate(savedInstanceState);

        ViewGroup root = createRootView();
        scrollView = createScrollViewIn(root);

        setContentView(root);

        adapter = new TestAdapter(generateTestData(10));
        scrollView.setAdapter(adapter);
        scrollView.addScrollStateChangeListener(this);
    }

    public DiscreteScrollView getScrollView() {
        return scrollView;
    }

    public TestAdapter getAdapter() {
        return adapter;
    }

    private DiscreteScrollView createScrollViewIn(ViewGroup root) {
        DiscreteScrollView scrollView = new DiscreteScrollView(this);
        FrameLayout.LayoutParams scrollViewLp = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        scrollViewLp.gravity = Gravity.CENTER;
        scrollView.setLayoutParams(scrollViewLp);
        root.addView(scrollView);
        return scrollView;
    }

    private ViewGroup createRootView() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        return root;
    }

    public void incrementExpectedScrollEndCalls() {
        expectedScrollEndCalls.increment();
    }

    @Override
    public void onScrollStart(@NonNull RecyclerView.ViewHolder currentItemHolder, int adapterPosition) {
    }

    @Override
    public void onScrollEnd(@NonNull RecyclerView.ViewHolder currentItemHolder, int adapterPosition) {
        if (!expectedScrollEndCalls.isIdleNow()) {
            expectedScrollEndCalls.decrement();
        }
    }

    @Override
    public void onScroll(float scrollPosition, int currentPosition, int newPosition, @Nullable RecyclerView.ViewHolder currentHolder, @Nullable RecyclerView.ViewHolder newCurrent) {

    }

    public @NonNull List<IdlingResource> getIdlingResources() {
        return Collections.<IdlingResource>singletonList(expectedScrollEndCalls);
    }

    private List<TestData> generateTestData(int size) {
        List<TestData> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(new TestData());
        }
        return result;
    }
}
