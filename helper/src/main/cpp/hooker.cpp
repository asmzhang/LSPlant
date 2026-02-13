#include <jni.h>
#include <string>
#include <dobby.h>
#include <sys/mman.h>
#include <bits/sysconf.h>
#include "elf_img.h"
#include "log.h"
#include "profile_saver.h"
#include "hidden_api.h"
#include <sys/system_properties.h>
#include <cstdlib>
#include <cerrno>
#include "aliuhook.h"
#include "invoke_constructor.h"

#include "lsplant.hpp"


int AliuHook::android_version = -1;
pine::ElfImg AliuHook::elf_img; // NOLINT(cert-err58-cpp)

void AliuHook::init(int version) {
    elf_img.Init("libart.so", version);
    android_version = version;
}

static size_t page_size_;

// Macros to align addresses to page boundaries
#define ALIGN_DOWN(addr, page_size)         ((addr) & -(page_size))
#define ALIGN_UP(addr, page_size)           (((addr) + ((page_size) - 1)) & ~((page_size) - 1))

static bool Unprotect(void *addr) {
    auto addr_uint = reinterpret_cast<uintptr_t>(addr);
    auto page_aligned_prt = reinterpret_cast<void *>(ALIGN_DOWN(addr_uint, page_size_));
    size_t size = page_size_;
    if (ALIGN_UP(addr_uint + page_size_, page_size_) != ALIGN_UP(addr_uint, page_size_)) {
        size += page_size_;
    }

    int result = mprotect(page_aligned_prt, size, PROT_READ | PROT_WRITE | PROT_EXEC);
    if (result == -1) {
        LOGE("mprotect failed for %p: %s (%d)", addr, strerror(errno), errno);
        return false;
    }
    return true;
}

void *InlineHooker(void *address, void *replacement) {
    if (!Unprotect(address)) {
        return nullptr;
    }

    void *origin_call;
    if (DobbyHook(address, (dobby_dummy_func_t)replacement, (dobby_dummy_func_t *)&origin_call) == 0) {
        return origin_call;
    } else {
        return nullptr;
    }
}

bool InlineUnhooker(void *func) {
    return DobbyDestroy(func) == RT_SUCCESS;
}

void call_hooks(JNIEnv* env){
     jclass hookerClass = env->FindClass("com/asmzx/helper/hooker");
      if (hookerClass == nullptr) {
          LOGE("Failed to find hooker class");
          return ;
      }

      jmethodID hooksMethod = env->GetStaticMethodID(hookerClass, "hooks", "()V");
      if (hooksMethod == nullptr) {
          LOGE("Failed to find hooks method");
          env->DeleteLocalRef(hookerClass);
          return ;
      }

      env->CallStaticVoidMethod(hookerClass, hooksMethod);
      env->DeleteLocalRef(hookerClass);
      LOGI("hooks() method called successfully");
    return;
}


extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

//    page_size_ = static_cast<const size_t>(sysconf(_SC_PAGESIZE));
//
//    {
//        int api_level = android_get_device_api_level();
//
//        if (api_level <= 0) {
//            LOGE("Invalid SDK int %i", api_level);
//            return JNI_ERR;
//        }
//
//        AliuHook::init(static_cast<int>(api_level));
//    }
//
//    lsplant::InitInfo initInfo{
//            .inline_hooker = InlineHooker,
//            .inline_unhooker = InlineUnhooker,
//            .art_symbol_resolver = [](std::string_view symbol) -> void * {
//                return AliuHook::elf_img.GetSymbolAddress(symbol, false, false);
//            },
//            .art_symbol_prefix_resolver = [](std::string_view symbol) -> void * {
//                return AliuHook::elf_img.GetSymbolAddress(symbol, false, true);
//            }
//    };
//
//    int res = lsplant::Init(env, initInfo);
//    if (res == lsplant::INIT_FAILED) {
//        LOGE("lsplant init failed");
//        return JNI_ERR;
//    } else if (res == lsplant::INIT_ALREADY_DONE) {
//        LOGI("LSPlant already initialized, skipping remaining initialization");
//        return JNI_VERSION_1_6;
//    }
//
//    LOGI("lsplant init finished");
//
//    bool cache_res = LoadInvokeConstructorCache(env, AliuHook::android_version);
//    if (!cache_res) {
//        LOGE("invoke_constructor init failed");
//        return JNI_ERR;
//    }

     // ★ 安装 attachBaseContext hook
//     HookAttachBaseContext(env);
    call_hooks(env);
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_1);

    UnloadInvokeConstructorCache(env);
}



// // extern "C" JNIEXPORT
// // jint JNI_OnLoad(JavaVM *vm, void *reserved)//相当于dll的dllmain
// // {
// //     LOGI("JNI_OnLoad");

// //     JNIEnv *env = nullptr;
// //     if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
// //         LOGE("Failed to get JNIEnv");
// //         return JNI_ERR;
// //     }

// //     jclass hookerClass = env->FindClass("com/asmzx/helper/hooker");
// //     if (hookerClass == nullptr) {
// //         LOGE("Failed to find hooker class");
// //         return JNI_ERR;
// //     }

// //     jmethodID hooksMethod = env->GetStaticMethodID(hookerClass, "hooks", "()V");
// //     if (hooksMethod == nullptr) {
// //         LOGE("Failed to find hooks method");
// //         env->DeleteLocalRef(hookerClass);
// //         return JNI_ERR;
// //     }

// //     env->CallStaticVoidMethod(hookerClass, hooksMethod);
// //     env->DeleteLocalRef(hookerClass);
// //     LOGI("hooks() method called successfully");

// // return JNI_VERSION_1_6;
// // }

// // extern "C"
// // JNIEXPORT
// // void JNI_OnUnload(JavaVM *vm, void *reserved)
// // {
// //     LOGI("JNI_OnUnload");
// // }