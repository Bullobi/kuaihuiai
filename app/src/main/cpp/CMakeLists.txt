cmake_minimum_required(VERSION 3.22.1)

project("kuaihui_native")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# 添加编译选项
add_compile_options(
    -fexceptions
    -frtti
    -Wall
    -Wextra
    -Wpedantic
    -O3
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
)

# 查找 Android log 库
find_library(log-lib log)

# 查找 neuralnetworks 库 (NNAPI)
find_library(nnapi-lib neuralnetworks)

# 添加 MNN 预编译库（如果存在）
set(MNN_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/../../prebuilt/mnn/include")
set(MNN_LIB_DIR "${CMAKE_SOURCE_DIR}/../../prebuilt/mnn/lib")

if(EXISTS ${MNN_INCLUDE_DIR})
    include_directories(${MNN_INCLUDE_DIR})
    message(STATUS "MNN include found: ${MNN_INCLUDE_DIR}")
endif()

if(EXISTS ${MNN_LIB_DIR})
    message(STATUS "MNN lib found: ${MNN_LIB_DIR}")
endif()

# 添加 LocalDream 头文件目录（仅供引用，不编译）
set(LOCALDREAM_INCLUDE "${CMAKE_SOURCE_DIR}/localdream")
if(EXISTS ${LOCALDREAM_INCLUDE})
    include_directories(${LOCALDREAM_INCLUDE})
    message(STATUS "LocalDream headers: ${LOCALDREAM_INCLUDE}")
endif()

# 源文件列表 - 快绘AI核心引擎
set(NATIVE_SOURCES
    native_engine.cpp
    native_model.cpp
    native_utils.cpp
    scheduler.cpp
    tokenizer.cpp
    jni_bridge.cpp
)

# 创建共享库
add_library(kuaihui_native SHARED ${NATIVE_SOURCES})

# 链接库
target_link_libraries(kuaihui_native
    ${log-lib}
    android
    jnigraphics
    OpenSLES
    android
)

# 链接 NNAPI
if(nnapi-lib)
    target_link_libraries(kuaihui_native ${nnapi-lib})
    message(STATUS "NNAPI library linked")
else()
    message(WARNING "NNAPI not found")
endif()

# 链接 MNN（如果存在）
if(EXISTS ${MNN_LIB_DIR})
    if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
        if(EXISTS ${MNN_LIB_DIR}/arm64-v8a/libMNN.so)
            target_link_libraries(kuaihui_native ${MNN_LIB_DIR}/arm64-v8a/libMNN.so)
        endif()
    endif()
endif()

# 设置输出
set_target_properties(kuaihui_native PROPERTIES
    OUTPUT_NAME "kuaihui_native"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}"
)

# 编译定义
target_compile_definitions(kuaihui_native PRIVATE 
    KUAIHUI_VERSION=2
    SUPPORT_MNN=1
    SUPPORT_NNAPI=1
    LOCALDREAM_INTEGRATED=1
)

message(STATUS "=== KuaiHui Native Build ===")
message(STATUS "Version: 2.0.1 - Full LocalDream Integration")
message(STATUS "Features: MNN, NNAPI, LoRA, ControlNet, SDXL Ready")
