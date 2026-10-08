-keep class com.partner.studyreminder.sync.SyncWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
-keep class com.partner.studyreminder.alarm.AlarmReceiver { *; }
-keep class com.partner.studyreminder.alarm.BootReceiver { *; }
-keep class com.partner.studyreminder.alarm.AlarmActionReceiver { *; }
-keep class com.partner.studyreminder.alarm.RingingService { *; }
-keep class com.kyant.backdrop.** { *; }
-keep class com.kyant.shapes.** { *; }
-dontwarn android.graphics.RuntimeShader

# Room 2.5 loads WorkDatabase_Impl with Class.newInstance(). R8 full mode
# (AGP 9) drops that constructor unless it is named here, and the app then
# crashes in WorkManagerInitializer before any screen opens.
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.migration.AutoMigrationSpec {
    <init>();
}
