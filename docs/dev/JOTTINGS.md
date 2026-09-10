# JOTTINGS
Minds

## Features todo

- JetPack
  - UseCase
  - Navigation
  - Hilt
  - Startup
  - Compose
  - Transitions
- detect dm-verity (version)
- detect FBE/FDE
- detect HAL
- Android version market share
- OpenGL ES/Vulkan/GPU Driver version
- icu (https://developer.android.com/guide/topics/resources/internationalization)
- unicode
- custom commands, result filters, scroll top, scrollbar (V2.0 ?)
- settings
  - root mode
  - show commands
  - developer mode
  - network cache logic
  - select json server
- documents
  - development references
  - user-readable explanation why mine not supported
- copy result
- acknowledge
- etc.

## external

https://android.googlesource.com/platform/external/

## avb

https://source.android.com/security/verifiedboot/avb
https://source.android.com/security/verifiedboot
https://android.googlesource.com/platform/external/avb/+/master/README.md
https://android.googlesource.com/platform/external/avb/+/master/avbtool
https://android.googlesource.com/platform/external/avb/+/master/libavb/avb_version.h

``` sh
adb shell "grep dm- /proc/mounts"

adb shell getprop ro.boot.avb_version # avbtool version
adb shell getprop ro.boot.veritymode
adb shell getprop ro.boot.verifiedbootstate
adb shell getprop ro.boot.flash.locked

adb shell getprop ro.boot.secureboot # MIUI only?
adb shell getprop ro.secure # Secure boot
adb shell getprop ro.debuggable
# * eng builds: ro.secure=0 and ro.debuggable=1
# * userdebug builds: ro.secure=1 and ro.debuggable=1
# * user builds: ro.secure=1 and ro.debuggable=0

adb shell getprop ro.oem_unlock_supported

adb shell getprop ro.boot.vbmeta.avb_version # required (lib)avb version
adb shell getprop ro.boot.vbmeta.device_state
```

## busybox

busybox (https://busybox.net/downloads/binaries/)
