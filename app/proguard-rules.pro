# Evora Office R8 / ProGuard keep rules
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepattributes Signature,InnerClasses,EnclosingMethod
