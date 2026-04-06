cmake_minimum_required(VERSION 3.18)
project(lsplant-standalone LANGUAGES C CXX)

# Standalone 自由态 - 最终产物
# 目标：注入、单 so、不被任何人依赖
# 特征：
# - c++_shared
# - exceptions / RTTI
# - LTO
# - strip / split symbols

# 添加核心源码
add_subdirectory(
    ${CMAKE_CURRENT_LIST_DIR}/../src/main/jni
    lsplant-core-build
)

# 查找 libcxx (NDK 25+ 已废弃)

# 创建 Standalone 库（自由规则）
add_library(lsplant_standalone SHARED
    standalone_entry.cc
)

# 设置输出名称
set_target_properties(lsplant_standalone PROPERTIES
    OUTPUT_NAME "lsplant"
    POSITION_INDEPENDENT_CODE ON
)

# 包含目录
target_include_directories(lsplant_standalone PUBLIC
    ${CMAKE_CURRENT_LIST_DIR}/../src/main/jni
)

# Standalone 自由规则：link lsplant-core 和 自动处理 c++_shared
# 增加 --whole-archive 强制防止 LTO 以及 ld 死代码消除把空项目直接剥离干净
target_link_libraries(lsplant_standalone PRIVATE
    -Wl,--whole-archive
    lsplant-core
    -Wl,--no-whole-archive
)

# Standalone 自由规则：启用 exceptions 和 RTTI
target_compile_options(lsplant_standalone PRIVATE
    -fexceptions
    -frtti
)

# Standalone 自由规则：使用 LTO
target_compile_options(lsplant_standalone PRIVATE
    -flto
)
target_link_options(lsplant_standalone PRIVATE
    -flto
)

# Standalone 自由规则：使用共享 STL
set_target_properties(lsplant_standalone PROPERTIES
    ANDROID_STL c++_shared
)

# 提取符号并 strip
if (DEFINED DEBUG_SYMBOLS_PATH)
    add_custom_command(TARGET lsplant_standalone POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}
        COMMAND ${CMAKE_OBJCOPY} --only-keep-debug $<TARGET_FILE:lsplant_standalone>
                ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/lsplant
        COMMAND ${CMAKE_STRIP} --strip-all $<TARGET_FILE:lsplant_standalone>
        COMMAND ${CMAKE_OBJCOPY} --add-gnu-debuglink=${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/lsplant
                $<TARGET_FILE:lsplant_standalone>
        COMMENT "Extracting debug symbols and stripping binary"
    )
endif()
