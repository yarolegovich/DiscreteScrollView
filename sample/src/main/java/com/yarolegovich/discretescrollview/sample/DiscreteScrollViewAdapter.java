package com.yarolegovich.discretescrollview.sample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yarolegovich.discretescrollview.R;

import java.util.Locale;

/**
 * Created by yarolegovich on 17.02.2017.
 */

public class DiscreteScrollViewAdapter extends RecyclerView.Adapter<DiscreteScrollViewAdapter.ViewHolder> {

    private int layout;

    public DiscreteScrollViewAdapter(int layout) {
        this.layout = layout;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(layout,
                        parent,
                        false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.text.setText(String.format(Locale.US, "Card #%d", position));
    }

    @Override
    public int getItemCount() {
        return 15;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView text;

        public ViewHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.text);
        }
    }
}
