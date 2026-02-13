package com.my.testmod;

//import static com.asmzx.antidebug.antidebughelper.startAntiDebug;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class testmodcls {

    // Used to load the 'testmodcls' library on application startup.
    static {
        System.loadLibrary("testmodcls");
    }


    public static void start_test(){
        //        Object[] obj2Arr = new Object[1];
//
//
//        int i=1;
//        int j=2;
//        obj2Arr[0] = 0;
//
//        testcls.test(i,j,obj2Arr);
//        tv.setText(obj2Arr[0].toString());
//
//        Log.d("test","obj2Arr[0]: " + obj2Arr[0]);

        Class<?> cls = null; // 目标类
        try {
            cls = Class.forName("com.my.testmod.testcls");
//            Object obj = cls.newInstance(); // 如果是静态方法，可以不需要这个实例

            // 获取方法，参数类型要准确
            Method method = cls.getDeclaredMethod("test", int.class, int.class,Object[].class);
            method.setAccessible(true); // 私有方法也能调用

            // 调用方法
//            Object result = method.invoke(obj, "hello", 123);
            Object[] objs = new Object[1];
            Object result = method.invoke(null,1, 2,objs);
//            System.out.println("结果: " + result);

            Log.d("test","结果: " + result+" objs[0]:"+objs[0]);
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                 IllegalAccessException e) {
            Log.e("test", "invoke failed", e);
            throw new RuntimeException(e);
        }


//        startAntiDebug();
		// 创建实例并调用非静态方法
		testmodcls instance = new testmodcls();
		Log.d("test","stringFromJNI: " + instance.stringFromJNI());
    }
    /**
     * A native method that is implemented by the 'testmodcls' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}