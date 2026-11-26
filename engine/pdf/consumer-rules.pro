# Consumer rules for :engine:pdf (PdfiumAndroid, PDFBox-Android, BouncyCastle)
-keep class io.legere.pdfiumandroid.** { *; }
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn com.gemalto.jp2.**
-dontwarn java.awt.**
-dontwarn javax.swing.**
