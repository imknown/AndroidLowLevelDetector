# https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports-new-sdk?platform=android

# To preserve the information Crashlytics requires for producing readable crash reports, add the following lines to your Proguard or Dexguard config file:
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.

# If you want a faster, obfuscated build with ProGuard, exclude Crashlytics by adding the following lines to your ProGuard config file:
# -keep class com.google.firebase.crashlytics.** { *; }
# -dontwarn com.google.firebase.crashlytics.**