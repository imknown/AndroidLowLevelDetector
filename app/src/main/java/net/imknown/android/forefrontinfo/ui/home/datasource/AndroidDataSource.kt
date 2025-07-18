package net.imknown.android.forefrontinfo.ui.home.datasource

class AndroidDataSource {
    companion object {
        // region [BuildId]
        const val BUILD_ID_SEPARATOR = '.'

        const val PROP_RO_SYSTEM_BUILD_ID = "ro.system.build.id"
        const val PROP_RO_VENDOR_BUILD_ID = "ro.vendor.build.id"
        const val PROP_RO_ODM_BUILD_ID = "ro.odm.build.id"
        // endregion [BuildId]

        // region [Security Patch]
        const val PROP_SECURITY_PATCH = "ro.build.version.security_patch"

        const val PROP_VENDOR_SECURITY_PATCH = "ro.vendor.build.security_patch"

        const val SYSTEM_PROPERTY_LINUX_VERSION = "os.version"
        // endregion [Security Patch]

        // region [A/B]
        // https://source.android.com/devices/tech/ota/ab?hl=en
        // /* root needed */ private const val CMD_BOOT_PARTITION = "ls /dev/block/bootdevice/by-name | grep boot_"
        const val PROP_AB_UPDATE = "ro.build.ab_update"
        const val PROP_VIRTUAL_AB_ENABLED = "ro.virtual_ab.enabled"
        const val PROP_VIRTUAL_AB_RETROFIT = "ro.virtual_ab.retrofit"
        // const val PROP_VIRTUAL_AB_ALLOW_NON_AB = "ro.virtual_ab.allow_non_ab"
        // const val PROP_VIRTUAL_AB_COMPRESSION_ENABLED = "ro.virtual_ab.compression.enabled"
        // const val PROP_VIRTUAL_AB_IO_URING_ENABLED = "ro.virtual_ab.io_uring.enabled"
        // const val PROP_VIRTUAL_AB_COMPRESSION_XOR_ENABLED = "ro.virtual_ab.compression.xor.enabled"
        // const val PROP_VIRTUAL_AB_USERSPACE_SNAPSHOTS_ENABLED = "ro.virtual_ab.userspace.snapshots.enabled"
        const val PROP_SLOT_SUFFIX = "ro.boot.slot_suffix"
        // endregion [A/B]

        // region [SAR]
        // https://source.android.com/devices/bootloader/system-as-root?hl=en
        //
        // https://twitter.com/topjohnwu/status/1174392824625676288
        // https://github.com/topjohnwu/Magisk/blob/master/docs/boot.md
        // https://github.com/topjohnwu/Magisk/blob/master/scripts/util_functions.sh#L239
        // https://github.com/opengapps/opengapps/blob/master/scripts/templates/installer.sh#L1032
        //
        // https://github.com/penn5/TrebleCheck/blob/master/app/src/main/java/tk/hack5/treblecheck/MountDetector.kt
        // https://github.com/kevintresuelo/treble/blob/master/app/src/main/java/com/kevintresuelo/treble/checker/SystemAsRoot.kt
        const val PROP_SYSTEM_ROOT_IMAGE = "ro.build.system_root_image"
        // endregion [SAR]

        // region [Dynamic Partitions]
        // https://source.android.com/devices/tech/ota/dynamic_partitions/ab_legacy?hl=en
        // https://source.android.com/devices/tech/ota/dynamic_partitions/ab_launch?hl=en
        // https://codelabs.developers.google.com/codelabs/using-Android-GSI?hl=en
        const val PROP_DYNAMIC_PARTITIONS = "ro.boot.dynamic_partitions"
        const val PROP_DYNAMIC_PARTITIONS_RETROFIT = "ro.boot.dynamic_partitions_retrofit"
        // /* root needed */ const val CMD_LL_DEV_BLOCK_SUPER = "ls -l /dev/block/by-name/super"
        // endregion [Dynamic Partitions]

        // region [Treble]
        // https://source.android.com/devices/architecture?hl=en#hidl
        // https://source.android.com/devices/architecture/vintf/objects#device-manifest-file
        // https://source.android.com/compatibility/vts/hal-testability
        // https://android.googlesource.com/platform/cts/+/master/hostsidetests/security/src/android/security/cts/SELinuxHostTest.java#268
        // https://android.googlesource.com/platform/system/libvintf/+/master/VintfObject.cpp#286
        // https://android.googlesource.com/platform/system/libvintf/+/master/VintfObject.cpp#313
        const val PROP_TREBLE_ENABLED = "ro.treble.enabled"
        const val PROP_VENDOR_SKU = "ro.boot.product.vendor.sku"
        const val PATH_VENDOR_VINTF_SKU = "/vendor/etc/vintf/manifest_%s.xml"
        const val PATH_VENDOR_VINTF = "/vendor/etc/vintf/manifest.xml"
        const val PATH_VENDOR_VINTF_FRAGMENTS = "/vendor/etc/vintf/manifest/"
        const val PATH_VENDOR_LEGACY_NO_FRAGMENTS = "/vendor/manifest.xml"
        // endregion [Treble]

        // region [GSI]
        // https://codelabs.developers.google.com/codelabs/using-Android-GSI?hl=en#2
        // https://developer.android.google.cn/topic/generic-system-image?hl=en
        // https://source.android.com/setup/build/gsi?hl=en
        // https://source.android.com/devices/architecture/vndk/linker-namespace?hl=en
        const val LD_CONFIG_FILE_ANDROID_11 = "/linkerconfig/ld.config.txt"
        const val LD_CONFIG_FILE_ANDROID_9 = "/system/etc/ld.config*.txt"
        const val CMD_VENDOR_NAMESPACE_DEFAULT_ISOLATED = "cat %s | grep -A 20 '\\[vendor\\]' | grep namespace.default.isolated"
        // endregion [GSI]

        // region [Dynamic System]
        // https://developer.android.com/topic/dsu?hl=en
        const val PROP_PERSIST_DYNAMIC_SYSTEM_UPDATE = "persist.sys.fflag.override.settings_dynamic_system"
        const val PROP_DYNAMIC_SYSTEM_UPDATE = "sys.fflag.override.settings_dynamic_system"
        // endregion [Dynamic System]

        // region [VNDK]
        // https://source.android.com/devices/architecture/vndk?hl=en
        const val PROP_VNDK_LITE = "ro.vndk.lite"
        const val PROP_VNDK_VERSION = "ro.vndk.version"
        // const val PROP_VENDOR_VNDK_VERSION = "ro.vendor.vndk.version"
        // const val PROP_PRODUCT_VNDK_VERSION = "ro.product.vndk.version"
        // endregion [VNDK]

        // region [APEX]
        // https://source.android.com/devices/tech/ota/apex?hl=en
        const val PROP_APEX_UPDATABLE = "ro.apex.updatable"
        // endregion [APEX]

        // region [Settings]
        const val SETTINGS_DISABLED = 0
        const val SETTINGS_ENABLED = 1

        // https://android.googlesource.com/platform/bootable/recovery/+/master/README.md#shows-the-device_but-in-state
        const val PROP_ADB_SECURE = "ro.adb.secure"
        // endregion [Settings]

        // region [Encryption]
        // https://source.android.com/security/encryption/full-disk
        // https://source.android.com/security/encryption/file-based
        // const val PROP_CRYPTO_STATE = "ro.crypto.state"
        // endregion [Encryption]

        // region [SELinux]
        // https://source.android.com/security/selinux
        // https://android.googlesource.com/platform/external/selinux/+/master/libsepol/include/sepol/policydb/policydb.h#745
        // https://github.com/torvalds/linux/blob/master/security/selinux/include/security.h#L43
        // const val SELINUX_MOUNT = "/sys/fs/selinux"
        // const val CMD_SELINUX_POLICY_VERSION = "cat $SELINUX_MOUNT/policyvers"
        // const val PROP_BOOT_SELINUX = "ro.boot.selinux"
        const val CMD_GETENFORCE = "getenforce"
        const val CMD_ERROR_PERMISSION_DENIED = "Permission denied"
        const val SELINUX_STATUS_DISABLED = "Disabled"
        const val SELINUX_STATUS_PERMISSIVE = "Permissive"
        const val SELINUX_STATUS_ENFORCING = "Enforcing"
        // endregion [SELinux]

        // region [Toybox]
        const val CMD_TOYBOX_VERSION = "toybox --version"
        // endregion [Toybox]

        // region [WebView]
        // Need `android.permission.DUMP`
        // const val CMD_DUMPSYS_WEBVIEWUPDATE = "dumpsys webviewupdate"
        // const val CMD_DUMPSYS_WEBVIEWUPDATE = "cmd webviewupdate dump"
        //
        // https://chromium.googlesource.com/chromium/src/+/main/android_webview/docs/webview-providers.md#webview-provider-options
        // https://chromium.googlesource.com/chromium/src/+/main/android_webview/docs/webview-providers.md#package-name
        // https://chromium.googlesource.com/chromium/src/+/main/android_webview/docs/aosp-system-integration.md#configuring-the-android-framework
        // endregion [WebView]

        // region [OutdatedTargetSdkVersion]
        const val PROP_RO_PRODUCT_FIRST_API_LEVEL = "ro.product.first_api_level"
        // endregion [OutdatedTargetSdkVersion]
    }
}