package com.asmzx.android_attack_defense_test;

import android.app.Application;
import android.content.Context;

import com.asmzx.helper.hooker;

public class App extends Application {

    static {
        System.loadLibrary("helper");
        hooker.hooks();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
