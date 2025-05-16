#include <jni.h>
#include <cerrno>
#include <android/log.h>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <sys/ioctl.h>
#include <fcntl.h>

struct binder_version {
    __s32 protocol_version;
};