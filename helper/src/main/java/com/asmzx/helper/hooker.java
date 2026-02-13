package com.asmzx.helper;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import android.os.Looper;
import android.widget.Toast;

public class hooker {

    private static Context hostContext;
    /**
     * 测试 hook 功能
     */
    public static void hooks() {
        Log.d("hooker", "test() called");

        try {
            // hook Application.attachBaseContext
            XposedBridge.hookMethod(
                    Reflect.on(Application.class)
                            .exactMethod("attachBaseContext", Context.class),
                    new XC_MethodHook() {

                        // 保证只初始化一次
                        private boolean inited = false;

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                            // ⚠️ 核心判断：thisObject 必须是 Application
                            if (!(param.thisObject instanceof Application)) {
                                return;
                            }

                            // 防重复初始化
                            if (inited) return;
                            inited = true;

                            Application app = (Application) param.thisObject;
                            Context base = (Context) param.args[0];
                            hostContext = base;

                            Log.d("hooker", "Application.attachBaseContext hooked: " + app.getClass().getName());

                            // Toast 仅在主线程
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                try {
                                    Toast.makeText(hostContext,
                                            "Hook Application.attachBaseContext success!!!",
                                            Toast.LENGTH_LONG).show();
                                    Log.d("hooker", "Hook Application.attachBaseContext success!!!");

                                    // 调用你的 hook 方法
                                    hook();
                                } catch (Throwable tr) {
                                    Log.e("hooker", "Error showing Toast", tr);
                                }
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.thisObject instanceof Application) {
                                Log.d("hooker", "afterHookedMethod attachBaseContext: " +
                                        param.thisObject.getClass().getName());
                            }
                        }
                    }
            );
        } catch (Throwable tr) {
            Log.e("hooker", "Error to hook Application.attachBaseContext", tr);
        }
    }

    private static void hook() {
        try {
            Class<?> targetClass = Class.forName("com.my.testmod.testcls");
            Method targetMethod = targetClass.getDeclaredMethod("test", int.class, int.class, Object[].class);
            targetMethod.setAccessible(true);

            XposedBridge.hookMethod(targetMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Log.d("hooker", "beforeHookedMethod called");
                    int x = (int) param.args[0];
                    int y = (int) param.args[1];
                    Log.d("hooker", "original args: x=" + x + ", y=" + y);
                    param.args[0] = 888;
                    Log.d("hooker", "modified args: x=" + param.args[0] + ", y=" + param.args[1]);
//                    param.setResult(888888);//直接返回,原方法体不会执行
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Log.d("hooker", "afterHookedMethod called");
                    Object[] arr = (Object[]) param.args[2];
                    Object old = arr[0];

                    Log.d("hooker", "obj2Arr[0] before modify = " + old);

                    // ✅ 修改引用内容
                    arr[0] = 12345678;

                    Log.d("hooker", "obj2Arr[0] after modify = " + arr[0]);
                    // 可选：改返回值
                    int result = (int) param.getResult();
                    Log.d("hooker", "result before modify = " + result);
                    result+=1;
                    param.setResult(result);
                    Log.d("hooker", "result after modify = " + result);


                }
            });

            Log.d("hooker", "hook installed successfully");
        } catch (Exception e) {
            Log.e("hooker", "hook failed", e);
        }
    }
}
