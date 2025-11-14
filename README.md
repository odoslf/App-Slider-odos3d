# App-Slider-odos3d

Aplicación Android para controlar el slider Odos3D mediante Bluetooth y automatizar secuencias timelapse.

## Características
- Control básico y avanzado del slider enviando comandos G-code.
- Gestión de conexión Bluetooth SPP usando el UUID `00001101-0000-1000-8000-00805F9B34FB`.
- Captura de timelapse con CameraX y exportación a MP4 mediante FFmpegKit.
- Ajustes persistentes con DataStore (MAC por defecto, intervalo, velocidad).

## Requisitos
- Android 8.0 (API 26) o superior.
- Permisos: cámara, audio, notificaciones, acceso a medios e interfaces Bluetooth.
- Dispositivo emparejado previamente por Bluetooth clásico (SPP).

## Uso rápido
1. Empareja el slider Odos3D desde los ajustes Bluetooth del dispositivo.
2. Abre la app y utiliza la pestaña **Bluetooth** para conectar usando la MAC emparejada.
3. Configura controles básicos en **Home** o envía G-code en **Avanzado**.
4. En **Timelapse** define intervalo, pasos y velocidad; ejecuta y luego crea un MP4 desde las fotos.
5. Ajusta valores por defecto en **Ajustes**, que se guardan con DataStore.

## Construcción
```bash
./gradlew assembleDebug
```
