package com.asmzx.helper;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class HostAttachBaseContextHook {

    private static final Set<Application> seenApplications =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private static final AtomicBoolean onceGlobal = new AtomicBoolean(false);

    private static final String HOST_APPLICATION_CLASS = "com.example.app.MyApplication"; // TODO

    public static void hookAttachBaseContext() {
        try {
            XposedHelpers.findAndHookMethod(
                    Application.class,
                    "attachBaseContext",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                            Application app = (Application) param.thisObject;
                            Context ctx = (Context) param.args[0];

                            // 获取当前进程名
                            String proc = getProcessName(ctx);
                            if (proc == null) return;

                            // 只处理主进程
                            String pkg = ctx.getPackageName();
                            if (!pkg.equals(proc)) {
                                Log.d("hooker", "非主进程跳过: " + proc);
                                return;
                            }

                            // 过滤非宿主 Application
                            if (!app.getClass().getName().equals(HOST_APPLICATION_CLASS)) {
                                Log.d("hooker", "非宿主 Application，跳过: " + app.getClass().getName());
                                return;
                            }

                            // Application 对象级防重
                            if (!seenApplications.add(app)) return;

                            // 全局只执行一次
                            if (!onceGlobal.compareAndSet(false, true)) return;

                            Context hostContext = app.getApplicationContext();
                            Log.d("hooker", "宿主 attachBaseContext Hook 成功: " + app.getClass().getName());

                            hook(hostContext);
                        }
                    }
            );
        } catch (Throwable tr) {
            Log.e("hooker", "hook attachBaseContext 出错", tr);
        }
    }

    private static void hook(Context ctx) {
        Log.d("hooker", "执行宿主初始化逻辑");
    }

    private static String getProcessName(Context context) {
        int pid = android.os.Process.myPid();
        android.app.ActivityManager am = (android.app.ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            for (android.app.ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                if (info.pid == pid) return info.processName;
            }
        }
        return null;
    }
}

