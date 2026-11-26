# Google Drive REST v3, MSAL, and Microsoft Graph SDK keep rules
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.microsoft.identity.** { *; }
-keep class com.microsoft.graph.** { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn javax.annotation.**
