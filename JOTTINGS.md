# JOTTINGS
Minds

## Features todo

- JetPack
  - Architecture (MVVM, Lifecycle, ViewModel, LiveData, Navigation, etc.)
  - Animation & transitions
- detect dm-verity (version)
- detect FBE/FDE
- detect HAL
- Android version market share
- OpenGL ES/Vulkan/GPU Driver version
- icu (https://developer.android.com/guide/topics/resources/internationalization)
- kotlinPoet, googleAutoService, custom commands (V2.0 ?)
- settings
  - root mode
  - show commands
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
https://android.googlesource.com/platform/external/avb/+/refs/heads/master/avbtool
https://android.googlesource.com/platform/external/avb/+/refs/heads/master/libavb/avb_version.h

``` sh
adb shell "grep dm- /proc/mounts"

adb shell getprop ro.boot.avb_version # avbtool version
adb shell getprop ro.boot.veritymode
adb shell getprop ro.boot.verifiedbootstate
adb shell getprop ro.boot.flash.locked
adb shell getprop ro.boot.secureboot
adb shell getprop ro.oem_unlock_supported

adb shell ro.boot.vbmeta.avb_version # required (lib)avb version
adb shell ro.boot.vbmeta.device_state

adb shell ro.crypto.state
adb shell ro.build.selinux
adb shell ro.secure
```

## Settings

- https://developer.android.com/studio/command-line/adb#shellcommands
- https://stackoverflow.com/questions/40624222/how-does-adb-shell-getprop-and-setprop-work

- https://developer.android.com/reference/android/provider/Settings.System
- https://developer.android.com/reference/android/provider/Settings.Global
- https://developer.android.com/reference/android/provider/Settings.Secure

``` sh
adb shell settings list system
adb shell settings list global
adb shell settings list secure
```

``` sh
# adb root
# adb shell cat /data/property/persistent_properties
```

## busybox

busybox (https://busybox.net/downloads/binaries/)

## selinux

``` sh
adb shell getenforce
```

- root: `/sys/fc/selinux/policyvers`
- 8+: `/etc/selinux/plat_sepolicy_vers.txt`

https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/Android.bp#15
https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/policy_version.mk#4
