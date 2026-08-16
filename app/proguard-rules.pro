# SQLCipher loads its native library and classes reflectively.
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Retrofit relies on generic signatures at runtime (R8 full mode strips them otherwise).
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp optional platform integrations that are not on the classpath.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Tink (via androidx.security:security-crypto) references optional annotations.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
