
```
adb shell getprop ro.boot.verifiedbootstate
adb shell getprop ro.boot.flash.locked
adb shell getprop ro.oem_unlock_supported
```

------

busybox (https://busybox.net/downloads/binaries/)

------

```
adb shell getforce
```

- root: `/sys/fc/selinux/policyvers`
- 8+: `/etc/selinux/plat_sepolicy_vers.txt`

https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/Android.bp#15
https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/policy_version.mk#4

------

```
ro.config.low_ram
```
