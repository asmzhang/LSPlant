cmake_minimum_required(VERSION 3.18)
project(helper-srcdebug LANGUAGES C CXX)

# 源码调试态 - 开发态
# 目的：
# - lldb 单步进
# - 看 C++ modules
# - exceptions / RTTI / STL 随便用
# 特征：
# - 不用 Prefab
# - 不考虑 ABI 传播
# - add_subdirectory(core)
# - -g3 -O0

# 添加核心源码
add_subdirectory(
    ${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp
    helper-core-build
)

# 添加 lsplant 源码
add_subdirectory(
    ${CMAKE_CURRENT_LIST_DIR}/../../lsplant/src/main/jni
    lsplant-core-build
)

# 创建调试库（实验室模式）
add_library(helper_debug SHARED
    helper_debug_entry.cc
    ${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp/hooker.cpp
)

# 设置输出名称
set_target_properties(helper_debug PROPERTIES
    OUTPUT_NAME "helper"
    POSITION_INDEPENDENT_CODE ON
)

# 包含目录
target_include_directories(helper_debug PUBLIC
    ${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp
    ${CMAKE_CURRENT_LIST_DIR}/../../lsplant/src/main/jni
)

# 源码调试规则：link log, android, helper-core 和 lsplant-core
target_link_libraries(helper_debug PRIVATE
    helper-core
    lsplant-core
)

# 源码调试规则：启用 exceptions 和 RTTI
target_compile_options(helper_debug PRIVATE
    -fexceptions
    -frtti
)

# 源码调试规则：最大调试信息，禁用优化
target_compile_options(helper_debug PRIVATE
    -g3
    -O0
)

# 源码调试规则：不使用 LTO
target_compile_options(helper_debug PRIVATE
    -fno-lto
)
target_link_options(helper_debug PRIVATE
    -fno-lto
)

# 源码调试规则：使用静态 STL（避免 NDK 29 的 libc++_shared.so 损坏问题）
set_target_properties(helper_debug PROPERTIES
    ANDROID_STL c++_static
)

# 源码调试规则：导出所有符号便于调试
target_compile_options(helper_debug PRIVATE
    -fvisibility=default
)
