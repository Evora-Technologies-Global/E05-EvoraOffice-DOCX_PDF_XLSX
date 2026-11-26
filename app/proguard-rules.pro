# Evora Office R8 / ProGuard keep rules
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepattributes Signature,InnerClasses,EnclosingMethod

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

# Google Play Billing 8.0.0 & Monetization
-keep class com.android.billingclient.api.** { *; }
-keep class com.evora.technologies.office.engine.monetization.** { *; }
