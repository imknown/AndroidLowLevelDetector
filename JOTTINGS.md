- https://developer.android.com/about/versions/10
- https://source.android.com/
- https://source.android.com/security/enhancements/enhancements9
- https://source.android.com/setup/start/p-release-notes
 -https://source.android.com/setup/start/build-numbers

---

```
adb shell getprop ro.boot.verifiedbootstate
adb shell getprop ro.boot.flash.locked
adb shell getprop ro.oem_unlock_supported
```

---

https://chromium.googlesource.com/aosp/platform/system/core/+/upstream/shell_and_utilities/

---

busybox

https://busybox.net/downloads/binaries/

---

```
adb shell getforce
```

- root: `/sys/fc/selinux/policyvers`
- 8+: `/etc/selinux/plat_sepolicy_vers.txt`

https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/Android.bp#15
https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/policy_version.mk#4

---

```
ro.config.low_ram
```
