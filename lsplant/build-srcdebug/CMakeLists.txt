cmake_minimum_required(VERSION 3.18)
project(lsplant-srcdebug LANGUAGES C CXX)

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
    ${CMAKE_CURRENT_LIST_DIR}/../src/main/jni
    lsplant-core-build
)

# 创建调试库
add_library(lsplant_debug SHARED
    debug_entry.cc
)

# 设置输出名称
set_target_properties(lsplant_debug PROPERTIES
    OUTPUT_NAME "lsplant"
    POSITION_INDEPENDENT_CODE ON
)

# 包含目录
target_include_directories(lsplant_debug PUBLIC
    ${CMAKE_CURRENT_LIST_DIR}/../src/main/jni
)

# 源码调试规则：link log 和 lsplant-core
# 使用 --whole-archive 确保包含所有符号（包括 JNI_OnLoad）
target_link_libraries(lsplant_debug PRIVATE
    -Wl,--whole-archive
    lsplant-core
    -Wl,--no-whole-archive
    log
)

# 源码调试规则：启用 exceptions 和 RTTI
target_compile_options(lsplant_debug PRIVATE
    -fexceptions
    -frtti
)

# 源码调试规则：最大调试信息，禁用优化
target_compile_options(lsplant_debug PRIVATE
    -g3
    -O0
)

# 源码调试规则：不使用 LTO
target_compile_options(lsplant_debug PRIVATE
    -fno-lto
)
target_link_options(lsplant_debug PRIVATE
    -fno-lto
)

# 源码调试规则
set_target_properties(lsplant_debug PROPERTIES
    ANDROID_STL c++_static
)

# 源码调试规则：导出所有符号便于调试
target_compile_options(lsplant_debug PRIVATE
    -fvisibility=default
)
