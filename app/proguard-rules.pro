# NewPipe Extractor (reflexão interna e Rhino)
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn javax.annotation.**

# youtubedl-android
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.youtubedl_common.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# Commons Compress: o ZipUtils do youtubedl-android extrai o Python com ele, e
# ExtraFieldUtils instancia os ZipExtraField por reflexão no <clinit>. Sem isto
# o R8 torna AsiExtraField não-concreta e o init quebra com
# "class ... is not a concrete class".
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
