package com.asmzx.helper;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class StableHostAttachHook {

    // Application 对象级防重
    private static final Set<Application> seenApplications =
            Collections.newSetFromMap(new IdentityHashMap<>());

    // 全局只执行一次（可选）
    private static final AtomicBoolean onceGlobal = new AtomicBoolean(false);

    public static void hookApplication(Context initialContext) {
        try {
            // 直接用 BootClassLoader 查找 Application
            Class<?> appClass = XposedHelpers.findClass("android.app.Application", null);

            // hook onCreate() 替代 attachBaseContext（避免 NoSuchMethodError）
            XposedHelpers.findAndHookMethod(appClass, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Application app = (Application) param.thisObject;

                    // 获取 Context
                    Context ctx = app.getApplicationContext();

                    // 获取当前进程名
                    String procName = getProcessName(ctx);
                    if (procName == null) return;

                    // 只处理主进程
                    String pkgName = ctx.getPackageName();
                    if (!pkgName.equals(procName)) {
                        Log.d("hooker", "非主进程跳过: " + procName);
                        return;
                    }

                    // 自动识别宿主 Application: 包名与宿主包名一致
                    if (!app.getClass().getPackage().getName().equals(pkgName)) {
                        Log.d("hooker", "非宿主 Application，跳过: " + app.getClass().getName());
                        return;
                    }

                    // Application 对象级防重
                    if (!seenApplications.add(app)) return;

                    // 全局只执行一次（可选）
                    if (!onceGlobal.compareAndSet(false, true)) return;

                    Log.d("hooker", "宿主 Application onCreate Hook 成功: " + app.getClass().getName());

                    hook(ctx);
                }
            });

        } catch (Throwable tr) {
            Log.e("hooker", "hook Application onCreate 出错", tr);
        }
    }

    private static void hook(Context ctx) {
        // TODO: 你的初始化 / Hook 逻辑
        Log.d("hooker", "执行宿主初始化逻辑");
    }

    private static String getProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                if (info.pid == pid) return info.processName;
            }
        }
        return null;
    }
}

