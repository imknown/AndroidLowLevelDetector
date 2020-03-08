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
// https://android.googlesource.com/platform/external/linux-kselftest/+/refs/heads/master/tools/testing/selftests/filesystems/binderfs/binderfs_test.c#135
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

extern "C" JNIEXPORT jint JNICALL
Java_net_imknown_android_forefrontinfo_ui_home_HomeViewModel_getBinderVersion(
        JNIEnv *env,
        jobject instance,
        jstring driver
) {
    int binderVersion = -1;

    const char *driverChars = env->GetStringUTFChars(driver, nullptr);
    int fd = open(driverChars, O_RDONLY | O_CLOEXEC);
    if (fd < 0) {
        int errorNo = errno;

        char buf[256];
        snprintf(buf, sizeof buf, "%s%s%s%d%s%d%s%s",
                 "Driver: ", driverChars,
                 ", fd: ", fd,
                 ", errorNo: ", errorNo,
                 ", error: ", strerror(errorNo));
        __android_log_print(ANDROID_LOG_WARN, "BinderDetector open", buf, nullptr);

        env->ReleaseStringUTFChars(driver, driverChars);
        close(fd);

        return -abs(errorNo);
    }

    int version = _IOWR('b', 9, struct binder_version);
    int ret = ioctl(fd, version, &binderVersion);

    if (ret < 0) {
        int errorNo = errno;

        char buf[256];
        snprintf(buf, sizeof buf, "%s%s%s%d%s%d%s%s",
                 "Driver: ", driverChars,
                 ", ret: ", ret,
                 ", errorNo: ", errorNo,
                 ", error: ", strerror(errorNo));
        __android_log_print(ANDROID_LOG_WARN, "BinderDetector ioctl", buf, nullptr);

        binderVersion = -abs(errorNo);
    }

    env->ReleaseStringUTFChars(driver, driverChars);
    close(fd);

    return binderVersion;
}