package com.asmzx.android_attack_defense_test;

//import static com.asmzx.antidebug.antidebughelper.startAntiDebug;

//import static com.my.testmod.testmodcls.start_test;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.asmzx.android_attack_defense_test.databinding.ActivityMainBinding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'android_attack_defense_test' library on application startup.
    static {
        System.loadLibrary("android_attack_defense_test");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        //start_test();
    }


    /**
     * A native method that is implemented by the 'android_attack_defense_test' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}