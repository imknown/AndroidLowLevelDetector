https://source.android.com/
https://developer.android.com/about/versions/10

=========

adb shell uname -a
uname -m
uname -r

=========

adb shell getprop ro.boot.verifiedbootstate
adb shell getprop ro.boot.flash.locked
adb shell getprop ro.oem_unlock_supported

=========

https://chromium.googlesource.com/aosp/platform/system/core/+/upstream/shell_and_utilities/

=========

http://landley.net/toybox/index.html
http://landley.net/toybox/bin/
https://github.com/landley/toybox/releases
https://android.googlesource.com/platform/external/toybox/+/refs/heads/master/www/news.html

toolbox toybox busybox
adb shell toybox --version

https://busybox.net/downloads/binaries/

=========

adb shell getforce

root: /sys/fc/selinux/policyvers
8+: /etc/selinux/plat_sepolicy_vers.txt
https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/Android.bp#15
https://android.googlesource.com/platform/system/sepolicy/+/refs/heads/master/policy_version.mk#4

=========

ro.config.low_ram

=========

https://android.googlesource.com/kernel/common/+refs

=========
