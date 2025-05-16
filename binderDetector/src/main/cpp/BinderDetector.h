#ifndef BINDER_DETECTOR_H
#define BINDER_DETECTOR_H

#include <jni.h>
#include <errno.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <fcntl.h>

struct binder_version {
    __s32 protocol_version;
};

#endif
