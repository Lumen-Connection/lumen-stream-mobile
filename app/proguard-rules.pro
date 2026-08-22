# NewPipe Extractor (reflexão interna e Rhino)
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn javax.annotation.**

# youtubedl-android
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.youtubedl_common.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
