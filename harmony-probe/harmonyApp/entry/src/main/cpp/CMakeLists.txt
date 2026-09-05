# the minimum version of CMake.
cmake_minimum_required(VERSION 3.5.0)
project(harmonyApp)

set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})
set(MODULE_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR}/../../..)

if(DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif()

# 按 ABI 选 libkn.so / libkn_api.h（须与 libs/<abi>/libkn.so 来自同一次 K/N link）
if(CMAKE_OHOS_ARCH_ABI STREQUAL "x86_64" OR OHOS_ARCH STREQUAL "x86_64")
    set(LIBKN_ABI_DIR "x86_64")
else()
    set(LIBKN_ABI_DIR "arm64-v8a")
endif()

include_directories(${NATIVERENDER_ROOT_PATH}
                    ${NATIVERENDER_ROOT_PATH}/include
                    ${NATIVERENDER_ROOT_PATH}/include/${LIBKN_ABI_DIR})

set(COMPOSE_MODULE_PATH ${MODULE_ROOT_PATH}/oh_modules/@cpf-kmp-cmp/compose)
list(APPEND CMAKE_PREFIX_PATH ${COMPOSE_MODULE_PATH})
find_package(compose REQUIRED CONFIG)

add_library(entry SHARED napi_init.cpp)

# Link against ComposeApp shared library to resolve exported symbols
#add_library(ComposeApp SHARED IMPORTED)
target_link_libraries(entry PUBLIC libace_napi.z.so)
target_link_libraries(entry PUBLIC libhilog_ndk.z.so)
target_link_libraries(entry PUBLIC libdeviceinfo_ndk.z.so)
target_link_libraries(entry PUBLIC librawfile.z.so)
#target_link_libraries(entry PUBLIC libicu.so)

target_link_libraries(entry PUBLIC ${NATIVERENDER_ROOT_PATH}/../../../libs/${LIBKN_ABI_DIR}/libkn.so)
# 先链接 libkn.so，再链接 compose里面的skikobridge
target_link_libraries(entry PUBLIC compose::skikobridge)
target_link_libraries(entry PUBLIC ${EGL-lib} ${GLES-lib} ${hilog-lib} ${libace-lib} ${libnapi-lib} ${libuv-lib} libc++_shared.so)
