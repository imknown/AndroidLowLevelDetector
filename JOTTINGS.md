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
https://android.googlesource.com/platform/external/avb/+/refs/heads/master/avbtool
https://android.googlesource.com/platform/external/avb/+/refs/heads/master/libavb/avb_version.h

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

adb shell getprop ro.build.selinux
adb shell getprop ro.boot.selinux
```

- root: `/sys/fc/selinux/policyvers`
- 8+: `/etc/selinux/plat_sepolicy_vers.txt`

https://source.android.com/security/selinux
https://github.com/torvalds/linux/blob/master/security/selinux/include/security.h
https://cs.android.com/search?q=SEPOL_POLICY_KERN
https://cs.android.com/search?q=POLICYDB_VERSION_MAX
https://cs.android.com/search?q=VERSION_MAX
https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/Android.bp#15
https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/policy_version.mk#4
