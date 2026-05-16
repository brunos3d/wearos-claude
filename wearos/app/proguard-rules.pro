# Keep Kotlin serialization metadata.
-keepclasseswithmembers class * { @kotlinx.serialization.Serializable <methods>; }
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class * { *** Companion; }
-keepclasseswithmembers class * { kotlinx.serialization.KSerializer serializer(...); }

# Wear OS service entry points discovered by manifest.
-keep class com.brunos3d.wearosclaude.tile.UsageTileService { *; }
-keep class com.brunos3d.wearosclaude.complication.UsageComplicationService { *; }
