# WebView JS bridge 보존
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.myhub.app.** { *; }
