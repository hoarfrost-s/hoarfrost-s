# =====================================================
# HyperFetchMobile ProGuard 配置文件
# 用于代码混淆、优化和压缩
# =====================================================

# ----------------------
# 基本配置
# ----------------------

# 优化次数，默认 5 次
-optimizationpasses 5

# 混淆时不使用大小写混合类名
-dontusemixedcaseclassnames

# 不忽略库中的非公开类
-dontskipnonpubliclibraryclasses

# 混淆时预校验，加快启动速度
-dontpreverify

# 详细日志输出
-verbose

# 优化配置
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# 保留注解
-keepattributes *Annotation*

# 保留泛型签名
-keepattributes Signature

# 保留源文件名和行号信息（用于调试）
-keepattributes SourceFile,LineNumberTable

# ----------------------
# Android 组件保留规则
# ----------------------

# 保留所有 Activity
-keep public class * extends android.app.Activity
-keep public class * extends android.app.ActivityGroup

# 保留所有 Service
-keep public class * extends android.app.Service

# 保留所有 BroadcastReceiver
-keep public class * extends android.content.BroadcastReceiver

# 保留所有 ContentProvider
-keep public class * extends android.content.ContentProvider

# 保留所有 Application
-keep public class * extends android.app.Application

# 保留所有 Fragment
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment

# 保留所有 View
-keep public class * extends android.view.View

# 保留自定义 View 的构造函数
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保留 Parcelable 实现类
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 Serializable 实现类
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ----------------------
# 数据模型类保留规则
# ----------------------

# 保留所有数据模型类（用于 JSON 序列化）
-keep class com.hyperfetch.model.** { *; }

# 保留所有实体类
-keep class com.hyperfetch.data.entity.** { *; }

# ----------------------
# Retrofit 保留规则
# ----------------------

# 保留 Retrofit 接口方法
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# 保留 Retrofit 服务接口
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# 保留 OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ----------------------
# Gson 保留规则
# ----------------------

# 保留 Gson 相关类
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# 保留 Gson 实例化对象
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ----------------------
# Room 数据库保留规则
# ----------------------

# 保留 Room 生成的类
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# 保留 Room DAO 接口
-keep interface * extends androidx.room.Dao
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static <methods>;
}

# ----------------------
# Hilt 依赖注入保留规则
# ----------------------

# 保留 Hilt 生成的类
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# 保留 Hilt 模块
-keep @dagger.hilt.InstallIn class *
-keep @dagger.hilt.android.AndroidEntryPoint class *

# ----------------------
# Kotlin 协程保留规则
# ----------------------

# 保留协程相关类
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ----------------------
# WorkManager 保留规则
# ----------------------

# 保留 WorkManager Worker 类
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker

# ----------------------
# WebView JavaScript 接口保留规则
# ----------------------

# 保留 JavaScript 接口方法
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ----------------------
# 自定义保留规则
# ----------------------

# 保留下载引擎相关类
-keep class com.hyperfetch.engine.** { *; }
-keep class com.hyperfetch.protocol.** { *; }

# 保留下载回调接口
-keep interface com.hyperfetch.engine.DownloadCallback { *; }
-keep interface com.hyperfetch.engine.DownloadListener { *; }

# 保留通知相关类
-keep class com.hyperfetch.notification.** { *; }

# ----------------------
# 警告抑制
# ----------------------

# 忽略警告
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ----------------------
# 移除日志（发布版本）
# ----------------------

# 移除所有日志输出（可选，根据需要启用）
# -assumenosideeffects class android.util.Log {
#     public static boolean isLoggable(java.lang.String, int);
#     public static int v(...);
#     public static int i(...);
#     public static int w(...);
#     public static int d(...);
#     public static int e(...);
# }