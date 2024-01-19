package com.bely.automotiveplayerclient;

import android.app.Application;
import android.content.Context;

public class PlayerApp extends Application {
    static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext() {
        return mContext;
    }
}
