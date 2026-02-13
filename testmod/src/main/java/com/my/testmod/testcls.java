package com.my.testmod;

import android.util.Log;

import androidx.annotation.Keep;

public class testcls {

    @Keep
    public static int test(int x,int y, Object[] obj2Arr){
        y=666;
        obj2Arr[0]=x+y;
        Log.d("test","test");
        return x+y;
    }
}
