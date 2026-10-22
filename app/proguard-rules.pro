# ============================================================================
# Reglas de Ofuscación, Seguridad y Optimización (ProGuard / R8)
# Proyecto: IA Student (Orbe)
# ============================================================================

# 1. Conservar nombres de archivo fuente y tablas de líneas para análisis seguro de crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. Conservar modelos de datos y serialización JSON (Gemini y arquitectura interna)
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.api.** { *; }
-keep class com.example.data.api.ApiTestResult { *; }

# 3. Conservar interfaces y clases de seguridad y permisos
-keep class com.example.security.** { *; }
-keepclassmembers class com.example.security.** { *; }
-keep class com.example.data.permission.** { *; }
-keepclassmembers class com.example.data.permission.** { *; }

# 4. Eliminar llamadas de registro de logs sensibles en builds de Release (Previene filtración de datos)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# 5. Compatibilidad con Jetpack Compose y Kotlin Coroutines
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 6. Descartar metadata sensible de depuración en clases compiladas
-dontwarn kotlin.reflect.**
-dontwarn javax.sound.sampled.**
-dontwarn java.awt.**
