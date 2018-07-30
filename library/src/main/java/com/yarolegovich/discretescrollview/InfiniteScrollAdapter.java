package com.yarolegovich.discretescrollview;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.Locale;

/**
 * Created by yarolegovich on 28-Apr-17.
 */

public class InfiniteScrollAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T>
        implements DiscreteScrollLayoutManager.InitialPositionProvider {

    private static final int CENTER = Integer.MAX_VALUE / 2;
    private static final int RESET_BOUND = 100;

    public static <T extends RecyclerView.ViewHolder> InfiniteScrollAdapter<T> wrap(
            @NonNull RecyclerView.Adapter<T> adapter) {
        return new InfiniteScrollAdapter<>(adapter);
    }

    private RecyclerView.Adapter<T> wrapped;
    private DiscreteScrollLayoutManager layoutManager;

    public InfiniteScrollAdapter(@NonNull RecyclerView.Adapter<T> wrapped) {
        this.wrapped = wrapped;
        this.wrapped.registerAdapterDataObserver(new DataSetChangeDelegate());
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        wrapped.onAttachedToRecyclerView(recyclerView);
        if (recyclerView instanceof DiscreteScrollView) {
            layoutManager = (DiscreteScrollLayoutManager) recyclerView.getLayoutManager();
        } else {
            String msg = recyclerView.getContext().getString(R.string.dsv_ex_msg_adapter_wrong_recycler);
            throw new RuntimeException(msg);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        wrapped.onDetachedFromRecyclerView(recyclerView);
        layoutManager = null;
    }

    @Override
    public @NonNull T onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return wrapped.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull T holder, int position) {
        if (isResetRequired(position)) {
            int resetPosition = CENTER + mapPositionToReal(layoutManager.getCurrentPosition());
            setPosition(resetPosition);
            return;
        }
        wrapped.onBindViewHolder(holder, mapPositionToReal(position));
    }

    @Override
    public int getItemViewType(int position) {
        return wrapped.getItemViewType(mapPositionToReal(position));
    }

    @Override
    public int getItemCount() {
        return isInfinite() ? Integer.MAX_VALUE : wrapped.getItemCount();
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
        ensureValidPosition(position);
        int adapterCurrent = layoutManager.getCurrentPosition();
        int current = mapPositionToReal(adapterCurrent);
        if (position == current) {
            return adapterCurrent;
        }
        int delta = position - current;
        int target = adapterCurrent + delta;
        int wraparoundTarget = adapterCurrent + (position > current ?
                delta - wrapped.getItemCount() :
                wrapped.getItemCount() + delta);
        int distance = Math.abs(adapterCurrent - target);
        int wraparoundDistance = Math.abs(adapterCurrent - wraparoundTarget);
        if (distance == wraparoundDistance) {
            //Scroll to the right feels more natural, so prefer it
            return target > adapterCurrent ? target : wraparoundTarget;
        } else {
            return distance < wraparoundDistance ? target : wraparoundTarget;
        }
    }

    private int mapPositionToReal(int position) {
        if (position < CENTER) {
            int rem = (CENTER - position) % wrapped.getItemCount();
            return rem == 0 ? 0 : wrapped.getItemCount() - rem;
        } else {
            return (position - CENTER) % wrapped.getItemCount();
        }
    }

    private boolean isResetRequired(int requestedPosition) {
        return isInfinite()
            && (requestedPosition <= RESET_BOUND
            || requestedPosition >= (Integer.MAX_VALUE - RESET_BOUND));
    }

    private void ensureValidPosition(int position) {
        if (position >= wrapped.getItemCount()) {
            throw new IndexOutOfBoundsException(String.format(Locale.US,
                    "requested position is outside adapter's bounds: position=%d, size=%d",
                    position, wrapped.getItemCount()));
        }
    }

    private boolean isInfinite() {
        return wrapped.getItemCount() > 1;
    }

    @Override
    public int getInitialPosition() {
        return isInfinite() ? CENTER : 0;
    }

    private void setPosition(int position) {
        layoutManager.scrollToPosition(position);
    }

    //TODO: handle proper data set change notifications
    private class DataSetChangeDelegate extends RecyclerView.AdapterDataObserver {

        @Override
        public void onChanged() {
            setPosition(getInitialPosition());
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
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

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            notifyItemRangeChanged(0, getItemCount());
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            notifyItemRangeChanged(0, getItemCount(), payload);
        }
    }
}