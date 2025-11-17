# Slider-Odos3D (Android)

## Toolchain (fallback/offline)
- **Android Gradle Plugin** 8.5.2 · **Kotlin** 1.9.23 · **Gradle** 8.7 · **JDK** 17
- **compileSdk / targetSdk** 35 · **minSdk** 24
- CameraX 1.3.4 · WorkManager 2.9.1 · FFmpegKit Full 6.0-2.LTS

### Build local
```bash
./gradlew :app:assembleDebug
```
> En contenedores sin acceso a internet puede fallar la descarga de plugins; en GitHub Actions se compila y publica el APK automáticamente.

## Permisos
- Bluetooth clásico (Android ≤ 30): `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION`.
- Android 12L/13+: `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (runtime).
- `CAMERA` + almacenamiento moderno (MediaStore) para capturas/exportaciones.
- `FileProvider`: `${applicationId}.provider` limitado a `cache/logs/` y `cache/exports/`.

## Flujo principal
1. **Ajustes** → guarda nombre/MAC del dispositivo, `pollHz`, paso y feed por defecto, y comparte logs desde cache.
2. **Conexión/Manual** → selecciona dispositivo (ConnectActivity), valida permisos, auto-restablece el chip de estado y ejecuta jog seguro con cooldown y `Cancelar Jog`.
3. **Escenas & Cámara/Timelapse** → 10 presets con auto-start; el slider avanza con $J=G21 G91…F… entre fotos (límites y eje desde Ajustes) mientras CameraX guarda en MediaStore.
4. **Galería/Export** → selección de fotos y render MP4 con WorkManager + FFmpegKit.

## CI (GitHub Actions)
Workflow [`android.yml`](.github/workflows/android.yml) ejecuta `:app:assembleDebug` en cada push a `main` (o desde *Run workflow*) y publica el APK de debug como artefacto `app-debug`.
