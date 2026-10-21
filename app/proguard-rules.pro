# ProGuard & R8 Optimization Rules for Dot Widgets

# Retain line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retain Room database classes, entities, and DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Retain AppWidget providers and RemoteViews receivers
-keep class com.example.widgets.base.DotWidgetProvider** { *; }
-keep class com.example.widgets.base.WidgetUpdateReceiver { *; }
-keep class com.example.widgets.collection.CollectionWidgetService { *; }
-keep class com.example.data.media.MediaListenerService { *; }

# Retain Data models and JSON configurations
-keepclassmembers class com.example.data.db.** { *; }
-keepclassmembers class com.example.widgets.base.** { *; }

# Retain Kotlin Reflection / Coroutines annotations
-keepclassmembers class * {
    @kotlin.jvm.JvmField <fields>;
}
