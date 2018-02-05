package com.yarolegovich.discretescrollview.context;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Created by yarolegovich on 2/4/18.
 */

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.ViewHolder> {

    private List<TestData> data;
    private RecyclerView recyclerView;

    public TestAdapter(List<TestData> data) {
        this.data = data;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        float dp = parent.getResources().getDisplayMetrics().density;
        ImageView iv = new ImageView(parent.getContext());
        iv.setLayoutParams(new ViewGroup.LayoutParams((int) (180 * dp), (int) (256 * dp)));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new ViewHolder(iv);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TestData item = data.get(position);
        holder.image.setImageDrawable(item.image);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public List<TestData> getData() {
        return data;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public final ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recyclerView.smoothScrollToPosition(getAdapterPosition());
                }
            });
        }
    }
}
