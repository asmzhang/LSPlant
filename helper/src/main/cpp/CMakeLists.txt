cmake_minimum_required(VERSION 3.18)
project(helper-core LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)

set(HELPER_NATIVE_SOURCE
    hooker.cpp
)

add_library(${PROJECT_NAME} STATIC ${HELPER_NATIVE_SOURCE})

target_include_directories(${PROJECT_NAME} PUBLIC
    .
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../lsplant/src/main/jni
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../lsplant/src/main/jni/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../lsplant/src/main/jni/external/LSPDobby/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../lsplant/src/main/jni/external/aliuhook
)

target_compile_options(${PROJECT_NAME} PRIVATE
    -Wall
    -Wextra
)

target_link_libraries(${PROJECT_NAME} PUBLIC
    log
    android
)
