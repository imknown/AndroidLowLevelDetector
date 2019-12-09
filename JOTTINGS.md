
------

https://android.googlesource.com/platform/external/

------

https://android.googlesource.com/platform/external/avb/+/refs/tags/android-10.0.0_r17/avbtool
https://android.googlesource.com/platform/external/avb/+/refs/tags/android-10.0.0_r17/libavb/avb_version.h

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
```

------


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

------

busybox (https://busybox.net/downloads/binaries/)

------

``` sh
adb shell getforce
```

- root: `/sys/fc/selinux/policyvers`
- 8+: `/etc/selinux/plat_sepolicy_vers.txt`

https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/Android.bp#15
https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/policy_version.mk#4

------

``` sh
adb shell ro.config.low_ram
```
