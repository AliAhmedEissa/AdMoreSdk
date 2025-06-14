## Consumer ProGuard Rules for AdMore SDK
#
## Keep AdMore SDK public API - these rules will be applied to the host app
#-keep public class com.seamlabs.admore.sdk.AdMoreSDK {
#    public *;
#}
#
#-keep public interface com.seamlabs.admore.sdk.presentation.callback.* {
#    *;
#}
#
## Keep callback implementations in host app
#-keepclassmembers class * implements com.seamlabs.admore.sdk.presentation.callback.InitCallback {
#    public *;
#}
#
#-keepclassmembers class * implements com.seamlabs.admore.sdk.presentation.callback.EventCallback {
#    public *;
#}
#
## Keep Koin modules if host app uses Koin
#-keep class com.seamlabs.admore.sdk.di.** { *; }
#
## Prevent obfuscation of SDK initialization
#-keepclassmembers class * {
#    *** initialize(...);
#    *** sendEvent(...);
#    *** isInitialized(...);
#}
#
## Keep enum values that might be used by host app
#-keepclassmembers enum com.seamlabs.admore.sdk.domain.model.Permission {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}