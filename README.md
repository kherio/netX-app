# NetAlertX Viewer

App Android nativa (Kotlin + Jetpack Compose) de solo lectura para ver lo que muestra un servidor [NetAlertX](https://github.com/netalertx/NetAlertX):

- **Resumen**: dispositivos, conectados, caídos, nuevos, favoritos y archivados.
- **Dispositivos**: lista con búsqueda (nombre, IP, MAC, fabricante) y filtros.
- **Eventos**: últimas 72 h (conexiones, desconexiones, dispositivos nuevos…).
- **Notificaciones**: aviso en segundo plano cuando aparece un dispositivo nuevo o cae uno (WorkManager, mínimo cada 15 min).

## Configuración

En **Ajustes** de la app:

1. **URL de la API**: la dirección donde responde la API REST de NetAlertX (puerto `20212` por defecto, o el dominio HTTPS que el administrador haya puesto delante con un proxy inverso). No es la URL de la web (`20211`) salvo que el proxy reenvíe también las rutas de la API.
2. **API_TOKEN**: el valor del ajuste `API_TOKEN` de NetAlertX (Ajustes → System en su web). Hay que pedírselo a quien administra el servidor.
3. Pulsa **Probar conexión** y luego **Guardar**.

La app solo hace peticiones `GET` (`/devices`, `/devices/totals`, `/events/72`). El token se guarda en el almacenamiento privado de la app.

## Compilar

El workflow `.github/workflows/build.yml` compila el APK debug en cada push (artefacto `NetAlertXViewer-debug` en la pestaña Actions). Si creas una etiqueta `v*` (por ejemplo `git tag v1.0.0 && git push origin v1.0.0`), además publica el APK en Releases. En local: abrir en Android Studio, o `gradle assembleDebug` con Gradle 8.10+ y JDK 17.
