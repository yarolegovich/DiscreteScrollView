package com.yarolegovich.discretescrollview.sample.weather;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.yarolegovich.discretescrollview.sample.R;

import java.util.List;

/**
 * Created by yarolegovich on 08.03.2017.
 */

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private List<Forecast> data;

    public ForecastAdapter(List<Forecast> data) {
        this.data = data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.item_city_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int iconTint = ContextCompat.getColor(holder.itemView.getContext(), R.color.grayIconTint);
        Forecast forecast = data.get(position);
        Glide.with(holder.itemView.getContext())
                .load(forecast.getCityIcon())
                .listener(new TintOnLoad(holder.imageView, iconTint))
                .into(holder.imageView);
        holder.textView.setText(forecast.getCityName());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.city_image);
            textView = (TextView) itemView.findViewById(R.id.city_name);
        }

        public void showText() {
            int parentHeight = ((View) imageView.getParent()).getHeight();
            float scale = (parentHeight - textView.getHeight()) / (float) imageView.getHeight();
            imageView.setPivotX(imageView.getWidth() * 0.5f);
            imageView.setPivotY(0);
            imageView.animate().scaleX(scale)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            textView.setVisibility(View.VISIBLE);
                            imageView.setColorFilter(Color.BLACK);
                        }
                    })
                    .scaleY(scale).setDuration(200)
                    .start();
        }

        public void hideText() {
            imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.grayIconTint));
            textView.setVisibility(View.INVISIBLE);
            imageView.animate().scaleX(1f).scaleY(1f)
                    .setDuration(200)
                    .start();
        }
    }

    private static class TintOnLoad implements RequestListener<Integer, GlideDrawable> {

        private ImageView imageView;
        private int tintColor;

        public TintOnLoad(ImageView view, int tintColor) {
            this.imageView = view;
            this.tintColor = tintColor;
        }

        @Override
        public boolean onException(Exception e, Integer model, Target<GlideDrawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(GlideDrawable resource, Integer model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
            imageView.setColorFilter(tintColor);
            return false;
        }
    }
}
