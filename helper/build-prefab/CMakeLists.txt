cmake_minimum_required(VERSION 3.18)
project(helper-prefab LANGUAGES C CXX)

# Prefab 发布态 - 对外契约
# 目标：稳定、不炸别人、ABI 可控
# 硬规则：
# - 不 link cxx::cxx
# - 不 exceptions / RTTI
# - 不跨 STL
# - 不 LTO
# - C ABI 边界

# 添加核心源码
add_subdirectory(
    ${CMAKE_CURRENT_LIST_DIR}/../../src/main/cpp
    helper-core-build
)

# 创建 Prefab 库（受限规则）
add_library(helper SHARED
    helper_prefab_entry.cc
)

# 设置输出名称
set_target_properties(helper PROPERTIES
    OUTPUT_NAME "helper"
    POSITION_INDEPENDENT_CODE ON
)

# 包含目录
target_include_directories(helper PUBLIC
    ${CMAKE_CURRENT_LIST_DIR}/../../src/main/cpp
)

# Prefab 硬规则：只 link helper-core
target_link_libraries(helper PRIVATE
    helper-core
)

# Prefab 硬规则：禁用 exceptions 和 RTTI
target_compile_options(helper PRIVATE
    -fno-exceptions
    -fno-rtti
)

# Prefab 硬规则：不使用 LTO
target_compile_options(helper PRIVATE
    -fno-lto
)
target_link_options(helper PRIVATE
    -fno-lto
)

# Prefab 硬规则：使用静态 STL（避免 ABI 问题）
set_target_properties(helper PROPERTIES
    ANDROID_STL c++_static
)

# Prefab 硬规则：不导出 C++ 符号，只导出 C ABI
target_compile_options(helper PRIVATE
    -fvisibility=hidden
)

# 导出 C ABI 符号
target_compile_definitions(helper PRIVATE
    HELPER_EXPORTS
)
