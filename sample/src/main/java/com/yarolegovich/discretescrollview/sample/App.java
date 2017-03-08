package com.yarolegovich.discretescrollview.sample;

import android.app.Application;

/**
 * Created by yarolegovich on 08.03.2017.
 */

public class App extends Application {

    private static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        DiscreteScrollViewOptions.init(this);
    }
}
