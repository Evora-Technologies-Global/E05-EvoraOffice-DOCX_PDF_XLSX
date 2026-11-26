# Google Drive REST v3, MSAL, and Microsoft Graph SDK keep rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-keep class com.google.gson.** { *; }
-keep class com.microsoft.identity.** { *; }
-keep class com.microsoft.kiota.** { *; }
-keep class com.microsoft.graph.models.Drive { *; }
-keep class com.microsoft.graph.models.DriveItem { *; }
-keep class com.microsoft.graph.models.DriveItemCollectionResponse { *; }
-keep class com.microsoft.graph.models.DriveItemUploadableProperties { *; }
-keep class com.microsoft.graph.models.Folder { *; }
-keep class com.microsoft.graph.models.File { *; }
-keep class com.microsoft.graph.models.ItemReference { *; }
-keep class com.microsoft.graph.models.UploadSession { *; }
-keep class com.microsoft.graph.drives.item.items.item.createuploadsession.** { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn javax.annotation.**
-dontwarn org.apache.http.**
-dontwarn com.google.api.client.http.apache.**
-dontwarn com.google.crypto.tink.subtle.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn net.jcip.annotations.**
-dontwarn io.micrometer.context.**
-dontwarn reactor.blockhound.integration.**
