# Guía de Contribución (Contributing Guidelines)

¡Gracias por tu interés en contribuir a **IA Student (Orbe)**! Apreciamos enormemente las mejoras, correcciones de errores, traducciones y nuevas funcionalidades.

---

## 🧭 Flujo de Trabajo (Fork y Pull Request)

1. **Fork del Repositorio:**
   Haz un fork del repositorio en tu cuenta personal de GitHub o GitLab.

2. **Crear una Rama Descriptiva:**
   Crea una rama para tu característica o corrección a partir de `main` (o la rama activa):
   ```bash
   git checkout -b feature/nueva-funcionalidad
   # o
   git checkout -b fix/correccion-de-error
   ```

3. **Hacer Cambios y Probar:**
   - Asegúrate de seguir las directrices de estilo de código (Kotlin oficial y Jetpack Compose / M3).
   - Verifica que el proyecto compila sin errores y que las pruebas pasan localmente.

4. **Hacer Commit de tus Cambios:**
   Sigue las reglas de commit descritas a continuación.

5. **Enviar un Pull Request (PR):**
   - Abre un Pull Request contra la rama principal.
   - Describe con claridad el problema resuelto o la funcionalidad añadida.
   - Vincula issues relacionados si existen.

---

## 📝 Convenciones de Commit (Conventional Commits)

Recomendamos y solicitamos el uso de la especificación **[Conventional Commits](https://www.conventionalcommits.org/)**:

```
<tipo>(<ámbito opcional>): <descripción concisa en imperativo>

[cuerpo explicativo opcional]

[pie de página con referencias a issues opcional]
```

### Tipos Permitidos:
- `feat`: Nueva funcionalidad (ej. `feat(shared): soporte para Compose Desktop`)
- `fix`: Corrección de un fallo (ej. `fix(orb): optimizar cálculo de partículas en pantallas de alta resolución`)
- `docs`: Cambios en la documentación (ej. `docs: actualizar README y licencias`)
- `style`: Formateo de código sin cambios funcionales (espacios, comas, etc.)
- `refactor`: Reestructuración de código sin alterar su comportamiento
- `test`: Añadir o corregir pruebas unitarias o de integración
- `chore`: Mantenimiento de scripts de compilación, Gradle o dependencias

---

## 🧪 Cómo Ejecutar las Pruebas

Antes de enviar cualquier PR, valida que todos los tests pasen exitosamente:

### Pruebas Unitarias de Android (`:app`):
```bash
./gradlew :app:testDebugUnitTest
```

### Compilación y Verificación de Desktop / Multiplataforma (`:shared`):
```bash
./gradlew :shared:build
```

### Verificación Global del Repositorio:
```bash
./gradlew build test --no-daemon
```

---

## 🛡️ Reglas Importantes de Seguridad
- **Bajo ninguna circunstancia incluyas claves de API (`GEMINI_API_KEY`), credenciales de Firebase o secretos personales en tus commits.**
- Mantén el archivo `.env` fuera del seguimiento de Git.
