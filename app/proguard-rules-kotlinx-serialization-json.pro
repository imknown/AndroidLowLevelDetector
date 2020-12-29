-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Change here com.yourcompany.yourpackage
-keep,includedescriptorclasses class net.imknown.android.forefrontinfo.**$$serializer { *; }
-keepclassmembers class net.imknown.android.forefrontinfo.** {
    *** Companion;
}
-keepclasseswithmembers class net.imknown.android.forefrontinfo.** {
    kotlinx.serialization.KSerializer serializer(...);
}