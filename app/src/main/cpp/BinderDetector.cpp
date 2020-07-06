// region [Comments]
// region [Read file, but root needed]
// https://android.googlesource.com/platform/test/vts/+/master/utils/python/controllers/android_device.py#726
// https://android.googlesource.com/platform/test/vts-testcase/kernel/+/master/api/binder/VtsKernelBinderTest.py
// endregion [Read file, but root needed]

// region [Native]
// "/dev/binder"
// "/dev/binderfs"
// "/dev/binderfs/my-binder"
// "/dev/binderfs/binder-control"
// "/dev/hwbinder"
// "/dev/vndbinder"
//
// https://android.googlesource.com/platform/frameworks/native/+/master/libs/binder/ProcessState.cpp#349
// https://android.googlesource.com/platform/system/libhwbinder/+/master/ProcessState.cpp#406
// https://android.googlesource.com/platform/frameworks/native/+/master/libs/binder/tests/binderDriverInterfaceTest.cpp
// https://android.googlesource.com/platform/external/linux-kselftest/+/master/tools/testing/selftests/filesystems/binderfs/binderfs_test.c#135
//
// https://android.googlesource.com/platform/external/kernel-headers/+/master/original/uapi/linux/android/binder.h#102
// https://android.googlesource.com/platform/bionic/+/master/libc/kernel/uapi/linux/android/binder.h#98
//
// https://android.googlesource.com/platform/external/kernel-headers/+/master/original/uapi/linux/android/binder.h#114
// endregion [Native]

// region [Doc]
// https://source.android.com/setup/build/gsi#legacy-gsi-build-targets
// https://source.android.com/setup/build/gsi#gsi-build-targets
// endregion [Doc]
// endregion [Comments]

#include "BinderDetector.h"

void release(JNIEnv *env, jstring driver,
             const char *driverChars, int fd) {
    env->ReleaseStringUTFChars(driver, driverChars);
    close(fd);
}

int getErrorNo(JNIEnv *env, jstring driver,
               int ret, const char *function,
               const char *driverChars, int fd) {
    __android_log_print(
            ANDROID_LOG_WARN,
            "BinderDetector",
            "%s: Driver: %s, ret: %d, errorNo: %d, error: %s",
            function, driverChars, ret, errno, strerror(errno)
    );

    int errorNo = -abs(errno);

    release(env, driver, driverChars, fd);

    return errorNo;
}

extern "C" JNIEXPORT jint JNICALL
Java_net_imknown_android_forefrontinfo_ui_others_OthersViewModel_getBinderVersion(
        JNIEnv *env,
        jobject instance,
        jstring driver
) {
    const char *driverChars = env->GetStringUTFChars(driver, nullptr);
    int fd = open(driverChars, O_RDONLY | O_CLOEXEC);
    if (fd < 0) {
        return getErrorNo(env, driver, fd, "open", driverChars, fd);
    }

    int version = _IOWR('b', 9, struct binder_version);
    int binderVersion = -1;
    int ioctlRet = ioctl(fd, version, &binderVersion);
    if (ioctlRet < 0) {
        return getErrorNo(env, driver, ioctlRet, "ioctl", driverChars, fd);
    }

    release(env, driver, driverChars, fd);

    return binderVersion;
}