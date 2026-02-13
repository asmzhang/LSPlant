#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_asmzx_android_1attack_1defense_1test_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}