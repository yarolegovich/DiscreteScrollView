package com.yarolegovich.discretescrollview.sample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.yarolegovich.discretescrollview.DiscreteScrollView;

import java.lang.ref.WeakReference;

/**
 * Created by yarolegovich on 08.03.2017.
 */

public class DiscreteScrollViewOptions {

    private static DiscreteScrollViewOptions instance;

    private final String KEY_TRANSITION_TIME;

    public static void init(Context context) {
        instance = new DiscreteScrollViewOptions(context);
    }

    private DiscreteScrollViewOptions(Context context) {
        KEY_TRANSITION_TIME = context.getString(R.string.pref_key_transition_time);
    }

    public static void configureTransitionTime(DiscreteScrollView scrollView) {
        final BottomSheetDialog bsd = new BottomSheetDialog(scrollView.getContext());
        final TransitionTimeChangeListener timeChangeListener = new TransitionTimeChangeListener(scrollView);
        bsd.setContentView(R.layout.dialog_transition_time);
        defaultPrefs().registerOnSharedPreferenceChangeListener(timeChangeListener);
        bsd.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                defaultPrefs().unregisterOnSharedPreferenceChangeListener(timeChangeListener);
            }
        });
        bsd.findViewById(R.id.dialog_btn_dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd.dismiss();
            }
        });
        bsd.show();
    }

    public static void smoothScrollToUserSelectedPosition(final DiscreteScrollView scrollView, View anchor) {
        PopupMenu popupMenu = new PopupMenu(scrollView.getContext(), anchor);
        Menu menu = popupMenu.getMenu();
        for (int i = 0; i < scrollView.getAdapter().getItemCount(); i++) {
            menu.add(String.valueOf(i + 1));
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int destination = Integer.parseInt(String.valueOf(item.getTitle())) - 1;
                scrollView.smoothScrollToPosition(destination);
                return true;
            }
        });
        popupMenu.show();
    }

    public static int getTransitionTime() {
        return defaultPrefs().getInt(instance.KEY_TRANSITION_TIME, 150);
    }

    private static SharedPreferences defaultPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(App.getInstance());
    }

    private static class TransitionTimeChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {

        private WeakReference<DiscreteScrollView> scrollView;

        public TransitionTimeChangeListener(DiscreteScrollView scrollView) {
            this.scrollView = new WeakReference<>(scrollView);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(instance.KEY_TRANSITION_TIME)) {
                DiscreteScrollView scrollView = this.scrollView.get();
                if (scrollView != null) {
                    scrollView.setItemTransitionTimeMillis(sharedPreferences.getInt(key, 150));
                } else {
                    sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
                }
            }
        }
    }
}
