# ==========================================
# Base directives
# ==========================================
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose

# Preserve annotations, generic signatures, and line numbers for crash debugging
-keepattributes *Annotation*,InnerClasses,EnclosingMethod
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable


# ==========================================
# Android core components
# ==========================================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

# Keep only View constructors invoked by the framework; do not keep whole class names
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Activity layout onClick methods
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# Preserve R resources
-keep class **.R$* { public static <fields>; }

# JNI / native methods
-keepclasseswithmembernames class * { native <methods>; }
-keepclassmembers class * {
    native <methods>;
}

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Serializable
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Event callbacks
-keepclassmembers class * {
    void *(**On*Event);
    void *(**On*Listener);
}


# ==========================================
# AndroidX (only suppress warnings; AARs include their own ProGuard rules)
# ==========================================
-dontwarn androidx.**
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

# ViewBinding
-keep class ** implements androidx.viewbinding.ViewBinding {
    public static ** inflate(android.view.LayoutInflater);
    public android.view.View getRoot();
}

# Fragment
-keepclassmembers class androidx.fragment.app.Fragment {
    androidx.fragment.app.FragmentActivity getActivity();
}

# Classes annotated with @Keep
-keep @androidx.annotation.Keep class * { *; }


# ==========================================
# Third-party libraries
# ==========================================

## Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.internal.firebase.analytics.** { *; }

## Retrofit
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations

## OkHttp / Okio
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Media3
-keep class androidx.media3.** { *; }

# Tencent Sonic
-keep class com.tencent.sonic.** { *; }
-dontwarn com.tencent.sonic.**

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# AppsFlyer
-keep class com.appsflyer.** { *; }
-keep class kotlin.jvm.internal.** { *; }

# Google Install Referrer
-keep public class com.android.installreferrer.** { *; }
-dontwarn com.android.installreferrer

# HJQ Permissions
-keepclassmembers interface com.hjq.permissions.start.IStartActivityDelegate { <methods>; }
-keepclassmembers interface com.hjq.permissions.fragment.IFragmentMethodNative { <methods>; }

# DfSDK
-keep class com.dfsdk.** { *; }
-dontwarn com.dfsdk.**
-keep class com.liveness.dflivenesslibrary.** { *; }

# Moat / IAB
-keep class com.moat.** { *; }
-dontwarn com.moat.**
-keep class com.iab.** { *; }
-dontwarn com.iab.**

# Apache Commons Compress
-keep class org.apache.commons.compress.archivers.zip.** { *; }

# Project data classes
-keep class com.vaycore.finance.data.local.bean.** { *; }
-keep class com.vaycore.finance.data.network.** { *; }

# AlertDialog (accessed through reflection)
-keepclassmembers class androidx.appcompat.app.AlertDialog$Builder {
    <init>(android.content.Context, int);
    public android.content.Context getContext();
    public androidx.appcompat.app.AlertDialog$Builder setTitle(java.lang.CharSequence);
    public androidx.appcompat.app.AlertDialog$Builder setView(android.view.View);
    public androidx.appcompat.app.AlertDialog$Builder setPositiveButton(int, android.content.DialogInterface$OnClickListener);
    public androidx.appcompat.app.AlertDialog$Builder setNegativeButton(int, android.content.DialogInterface$OnClickListener);
    public androidx.appcompat.app.AlertDialog create();
}

# Dialog subclasses
-keep class * extends android.app.Dialog { *; }
-keep class * extends androidx.appcompat.app.AppCompatDialog { *; }

# Checker Framework / Kotlin / Javax annotations (build warnings)
-dontwarn org.checkerframework.**
-dontwarn kotlin.annotations.jvm.**
-dontwarn javax.annotation.**
