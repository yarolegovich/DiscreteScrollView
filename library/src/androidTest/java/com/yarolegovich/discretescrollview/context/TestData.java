package com.yarolegovich.discretescrollview.context;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;

import java.util.Random;

/**
 * Created by yarolegovich on 2/4/18.
 */

public class TestData {

    private static int NEXT_ID = 1;
    private static final Random random = new Random();

    public final int id;
    public final Drawable image;

    public TestData() {
        id = NEXT_ID++;
        image = new ColorDrawable(generateRandomColor());
    }

    @ColorInt
    private static int generateRandomColor() {
        return Color.argb(255,
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256));
    }
}
