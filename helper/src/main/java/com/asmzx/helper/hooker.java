package com.asmzx.helper;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class hooker {

    private static Context hostContext;

    public static void hooks() {
        Log.d("hooker", "hooks() called");

        try {
            XposedBridge.hookMethod(
                    Reflect.on(Application.class).exactMethod("attachBaseContext", Context.class),
                    new XC_MethodHook() {
                        private boolean inited = false;

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!(param.thisObject instanceof Application)) {
                                return;
                            }
                            if (inited) {
                                return;
                            }
                            inited = true;

                            Application app = (Application) param.thisObject;
                            hostContext = (Context) param.args[0];
                            Log.d("hooker", "Application.attachBaseContext hooked: " + app.getClass().getName());

                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                try {
                                    Toast.makeText(hostContext, "LSPlant hook ready", Toast.LENGTH_SHORT).show();
                                } catch (Throwable tr) {
                                    Log.e("hooker", "Toast failed", tr);
                                }
                            }

                            hookStringFromJNI();
                        }
                    }
            );
        } catch (Throwable tr) {
            Log.e("hooker", "Error hooking Application.attachBaseContext", tr);
        }
    }

    private static void hookStringFromJNI() {
        try {
            Class<?> targetClass = Class.forName("com.asmzx.android_attack_defense_test.MainActivity");
            Method targetMethod = targetClass.getDeclaredMethod("stringFromJNI");
            targetMethod.setAccessible(true);

            XposedBridge.hookMethod(targetMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    String original = (String) param.getResult();
                    if (TextUtils.isEmpty(original)) {
                        original = "empty";
                    }
                    String hooked = original + " [hooked by LSPlant]";
                    param.setResult(hooked);
                    Log.d("hooker", "stringFromJNI result -> " + hooked);
                }
            });

            Log.d("hooker", "stringFromJNI hook installed successfully");
        } catch (Throwable tr) {
            Log.e("hooker", "stringFromJNI hook failed", tr);
        }
    }
}
