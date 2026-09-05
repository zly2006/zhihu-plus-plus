#include "libkn_api.h"
#include "napi/native_api.h"
#include "hilog/log.h"
#include <rawfile/raw_file_manager.h>
#include <dlfcn.h>
#include <string>

// 避免工程侧未定义 LOG_DOMAIN 时编译失败
#ifndef LOG_DOMAIN
#define LOG_DOMAIN 0x0000
#endif

static napi_value MainArkUIViewController(napi_env env, napi_callback_info info) {
    return reinterpret_cast<napi_value>(MainArkUIViewController(env));
}

static std::string ReadStringArgument(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value argv[1] = {nullptr};
    if (napi_get_cb_info(env, info, &argc, argv, nullptr, nullptr) != napi_ok || argc == 0) {
        return {};
    }
    size_t length = 0;
    // ArkTS limits bodies to 2 MiB and images to 5 MiB (about 7 MiB in Base64).
    if (napi_get_value_string_utf8(env, argv[0], nullptr, 0, &length) != napi_ok || length > 8 * 1024 * 1024) {
        return {};
    }
    std::string value(length + 1, '\0');
    if (napi_get_value_string_utf8(env, argv[0], &value[0], value.size(), &length) != napi_ok) {
        return {};
    }
    value.resize(length);
    return value;
}

static napi_value ApplyHomeJson(napi_env env, napi_callback_info info) {
    const std::string value = ReadStringArgument(env, info);
    P2ApplyHomeJson(const_cast<char*>(value.c_str()));
    return nullptr;
}

static napi_value ApplyDetailJson(napi_env env, napi_callback_info info) {
    const std::string value = ReadStringArgument(env, info);
    P2ApplyDetailJson(const_cast<char*>(value.c_str()));
    return nullptr;
}

static napi_value ApplyImageBase64(napi_env env, napi_callback_info info) {
    const std::string value = ReadStringArgument(env, info);
    P2ApplyImageBase64(const_cast<char*>(value.c_str()));
    return nullptr;
}

static napi_value ApplySessionStatus(napi_env env, napi_callback_info info) {
    const std::string value = ReadStringArgument(env, info);
    P2ApplySessionStatus(const_cast<char*>(value.c_str()));
    return nullptr;
}

static napi_value ApplyError(napi_env env, napi_callback_info info) {
    const std::string value = ReadStringArgument(env, info);
    P2ApplyError(const_cast<char*>(value.c_str()));
    return nullptr;
}

static napi_value UsesNativeNetwork(napi_env env, napi_callback_info info) {
    napi_value result;
    napi_get_boolean(env, P2UsesNativeNetwork(), &result);
    return result;
}

static napi_value HandleBack(napi_env env, napi_callback_info info) {
    napi_value result;
    napi_get_boolean(env, P1HandleBack(), &result);
    return result;
}

static napi_value ApplyColorMode(napi_env env, napi_callback_info info) {
    const std::string value = ReadStringArgument(env, info);
    P1ApplyColorMode(const_cast<char*>(value.c_str()));
    return nullptr;
}

static napi_value SetDatabasePath(napi_env env, napi_callback_info info) {
    const std::string value = ReadStringArgument(env, info);
    P3SetDatabasePath(const_cast<char*>(value.c_str()));
    return nullptr;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    androidx_compose_ui_arkui_init(env, exports);
    napi_property_descriptor desc[] = {
        {"MainArkUIViewController", nullptr, MainArkUIViewController, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"applyHomeJson", nullptr, ApplyHomeJson, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"applyDetailJson", nullptr, ApplyDetailJson, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"applyImageBase64", nullptr, ApplyImageBase64, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"applySessionStatus", nullptr, ApplySessionStatus, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"applyError", nullptr, ApplyError, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"applyColorMode", nullptr, ApplyColorMode, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"setDatabasePath", nullptr, SetDatabasePath, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"usesNativeNetwork", nullptr, UsesNativeNetwork, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"handleBack", nullptr, HandleBack, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    return exports;
}
EXTERN_C_END

static napi_module demoModule = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "entry",
    .nm_priv = ((void*)0),
    .reserved = { 0 },
};

extern "C" __attribute__((constructor)) void RegisterEntryModule(void)
{
    napi_module_register(&demoModule);
}
