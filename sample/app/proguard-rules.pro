# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/cwen/workspace/android/ide/android-studio/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontpreverify
-repackageclasses ''
-allowaccessmodification
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-dontwarn com.google.ads.**
-dontwarn **CompatHoneycomb
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keepclasseswithmembers class * {
    public <init>(android.app.Activity, int);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * implements android.os.Parcelable {
    static android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-keep public class com.ibluetag.indoor.R$*{
    public static final int *;
}

-keep public class com.ibluetag.sdk.beacon.R$*{
    public static final int *;
}

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-dontwarn android.support.v4.**
-keep class android.support.v4.** { *; }
-keep public class * extends android.support.v4.**
-keep public class * extends android.app.Fragment

-keep class com.baidu.** { *; }
-keep class vi.com.gdi.bgl.android.** {*;}
-keep class com.sun.** {*;}
-keep class com.umeng.** {*;}
-keep class com.google.appengine.** {*;}
-keep class java.awt.** {*;}
-keep class javax.activation.** {*;}
-keep class org.apache.** {*;}
-keep class retrofit.** {*;}
-keep class com.google.gson.** {*;}
-keep class rx.** {*;}
-keep class android.support.v4.** { *; }
-keep class com.ibluetag.sdk.** { *; }
-keep class com.cc.maps.** { *; }
-keep class com.tencent.** { *; }
-keep class com.sina.weibo.** { *; }


# already obscure
-dontwarn com.ibluetag.**
-dontwarn com.baidu.**
-dontwarn com.squareup.**
-dontwarn com.sun.**
-dontwarn com.google.appengine.**
-dontwarn java.awt.**
-dontwarn javax.activation.**
-dontwarn org.apache.**

-keepclassmembers class * {
   public <init>(org.json.JSONObject);
}
