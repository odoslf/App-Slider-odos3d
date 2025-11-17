# Guía de toolchain offline

## Pasos rápidos
- Validar entorno: `scripts/check_env.sh`
- Intentar assemble o registrar estado: `scripts/assemble_or_note.sh` (escribe `build/status/assemble.txt`)

## Problemas comunes
- **AGP no encontrado** → Ejecuta con red una vez (local o CI) para popular caché antes de forzar builds offline.
- **Permisos Android 12L/13+** → Concede `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` y `CAMERA` cuando la app los solicite.
- **FileProvider** → Autoridad `${applicationId}.provider` usando rutas `cache/logs/` y `cache/exports/`.

## Matriz del fallback
- AGP 8.5.2 · Kotlin 1.9.23 · Gradle 8.14.x · JDK 17
- SDK: compile/target 35 · min 24

## Actualizar cuando haya red
1. Edita `gradle.properties` y sube `agpVersion` / `kotlinVersion`.
2. Sincroniza Gradle y ejecuta `./gradlew :app:assembleDebug` con acceso a `google()` y `mavenCentral()`.
