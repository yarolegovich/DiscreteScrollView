package com.yarolegovich.discretescrollview;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * Created by yarolegovich on 28-Apr-17.
 */

public class InfiniteScrollAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {

    private static final int NOT_INITIALIZED = -1;
    private static final int RESET_BOUND = 100;

    public static <T extends RecyclerView.ViewHolder> InfiniteScrollAdapter<T> wrap(
            @NonNull RecyclerView.Adapter<T> adapter) {
        return new InfiniteScrollAdapter<>(adapter);
    }

    private RecyclerView.Adapter<T> wrapped;
    private DiscreteScrollLayoutManager layoutManager;

    private int currentRangeStart;

    public InfiniteScrollAdapter(@NonNull RecyclerView.Adapter<T> wrapped) {
        this.wrapped = wrapped;
        this.wrapped.registerAdapterDataObserver(new DataSetChangeDelegate());
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        wrapped.onAttachedToRecyclerView(recyclerView);
        if (recyclerView instanceof DiscreteScrollView) {
            layoutManager = (DiscreteScrollLayoutManager) recyclerView.getLayoutManager();
            currentRangeStart = NOT_INITIALIZED;
        } else {
            String msg = recyclerView.getContext().getString(R.string.dsv_ex_msg_adapter_wrong_recycler);
            throw new RuntimeException(msg);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        wrapped.onDetachedFromRecyclerView(recyclerView);
        layoutManager = null;
    }

    @Override
    public T onCreateViewHolder(ViewGroup parent, int viewType) {
        if (currentRangeStart == NOT_INITIALIZED) {
            resetRange(0);
        }
        return wrapped.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(T holder, int position) {
        wrapped.onBindViewHolder(holder, mapPositionToReal(position));
    }

    @Override
    public int getItemViewType(int position) {
        return wrapped.getItemViewType(mapPositionToReal(position));
    }

    @Override
    public int getItemCount() {
        return wrapped.getItemCount() == 0 ? 0 : Integer.MAX_VALUE;
    }

    public int getRealItemCount() {
        return wrapped.getItemCount();
    }

    public int getRealCurrentPosition() {
        return getRealPosition(layoutManager.getCurrentPosition());
    }

    public int getRealPosition(int position) {
        return mapPositionToReal(position);
    }

    public int getClosestPosition(int position) {
        int adapterTarget = currentRangeStart + position;
        int adapterCurrent = layoutManager.getCurrentPosition();
        if (adapterTarget == adapterCurrent) {
            return adapterCurrent;
        } else if (adapterTarget < adapterCurrent) {
            int adapterTargetNextSet = currentRangeStart + wrapped.getItemCount() + position;
            return adapterCurrent - adapterTarget < adapterTargetNextSet - adapterCurrent ?
                    adapterTarget : adapterTargetNextSet;
        } else {
            int adapterTargetPrevSet = currentRangeStart - wrapped.getItemCount() + position;
            return adapterCurrent - adapterTargetPrevSet < adapterTarget - adapterCurrent ?
                    adapterTargetPrevSet : adapterTarget;
        }
    }

    private int mapPositionToReal(int position) {
        int newPosition = position - currentRangeStart;
        if (newPosition >= wrapped.getItemCount()) {
            currentRangeStart += wrapped.getItemCount();
            if (Integer.MAX_VALUE - currentRangeStart <= RESET_BOUND) {
                resetRange(0);
            }
            return 0;
        } else if (newPosition < 0) {
            currentRangeStart -= wrapped.getItemCount();
            if (currentRangeStart <= RESET_BOUND) {
                resetRange(wrapped.getItemCount() - 1);
            }
            return wrapped.getItemCount() - 1;
        } else {
            return newPosition;
        }
    }

    private void resetRange(int newPosition) {
        currentRangeStart = Integer.MAX_VALUE / 2;
        layoutManager.scrollToPosition(currentRangeStart + newPosition);
    }

    //TODO: handle proper data set change notifications
    private class DataSetChangeDelegate extends RecyclerView.AdapterDataObserver {

        @Override
        public void onChanged() {
            resetRange(0);
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            onChanged();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            onChanged();
        }
    }
}