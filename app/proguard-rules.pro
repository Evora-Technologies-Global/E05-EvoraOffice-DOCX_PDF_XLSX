# Evora Office R8 / ProGuard keep & release hardening rules
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Strip verbose, debug, and info logs from release bytecode (keep Log.w/Log.e for non-fatal error triage)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# PdfiumAndroid JNI & API models
-keep class io.legere.pdfiumandroid.** { *; }

# Hangul HWP / HWPX parsers
-keep class kr.dogfoot.hwplib.** { *; }
-keep class kr.dogfoot.hwpxlib.** { *; }

# Apache Commons Compress & XZ
-keep class org.apache.commons.compress.** { *; }
-keep class org.tukaani.xz.** { *; }

# PDFBox-Android & BouncyCastle
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ML Kit & CameraX
-keep class com.google.mlkit.** { *; }
-keep class androidx.camera.** { *; }

# LibreOfficeKit JNI callbacks & classes
-keep class com.evora.technologies.office.engine.office.** { *; }
-keep class org.libreoffice.kit.** { *; }
-keepclassmembers class org.libreoffice.kit.** { *; }
-keep class org.libreoffice.android.** { *; }

# Cloud Drives (Google Drive REST v3 + MSAL + Microsoft Graph v6)
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-keep class com.microsoft.identity.** { *; }
-keep class com.microsoft.kiota.** { *; }
-dontwarn org.apache.http.**
-dontwarn com.google.api.client.http.apache.**

# Ktor CIO LAN Transfer
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# Google Play Billing 8.0.0 & Monetization
-keep class com.android.billingclient.api.** { *; }
-keep class com.evora.technologies.office.engine.monetization.** { *; }
