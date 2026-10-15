# Avisos de Terceros y Licencias de Dependencias (Third-Party Notices)

Este proyecto utiliza librerías, dependencias de código abierto y recursos de terceros bajo sus respectivas licencias. Este documento proporciona un registro inicial de atribución para los componentes detectados en los módulos `:app` y `:shared`.

> ℹ️ **Nota de Referencia Inicial:** Esta lista es una relación inicial de las principales dependencias utilizadas. Se recomienda generar el reporte exhaustivo y detallado de dependencias del árbol de compilación ejecutando el script `scripts/generate_dependencies_report.sh` (el cual utiliza `./gradlew :app:dependencies --configuration releaseRuntimeClasspath > deps.txt`).

---

## 📦 Lista Inicial de Dependencias Detectadas

### 1. AndroidX & Jetpack Compose
- **Componentes:**
  - `androidx.core:core-ktx`
  - `androidx.activity:activity-compose`
  - `androidx.lifecycle:lifecycle-runtime-ktx`
  - `androidx.lifecycle:lifecycle-viewmodel-compose`
  - `androidx.lifecycle:lifecycle-runtime-compose`
  - `androidx.compose.ui:*`
  - `androidx.compose.material3:*`
  - `androidx.compose.material:material-icons-extended`
- **Licencia:** Apache License 2.0
- **Copyright:** The Android Open Source Project

### 2. Room Persistence Library
- **Componentes:**
  - `androidx.room:room-runtime`
  - `androidx.room:room-ktx`
  - `androidx.room:room-compiler`
- **Licencia:** Apache License 2.0
- **Copyright:** The Android Open Source Project

### 3. Google Firebase SDK
- **Componentes:**
  - `com.google.firebase:firebase-bom`
  - `com.google.firebase:firebase-ai`
  - `com.google.firebase:firebase-appcheck-recaptcha`
  - `com.google.firebase:firebase-appcheck-debug`
  - `com.google.firebase:firebase-firestore` (opcional)
  - `com.google.firebase:firebase-auth` (opcional)
- **Licencia:** Apache License 2.0
- **Copyright:** Google LLC

### 4. Retrofit & Moshi Converter
- **Componentes:**
  - `com.squareup.retrofit2:retrofit`
  - `com.squareup.retrofit2:converter-moshi`
- **Licencia:** Apache License 2.0
- **Copyright:** Square, Inc.

### 5. OkHttp & Logging Interceptor
- **Componentes:**
  - `com.squareup.okhttp3:okhttp`
  - `com.squareup.okhttp3:logging-interceptor`
- **Licencia:** Apache License 2.0
- **Copyright:** Square, Inc.

### 6. Moshi
- **Componentes:**
  - `com.squareup.moshi:moshi-kotlin`
  - `com.squareup.moshi:moshi-kotlin-codegen`
- **Licencia:** Apache License 2.0
- **Copyright:** Square, Inc.

### 7. Coil (Coroutine Image Loader)
- **Componentes:**
  - `io.coil-kt:coil-compose`
- **Licencia:** Apache License 2.0
- **Copyright:** Coil Contributors

### 8. Kotlin Coroutines & Kotlin Multiplatform
- **Componentes:**
  - `org.jetbrains.kotlinx:kotlinx-coroutines-core`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-test`
- **Licencia:** Apache License 2.0
- **Copyright:** JetBrains s.r.o.

### 9. Compose Multiplatform / Desktop
- **Componentes:**
  - `org.jetbrains.compose:*`
- **Licencia:** Apache License 2.0
- **Copyright:** JetBrains s.r.o.

### 10. Robolectric & Testing
- **Componentes:**
  - `org.robolectric:robolectric` (Apache License 2.0)
  - `junit:junit` (Eclipse Public License 1.0)
  - `androidx.test:*` (Apache License 2.0)

---

## 🎨 Recursos Gráficos y Multimedia (Assets)

Los archivos en `app/src/main/res/` incluyen:
1. **Imágenes y Texturas:** `app/src/main/res/drawable/` (`quantum_cyan_orb_1791993174579.jpg`, `ia_student_icon_1791930959879.jpg`). Creados con herramientas generativas para el proyecto.
2. **Iconos Adaptativos:** `app/src/main/res/mipmap-*/` e iconos vectoriales Material Symbols (Apache 2.0).
3. **Muestras Multimedia (`app/src/main/res/raw/`):**
   - `hello.wav`: Efecto de audio breve para saludo y síntesis de voz (Dominio público / Muestra Creative Commons 0).
   - `videoplayback.mp4`: Clip de muestra para pruebas del laboratorio de video (Creative Commons Attribution / Dominio Público).
   - **Nota de revisión:** Si se publican o redistribuyen clips comerciales en producción, se recomienda verificar que cuenten con licencia explícita de redistribución libre de regalías.
