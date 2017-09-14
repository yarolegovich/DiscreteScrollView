package com.yarolegovich.discretescrollview.sample.gallery;

import android.animation.ArgbEvaluator;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.sample.R;

import java.util.List;

public class GalleryActivity extends AppCompatActivity implements
        DiscreteScrollView.ScrollListener<GalleryAdapter.ViewHolder>,
        DiscreteScrollView.OnItemChangedListener<GalleryAdapter.ViewHolder>,
        View.OnClickListener {

    private ArgbEvaluator evaluator;
    private int currentOverlayColor;
    private int overlayColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        evaluator = new ArgbEvaluator();
        currentOverlayColor = ContextCompat.getColor(this, R.color.galleryCurrentItemOverlay);
        overlayColor = ContextCompat.getColor(this, R.color.galleryItemOverlay);

        Gallery gallery = Gallery.get();
        List<Image> data = gallery.getData();
        final DiscreteScrollView itemPicker = (DiscreteScrollView) findViewById(R.id.item_picker);
        itemPicker.setAdapter(new GalleryAdapter(data));
        itemPicker.addScrollListener(this);
        itemPicker.addOnItemChangedListener(this);
        itemPicker.scrollToPosition(1);

        findViewById(R.id.home).setOnClickListener(this);
        findViewById(R.id.fab_share).setOnClickListener(this);

        ehandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                itemPicker.setCurrentPosition(3);
            }
        }, 1000);
    }

    Handler ehandler = new Handler();

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.home:
                finish();
                break;
            case R.id.fab_share:
                share(v);
                break;
        }
    }

    @Override
    public void onScroll(
            float currentPosition,
            int currentIndex, int newIndex,
            @Nullable GalleryAdapter.ViewHolder currentHolder,
            @Nullable GalleryAdapter.ViewHolder newCurrent) {
        if (currentHolder != null && newCurrent != null) {
            float position = Math.abs(currentPosition);
            currentHolder.setOverlayColor(interpolate(position, currentOverlayColor, overlayColor));
            newCurrent.setOverlayColor(interpolate(position, overlayColor, currentOverlayColor));
        }
    }

    @Override
    public void onCurrentItemChanged(@Nullable GalleryAdapter.ViewHolder viewHolder, int adapterPosition) {
        //viewHolder will never be null, because we never remove items from adapter's list
        if (viewHolder != null) {
            viewHolder.setOverlayColor(currentOverlayColor);
        }
    }

    private void share(View view) {
        Snackbar.make(view, R.string.msg_unsupported_op, Snackbar.LENGTH_SHORT).show();
    }

    private int interpolate(float fraction, int c1, int c2) {
        return (int) evaluator.evaluate(fraction, c1, c2);
    }
}
