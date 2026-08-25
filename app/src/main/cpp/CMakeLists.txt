cmake_minimum_required(VERSION 3.22.1)

project("whisper-jni")

# Sätt C++17 explicit för Whisper C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Arkitekturspecifika kompileringsflaggor (isolerar ARM-instruktioner från x86_64)
if(ANDROID_ABI STREQUAL "arm64-v8a")
    # Hårdvaruoptimering för Moto G24 (Helio G85 / Cortex-A55)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -mcpu=cortex-a55 -march=armv8-a+simd -flto")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -mcpu=cortex-a55 -march=armv8-a+simd -flto")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -mfpu=neon -flto")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -mfpu=neon -flto")
else()
    # Säker reserv för x86_64 (Android Emulator)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3")
endif()

# Inkludera Whisper-headerrötter
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/whisper)

# Bygg det delade biblioteket
add_library(
        whisper-jni
        SHARED
        whisper-jni.cpp
        whisper/whisper.cpp
        whisper/ggml.c
        whisper/ggml-alloc.c
        whisper/ggml-backend.c
        whisper/ggml-quants.c
)

# Hitta och länka Android-systembibliotek
find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
        whisper-jni
        ${log-lib}
        ${android-lib}
)