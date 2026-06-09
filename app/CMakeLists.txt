cmake_minimum_required(VERSION 3.22.1)

project("kuaihui_native")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 添加编译选项
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden -fvisibility-inlines-hidden")
add_compile_options(-Wall -Wextra -Wno-unused-parameter)

# 设置 app 目录
set(APP_DIR ${CMAKE_CURRENT_SOURCE_DIR})

# 包含目录
include_directories(${APP_DIR}/src/main/cpp/include)

# 源文件列表 - 使用绝对路径
set(NATIVE_SOURCES
    ${APP_DIR}/src/main/cpp/src/kuaihui_native_engine.cpp
    ${APP_DIR}/src/main/cpp/src/SDEngine.cpp
    ${APP_DIR}/src/main/cpp/src/BPETokenizer.cpp
    ${APP_DIR}/src/main/cpp/src/VAEDecoder.cpp
    ${APP_DIR}/src/main/cpp/src/UNetDenoiser.cpp
    ${APP_DIR}/src/main/cpp/src/Scheduler.cpp
    ${APP_DIR}/src/main/cpp/src/ProgressReporter.cpp
)

# 创建共享库
add_library(kuaihui_native SHARED ${NATIVE_SOURCES})

# MNN 预编译库路径
set(MNN_LIB_DIR "${CMAKE_SOURCE_DIR}/src/main/jniLibs")

# 根据 ABI 选择 MNN 库路径
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(MNN_LIB_PATH "${MNN_LIB_DIR}/arm64-v8a")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(MNN_LIB_PATH "${MNN_LIB_DIR}/armeabi-v7a")
else()
    set(MNN_LIB_PATH "${MNN_LIB_DIR}")
endif()

message(STATUS "MNN Library Path: ${MNN_LIB_PATH}")
message(STATUS "ANDROID ABI: ${ANDROID_ABI}")

# 链接 MNN 预编译库
if(EXISTS "${MNN_LIB_PATH}/libMNN.so")
    message(STATUS "Found libMNN.so")
    add_library(MNN SHARED IMPORTED)
    set_target_properties(MNN PROPERTIES IMPORTED_LOCATION "${MNN_LIB_PATH}/libMNN.so")
    target_link_libraries(kuaihui_native MNN)
endif()

if(EXISTS "${MNN_LIB_PATH}/libMNN_Express.so")
    message(STATUS "Found libMNN_Express.so")
    add_library(MNN_Express SHARED IMPORTED)
    set_target_properties(MNN_Express PROPERTIES IMPORTED_LOCATION "${MNN_LIB_PATH}/libMNN_Express.so")
    target_link_libraries(kuaihui_native MNN_Express)
endif()

if(EXISTS "${MNN_LIB_PATH}/libMNN_CL.so")
    message(STATUS "Found libMNN_CL.so")
    add_library(MNN_CL SHARED IMPORTED)
    set_target_properties(MNN_CL PROPERTIES IMPORTED_LOCATION "${MNN_LIB_PATH}/libMNN_CL.so")
    target_link_libraries(kuaihui_native MNN_CL)
endif()

if(EXISTS "${MNN_LIB_PATH}/libMNN_Vulkan.so")
    message(STATUS "Found libMNN_Vulkan.so")
    add_library(MNN_Vulkan SHARED IMPORTED)
    set_target_properties(MNN_Vulkan PROPERTIES IMPORTED_LOCATION "${MNN_LIB_PATH}/libMNN_Vulkan.so")
    target_link_libraries(kuaihui_native MNN_Vulkan)
endif()

if(EXISTS "${MNN_LIB_PATH}/libmnncore.so")
    message(STATUS "Found libmnncore.so")
    add_library(mnncore SHARED IMPORTED)
    set_target_properties(mnncore PROPERTIES IMPORTED_LOCATION "${MNN_LIB_PATH}/libmnncore.so")
    target_link_libraries(kuaihui_native mnncore)
endif()

# 查找 Android 库
find_library(log-lib log)
find_library(egl-lib EGL)

# 链接系统库
target_link_libraries(kuaihui_native 
    ${log-lib}
    ${egl-lib}
    android
    jnigraphics
)
