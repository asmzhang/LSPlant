package com.asmzx.android_attack_defense_test;

import static com.my.testmod.testmodcls.start_test;

import android.app.Application;
import android.content.Context;

import com.asmzx.helper.hooker;
//import z.z.loader.Loader;

public class App extends Application {

    static {
        // 初始化成功后，挂上钩子
//        hooker.test();
        System.loadLibrary("helper");
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        start_test();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
