// Duplicated: https://juejin.cn/post/7472717843571621922

import kotlinx.cinterop.*
import platform.android.*
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName(externName="Java_com_wj_mylibrary_NativeLib_getStringFromNative")
fun getStringFromNative(env: CPointer<JNIEnvVar>, thiz: jobject): jstring {
    memScoped {
        return env.pointed.pointed!!.NewStringUTF!!.invoke(
            env, "Hello from Kotlin/Native!".cstr.ptr
        )!!
    }
}

// Dynamic register
fun sayHello() {
    __android_log_print(ANDROID_LOG_ERROR.toInt(), "Kotlin", "Hello %s", "Kotlin Native!")
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName(externName="JNI_OnLoad")
fun JNI_OnLoad(vm: CPointer<JavaVMVar>, preserved: COpaquePointer): jint {
    return memScoped {
        val envStorage = alloc<CPointerVar<JNIEnvVar>>()
        val vmValue = vm.pointed.pointed!!
        val result = vmValue.GetEnv!!(vm, envStorage.ptr.reinterpret(), JNI_VERSION_1_6)
        if (result == JNI_OK) {
            val env = envStorage.pointed!!.pointed!!
            val jclass = env.FindClass!!(envStorage.value, "com/wj/mylibrary/NativeLib".cstr.ptr)
            val jniMethod = allocArray<JNINativeMethod>(1)
            jniMethod[0].fnPtr = staticCFunction(::sayHello)
            jniMethod[0].name = "sayHello".cstr.ptr
            jniMethod[0].signature = "()V".cstr.ptr
            env.RegisterNatives!!(envStorage.value, jclass, jniMethod, 1)
        }
        JNI_VERSION_1_6
    }
}

fun main() {
    println("Hello from Kotlin/Native")
}

fun nativeFunction(): String {
    return "Hello from Kotlin/Native"
}
