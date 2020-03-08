# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.
cmake_minimum_required(VERSION 3.4.1)

set(nativeLibName "BinderDetector")

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.
add_library(
        ${nativeLibName}
        SHARED
        src/main/cpp/BinderDetector.cpp
)

find_library(log-lib log)

target_link_libraries(${nativeLibName} ${log-lib})