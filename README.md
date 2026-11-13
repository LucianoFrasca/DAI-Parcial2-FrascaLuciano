# Jornadas del Sur · Guardia de mantenimiento

Quedan dos horas para entregar una corrección de la agenda de las jornadas. La versión instalada ya tiene usuarios con charlas guardadas. La organización publica instantáneas completas del programa. Una respuesta exitosa puede contener cero charlas. Los identificadores de las charlas son estables aunque cambie el título.

## Preparación previa

Abrí esta carpeta en Android Studio con JDK 17, Android SDK 34 y un emulador de API 26 o superior. Sincronizá las dependencias con Internet antes de clase. Ejecutá app o compilá con ./gradlew assembleDebug (gradlew.bat assembleDebug en Windows). El APK queda en app/build/outputs/apk/debug/. Al actualizar una instalación, conservá los datos del usuario.

Mesa controla el servicio local que usa Retrofit. Cada solicitud captura disponibilidad y edición al comenzar. Para provocar una falla, apagá el servicio antes de iniciar la petición. El contador incluye intentos fallidos y vuelve a cero al morir el proceso. La demora normal es 1,4 segundos y la larga es 8. En v1 hay doce charlas. En v2 la charla 11 cambia de título y sala. En v3 se retira la 11 e ingresa la 90. En v4 el programa está vacío.

## Condiciones de entrega

| Pedido | Comportamiento observable |
| --- | --- |
| 41 · Programa sin señal | Con una edición previa, una falla conserva lista, detalle y favoritas e informa que se están usando datos guardados. Sin descarga previa debe ofrecer reintento. Una v4 exitosa muestra «sin charlas programadas» y sigue siendo una edición válida al reabrir sin señal. |
| 42 · Agenda personal | Al pasar de v1 a v2, la charla 11 conserva su marca pero muestra «Compose en producción avanzada» y «Sala Puerto». En v3 desaparece la 11 del catálogo y de Mi agenda, entra la 90 y se conservan las marcas de las charlas vigentes. Si una charla se retira y después regresa, regresa sin marcar. Una marca agregada o quitada en una charla vigente durante una petición debe respetarse cuando llega la respuesta. |
| 43 · Volver al punto de lectura | Elegí Día 2 y Datos, abrí «Cachés para viajeros», volvé y rotá. Deben quedar dos resultados de Datos. Repetí dejando la ficha abierta y mandando la app al fondo. Si Android recupera la tarea tras matar su proceso, debe recuperar la ficha y, al volver, ambos filtros. Una tarea nueva arranca sin filtros. |
| 44 · Control de descargas | Una carga automática por sesión (proceso) y una por actualización explícita. Rotar, entrar a una ficha o cambiar de pestaña agrega cero. Dos toques durante una petición agregan un intento. Tras éxito o falla se puede volver a pedir. Los filtros siguen siendo editables durante la espera. |

Para el caso de proceso de 43, presioná Inicio, ejecutá `adb shell am kill ar.jornadas` y regresá desde Recientes. Para un arranque vacío sin señal, ejecutá `adb shell pm clear ar.jornadas` y luego `adb shell am start -n ar.jornadas/.MainActivity --ez servicio_disponible false`. El primer comando borra los datos de prueba.
