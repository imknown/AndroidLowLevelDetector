import kotlinx.cinterop.*
import platform.android.*
import platform.posix.*
import kotlin.math.abs
import net.imknown.android.forefrontinfo.binderdetector.*

fun release(driverChars: String, fd: Int) {
    close(fd)
}

@OptIn(ExperimentalForeignApi::class)
fun getErrorNo(driverChars: String, ret: Int, function: String, fd: Int): Int {
    __android_log_print(
        ANDROID_LOG_WARN.toInt(),
        "BinderDetector",
        "%s: Driver: %s, ret: %d, errorNo: %d, error: %s",
        function, driverChars, ret, errno, strerror(errno)
    )

    val errorNo = -abs(errno)

    release(driverChars, fd)

    return errorNo
}

@OptIn(ExperimentalForeignApi::class)
fun getBinderVersion(driver: String): Int {
    val driverChars = driver.encodeToByteArray().decodeToString()
    val fd = open(driverChars, O_RDONLY or O_CLOEXEC)
    if (fd < 0) {
        return getErrorNo(driverChars, fd, "open", fd)
    }

    memScoped {
        val binderVersion = alloc<IntVar>()
        val version: Int = _IOWR('b'.code, 9, sizeOf<binder_version>())

        ioctl(fd, 1, 1)
        val ioctlRet = ioctl(fd, 9, binderVersion.ptr)

        if (ioctlRet < 0) {
            return getErrorNo(driverChars, ioctlRet, "ioctl", fd)
        }

        release(driverChars, fd)

        return binderVersion.value
    }
}