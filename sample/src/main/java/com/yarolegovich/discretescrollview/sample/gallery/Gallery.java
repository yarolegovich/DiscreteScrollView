package com.yarolegovich.discretescrollview.sample.gallery;

import android.content.Context;
import android.content.SharedPreferences;

import com.yarolegovich.discretescrollview.sample.App;
import com.yarolegovich.discretescrollview.sample.R;
import com.yarolegovich.discretescrollview.sample.shop.Item;
import com.yarolegovich.discretescrollview.sample.shop.Shop;

import java.util.Arrays;
import java.util.List;

/**
 * Created by yarolegovich on 16.03.2017.
 */

public class Gallery {

    public static Gallery get() {
        return new Gallery();
    }

    private Gallery() {
    }

    public List<Image> getData() {
        return Arrays.asList(
                new Image(R.drawable.shop1),
                new Image(R.drawable.shop2),
                new Image(R.drawable.shop3),
                new Image(R.drawable.shop4),
                new Image(R.drawable.shop5),
                new Image(R.drawable.shop6));
    }
}
