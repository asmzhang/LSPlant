cmake_minimum_required(VERSION 3.18)
project(helper-standalone LANGUAGES C CXX)

# Standalone 自由态 - 最终产物
# 目标：注入、单 so、不被任何人依赖
# 特征：
# - c++_shared
# - exceptions / RTTI
# - LTO
# - strip / split symbols

# 添加核心源码
add_subdirectory(
    ${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp
    helper-core-build
)

# Add lsplant core sources for standalone linking.
# 查找 libcxx (NDK 25+ 不再提供 cxx prefab 包，已移除)

# 创建 Standalone 库（自由规则）
add_library(helper_standalone SHARED
    helper_standalone_entry.cc
)

# 设置输出名称
set_target_properties(helper_standalone PROPERTIES
    OUTPUT_NAME "helper"
    POSITION_INDEPENDENT_CODE ON
)

# 包含目录
target_include_directories(helper_standalone PUBLIC
    ${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp
)

# Standalone 自由规则：link helper-core 和 自动处理 c++_shared
# 添加全量保留参数，防止由于空入口导致静态库逻辑全被死代码剔除 (1kb size)
target_link_libraries(helper_standalone PRIVATE
    -Wl,--whole-archive
    helper-core
    -Wl,--no-whole-archive
)

# Standalone 自由规则：启用 exceptions 和 RTTI
target_compile_options(helper_standalone PRIVATE
    -fexceptions
    -frtti
)

# Standalone 自由规则：使用 LTO
target_compile_options(helper_standalone PRIVATE
    -flto
)
target_link_options(helper_standalone PRIVATE
    -flto
)

# Standalone 自由规则：使用共享 STL
set_target_properties(helper_standalone PROPERTIES
    ANDROID_STL c++_shared
)

# 提取符号并 strip
if (DEFINED DEBUG_SYMBOLS_PATH)
    add_custom_command(TARGET helper_standalone POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}
        COMMAND ${CMAKE_OBJCOPY} --only-keep-debug $<TARGET_FILE:helper_standalone>
                ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/helper
        COMMAND ${CMAKE_STRIP} --strip-all $<TARGET_FILE:helper_standalone>
        COMMAND ${CMAKE_OBJCOPY} --add-gnu-debuglink=${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/helper
                $<TARGET_FILE:helper_standalone>
        COMMENT "Extracting debug symbols and stripping binary"
    )
endif()
