# LSPlant

![](https://img.shields.io/badge/license-LGPL--3.0-orange.svg)
![](https://img.shields.io/badge/Android-5.0%20--%2015%20Beta2-blue.svg)
![](https://img.shields.io/badge/arch-armeabi--v7a%20%7C%20arm64--v8a%20%7C%20x86%20%7C%20x86--64%7C%20riscv64-brightgreen.svg)
![](https://github.com/LSPosed/LSPlant/actions/workflows/build.yml/badge.svg?branch=master&event=push)
![](https://img.shields.io/maven-central/v/org.lsposed.lsplant/lsplant.svg)

LSPlant 是一款用于 Android ART 的轻量级 Hook 库，它提供了针对 Java 方法的 Hook/Unhook（挂钩与解挂接）能力，并支持内联去优化 (inline deoptimization)。

该项目属于 LSPosed 框架架构下的一部分，并遵循 GNU LGPL 开源协议发行。

## 核心功能与特性概览

+ 广泛的系统兼容：支持 Android 5.0 至 Android 15 Beta2 (API 级别 21 - 35)
+ 全架构适配：囊括 `armeabi-v7a`, `arm64-v8a`, `x86`, `x86-64` 以及最新的 `riscv64`
+ 深度 Native 支持：支持并整合了定制化的 Native Inline hook 框架（如 Dobby）及底层 ART 符号解析引擎

## 三种不同的编译发布配置 (Build Modes)

本项目在 `CMakeLists.txt` 和 `build.gradle` 中划分了三种不同的模块配置，以适应从开发调试到发行的应用场景：

1. **`build-prefab` (Prefab 发布稳健态)**：对外分发契约版。它旨在生成由外部项目使用的标准依赖包 (AAR)。它强制移除了 Exception/RTTI 等高级 C++ 特性并且藏匿 C++ 符号层实现，绑定静态 STL，来保障与其它宿主工程集成时环境绝对纯净、防止 ABI 炸毁。
2. **`build-standalone` (Standalone 独立性能态)**：极致性能自由版。为生成独立注入 `.so` 的场景准备的最终发版物目标。允许共享内部全部特性库，并且配置了超强的编译优化（LTO）并剥离了附加符号，以此实现库体积的压榨和极限性能。
3. **`build-srcdebug` (源码调试实验室态)**：开发调试版。专为了开发者在 AS 里追进底层排查深坑环境时设计。全面降频编译器的一切优化手法 (`-O0`) 保留极完整的 `g3` 调试数据链，向外部全量公开所有的导出与执行符号栈链，直接用它结合 LLDB 即查即用。

## 文档参考

API 原理和 C++ 原生方法注解请参考：https://lsposed.org/LSPlant/namespacelsplant.html

## 快速接入向导

如果您的其他新项目依然采用 Android Studio 和 Gradle，可以直接拉取线上仓库进行快速应用构建。

```gradle
repositories {
    mavenCentral()
}

android {
    buildFeatures {
        prefab true
    }
}

dependencies {
    implementation "org.lsposed.lsplant:lsplant:+"
}
```

如果您不希望自动打包 `libc++_shared.so` 文件并且想保持轻量化，可以使用 `lsplant-standalone`：

```gradle
dependencies {
    implementation "org.lsposed.lsplant:lsplant-standalone:+"
}
```

### 1. 在 JNI_OnLoad 内完成环境初始化 (Init)

初始化 LSPlant 以支持后续的挂接操作，它主要的生命周期任务涵盖预执行提取、符号抽取绑定，以及先决关键函数的绑定处理。

+ `env` : 当前的 JNI Java 环境句柄。
+ `info` : 用于配置初始化设置的结构体信息。

  这一过程一般是通过为 info 提供一个可靠的内联 Hook 方法体配置以及 `libart.so` 的符号解析组件，来精准定位和取得 Android 操作系统的 ART 核心机制内的隐藏/未导出函数。

```c++
bool Init(JNIEnv *env,
          const InitInfo &info);
```

**返回布尔值（是/否成功）**：如果在正式成功执行此初始化前提早强行去调用其它 LSPlant 的接口，抑或是在本方法返回了 false 的情景下继续跑其它接口，所有行为都将陷入不可预测的崩乱中。

### 2. 挂接方法 (Hook)

通过提供需干预的目标方法（`target_method`）、上下文承载对象（`hooker_object`）以及替换用的回调处理函数（`callback_method`），即可成功完成目标 Java 方法的干预修改。

+ `env` : 当前的 JNI Java 环境上下文句柄。
+ `target_method` : 您期望在系统层面对其发起劫持替换的原生目标 `Method` （反射封装的对象）。
+ `hooker_object` : 此对象将被充当上下文记忆体。
  它可以被用来存放原本的挂接前的方法体备份。这样您的 `callback_method` 被系统不小心呼叫并处理完成后，还是可以原封不动的把原来的程序原本应该走的操作执行一遍。还有个妙用是在 Xposed 框架中多个不同的外挂都想拦截同一个方法的时候，可以用它做堆叠与调度池化，从而不冲突。
+ `callback_method` : 一个具体的 `Method` 实例对象！它是从属于 `hooker_object` 之下用来取缔掉 `target_method` 的傀儡代理。

  只要 `target_method` 被任何其它代码给引用执行了，都会被阻断并跳转进这个预设的 `callback_method` 里处理。所以此替身回调方法它必须要满足且只能是这样的硬性签名定义：`public Object callback_method(Object []args)`。

  也就是强制规定无论拦截源返回了什么类型最终此代理方法只能传走 `Object` 给下一位，形参传参强制是 `Object[]`。哪怕参数对不上也不行（将酿成行为坍塌）。

```c++
jobject Hook(JNIEnv *env,
             jobject target_method,
             jobject hooker_object,
             jobject callback_method);
```

**返回备份方法 (Backup Method)**：您可以随时保留反射去利用。但如果返回 null 代表干预失败。

同时它会自动在内存栈里编织打桩所用的桩点类。若需更详细的日志辅助，可以通过填充 `InitInfo` 的 `generated_*` 参数项来进行细化的排错。

*注：该函数自身是支持并发安全的！多线程并行跑它不会炸，可是若针对同一个 `target_method` 却不享有原子性保护。意味着你不可能在一边同时执行对该方法的 `UnHook` 又在干预挂接，结果将难以捉摸。*

### 3. 检测挂接状态 (Check)

核验一个普通 Java 方法是否正身陷在由 LSPlant 部署编织的 Hook 状态中。

```c++
bool IsHooked(JNIEnv *env,
              jobject method);
```

返回一个标识 true/false 。

### 4. 移除并恢复方法 (Unhook)

在玩腻后，完美无缝地退场并解脱之前下辖的一个劫持关联干预体。

+ `env` : JNI Java 句柄环境
+ `target_method` : 此前曾执行过拦截的方法实体 (`Method` 资源对象)

```c++
bool UnHook(JNIEnv *env,
            jobject target_method);
```

返回是/否成功将流程拨乱反正。
**警告**：一旦执行了解除退场，再尝试利用之前获取存起来的那一份 `Hook()` 赋予的回执指针函数去模拟调用，必吃崩溃。

### 5. 阻止方法内联去优化 (Deoptimize)

反向去优化一个被系统彻底编译吃透吸收并内联扁平化后的目标方法，强制确保它不被优化处理掉以使得即使在调用极短逻辑的时候它能够继续被跳转处理。

+ `env` : JNI Java 句柄
+ `method` : 一个将它退回原始字节数组未编译形态强制剥夺优化权利的目标 `Method`。

  这是由于 Android 操作机制发现你比如短的 B 函数常常被另一个庞大的 A 函数叫走后，A 会觉得太麻烦把 B 直接并入 A 的运行段。这时候您去用 LSPlant 写好在外部准备搞 B 函数的钩子逻辑压根不可能捕捉得到！为杜绝这点，您可以选择“让老大哥带头冲锋”，把这个 A 取消系统的各种魔法编译待遇，重新还原流程分离 B 等等环节，便能顺当触发对于 B 的预设陷阱。虽然麻烦，却最直接。

```c++
bool Deoptimize(JNIEnv *env,
                jobject method);
```

返回是/否成功完成了对其的反向削弱操作。由于是对它的克隆代理体生效，就算您对正在活跃执行的代码投射这段剥夺逻辑也不用虚，系统兜得住。

---
## 致谢 / 鸣谢灵感来源
此框架项目的诞生与结构深度汲取了以下众多同类出界安全探索先驱的指导经验：
- [YAHFA](https://github.com/PAGalaxyLab/YAHFA)
- [SandHook](https://github.com/asLody/SandHook)
- [Pine](https://github.com/canyie/pine)
- [Epic](https://github.com/tiann/epic)
