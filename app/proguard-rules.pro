# Android instantiates these components by their manifest class names. Keep their constructors
# and callbacks under R8 full mode. The AdMob and UMP AARs contribute their own consumer rules.
-keep,allowoptimization class com.rotationcontrol.force.service.RotationForegroundService { *; }
-keep,allowoptimization class com.rotationcontrol.force.accessibility.RotationAccessibilityService { *; }
-keep,allowoptimization class com.rotationcontrol.force.receiver.BootCompletedReceiver { *; }
-keep,allowoptimization class com.rotationcontrol.force.RotationControlApp { *; }
-keep,allowoptimization class com.rotationcontrol.force.ui.MainActivity { *; }

# Preserve metadata used by Android framework annotations and library reflection.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# WindowManager is an Android platform API, not an app class packaged in this APK; R8 cannot
# remove or obfuscate platform framework classes. No reflective WindowManager lookup is used.
