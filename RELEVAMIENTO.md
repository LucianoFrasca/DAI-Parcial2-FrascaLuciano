# Relevamiento y plan — Jornadas del Sur (pedidos 41 a 44)

> Documento previo a cualquier cambio. Todavía no se modificó código.

## 1. Qué es el proyecto

App Android (Kotlin + **Jetpack Compose** + Material3), `minSdk 26`, `targetSdk 34`, `versionCode 12`.
Una sola Activity, navegación "a mano" con `rememberSaveable` (sección + id de la ficha abierta).

```
MainActivity ─► Portada (Scaffold + NavigationBar: Programa / Mi agenda / Mesa)
                  ├─ Catalogo (filtros día/tema + Grilla)   ui/Catalogo.kt
                  ├─ Mi agenda (Grilla de favoritas)         ui/Portada.kt
                  ├─ Ficha de detalle (busca por id)         ui/Portada.kt
                  └─ Mesa (panel del servicio de prueba)     ui/Mesa.kt
ProgramaModel (AndroidViewModel)                             ui/ProgramaModel.kt
   └─ Servicios (arma Room + Retrofit)                       Servicios.kt
        └─ AgendaRepository(api, dao)                        data/AgendaRepository.kt
             ├─ ProgramaApi (Retrofit, GET programa)         data/Programa.kt
             │    └─ ServicioLocal (Interceptor que simula el server con assets/programa-N.json)
             └─ CarteleraDao (Room, tabla charlas, columna favorita)  data/Programa.kt
```

- `lab/ServicioLocal.kt` y `ui/Mesa.kt`: herramienta de prueba de la cátedra (contador de peticiones, v1..v4, apagar servicio, demora 8 s). **No se tocan** salvo cómo se instancia.
- `legacy/AgendaImpresa.kt`: código muerto (solo un `@Preview`). **No se toca**.
- No hay tests ni carpeta `res/`. La carpeta `Jornadas` no tiene repo git propio (el git detectado es el del Escritorio, todo sin trackear) → conviene hacer una copia/ZIP del original antes de empezar.

### Datos de prueba (assets)
| Edición | Contenido |
|---|---|
| v1 | 12 charlas (ids 11-14 Android, 21-24 Datos, 31-34 Calidad; días 1 y 2) |
| v2 | igual, pero la 11 pasa a «Compose en producción avanzada» / «Sala Puerto» |
| v3 | sin la 11, entra la 90 («Clínica de trazas») |
| v4 | `[]` (programa vacío, respuesta **exitosa**) |

Día 2 + Datos = charlas 23 («Cachés para viajeros») y 24 → los «dos resultados» del pedido 43.

---

## 2. Diagnóstico por pedido

### 41 · Programa sin señal
| Qué pide | Qué pasa hoy | Causa |
|---|---|---|
| Con edición previa, una falla conserva lista, detalle y favoritas y avisa que se usan datos guardados | Si falla, **desaparece la lista** y solo se ve "No se pudo consultar el programa" + Reintentar | `Catalogo.kt:27-32`: `if (aviso.isNotEmpty())` reemplaza la Grilla por el error |
| Sin descarga previa → ofrecer reintento | Funciona de casualidad (lista vacía + aviso) | No se distingue "nunca descargué" de "descargué una edición vacía" |
| v4 exitosa → «sin charlas programadas» | Se siguen viendo las charlas viejas | `AgendaRepository.sincronizar()` hace `dao.guardar([])`: un insert vacío no borra nada. Tampoco existe el texto de vacío |
| v4 sigue siendo edición válida al reabrir sin señal | Con 0 filas no hay forma de saber que hubo descarga → se trataría como "sin datos" | No se persiste ningún registro de "hay una edición descargada" |
| (extra) | Solo se captura `IOException`; un error de parseo/HTTP crashea | `ProgramaModel.kt:25` |

### 42 · Agenda personal
| Qué pide | Qué pasa hoy | Causa |
|---|---|---|
| v1→v2: la 11 conserva la marca con título y sala nuevos | **Se pierden todas las marcas en cada sincronización** | `@Insert(REPLACE)` reescribe la fila completa con `favorita = false` (valor por defecto del mapeo en `AgendaRepository.kt:8`) |
| v3: la 11 desaparece de catálogo y Mi agenda, entra la 90 | La 11 queda para siempre | Nunca se borran las charlas que ya no vienen en la instantánea |
| Si una charla se retira y vuelve, vuelve sin marcar | — | Depende de borrar la fila (y su marca) al retirarse |
| Marca agregada/quitada **durante** la petición se respeta | Se pisa con la respuesta | Mismo `REPLACE`; la respuesta no sabe de la marca nueva |

### 43 · Volver al punto de lectura
| Qué pide | Qué pasa hoy | Causa |
|---|---|---|
| Día 2 + Datos → abrir ficha → volver → rotar: siguen 2 resultados | Se pierde el filtro **Tema** al abrir la ficha y al rotar | `Catalogo.kt:18`: `tema` es `remember` local; el Catalogo sale de composición al abrir la ficha |
| Tras matar el proceso: recuperar ficha y, al volver, ambos filtros | Ficha sí (`rememberSaveable`), filtros no | `dia` vive en un `MutableStateFlow` del ViewModel (no sobrevive a la muerte del proceso); `tema` ni siquiera sobrevive a rotar |
| Tarea nueva arranca sin filtros | OK | — (no hay que persistir en disco) |
| (arquitectura) | La vista filtra la lista y escribe `model.dia.value` directo | Estado mutable expuesto + lógica de filtrado en el Composable |

### 44 · Control de descargas
| Qué pide | Qué pasa hoy | Causa |
|---|---|---|
| 1 carga automática por proceso | Al abrir la app ya hay **2** peticiones | `MainActivity.onStart()` llama `recargar()` **y** `Catalogo` también (`LaunchedEffect(Unit)`) |
| Rotar / entrar a una ficha / cambiar de pestaña = 0 | Cada una suma | `onStart` corre en cada rotación y al volver del fondo; `LaunchedEffect` corre cada vez que el Catalogo vuelve a componerse |
| 2 toques durante una petición = 1 intento | Cada toque lanza otra corrutina | `recargar()` no controla si ya hay una en curso |
| Tras éxito o falla se puede volver a pedir | OK | — |
| Filtros editables durante la espera | OK hoy; hay que no romperlo | — |
| (extra) | Cada ViewModel crea su **propio** Room + Retrofit + ServicioLocal | `ProgramaModel.kt:11`: `Servicios(app)` por instancia → no es "por proceso" |
| (extra) | Carrera con `--ez servicio_disponible false` | `MainActivity.kt:14` toca `model.mesa`, lo que crea el ViewModel; si la carga automática arranca en su `init`, la petición puede leer `disponible` antes de que se ponga en `false` |

---

## 3. Qué voy a cambiar (plan)

Criterio: cambios mínimos, en la capa que corresponde, respetando lo que ya usa el repo (Compose + StateFlow + Room + Retrofit, nombres en español).

### Capa de datos
1. **`data/Programa.kt`**
   - Nueva entidad `Edicion(id = 0, descargadaEn: Long)` (una sola fila) = "hay una edición válida descargada", aunque tenga 0 charlas. → resuelve 41 (v4 válida sin señal).
   - DAO: `observarEdicion(): Flow<Edicion?>`, `idsFavoritas()`, `borrarTodas()`, `registrarEdicion()`, y una operación **transaccional** `aplicarEdicion(fichas)` que:
     1. lee las marcas actuales **dentro de la transacción** (o sea, cuando ya llegó la respuesta → respeta marcas hechas durante la petición),
     2. borra todo y vuelve a insertar la instantánea copiando `favorita = id in marcas` (las retiradas desaparecen junto con su marca; si regresan, regresan sin marcar),
     3. registra la edición.
     (El DAO pasa a `abstract class` para poder tener un método `@Transaction` con cuerpo; alternativa: `withTransaction` en el repo.)
   - `Archivo` sube a **version = 2** con una **`Migration(1, 2)` manual** que crea la tabla `edicion` y, si la tabla `charlas` ya tenía datos, inserta la fila de edición. Así los usuarios con la versión instalada **conservan sus charlas y marcas** y quedan con "edición previa" (no se usa `fallbackToDestructiveMigration`).
2. **`data/AgendaRepository.kt`**
   - `sincronizar()` mapea DTO→`Charla` y llama a `dao.aplicarEdicion(...)`.
   - Expone `edicion: Flow<Edicion?>`.
   - Guarda el flag de proceso `reclamarCargaAutomatica()` (`AtomicBoolean`) → 44.
3. **`Servicios.kt` + nueva `JornadasApp : Application`** (y `android:name` en el Manifest)
   - Un único `Servicios` por proceso (Room, Retrofit, ServicioLocal y Repository). El contador de la Mesa queda realmente "por proceso".

### ViewModel — `ui/ProgramaModel.kt`
4. Constructor `(app, estado: SavedStateHandle)`; filtros `dia` y `tema` con `estado.getStateFlow(...)` y funciones `elegirDia()` / `elegirTema()` → sobreviven rotación, ficha abierta y muerte del proceso, y una tarea nueva arranca limpia (43).
5. Estado de pantalla como `sealed interface ProgramaUiState`:
   - `Cargando` (todavía no hay edición y hay descarga en curso),
   - `SinDatos(mensaje)` (nunca se descargó y falló → botón Reintentar),
   - `Listo(charlas filtradas, totalEdicion, usandoGuardados)`.
   Se arma con `combine(charlas, edicion, filtros, estadoDescarga)`. Lo que coexiste (`descargando: Boolean`) va aparte para mostrar la barra de progreso sin tapar la lista.
6. `recargar()`: si ya hay un `Job` activo, no hace nada (2 toques = 1 intento). Captura `Exception` (relanzando `CancellationException`). La carga automática se dispara en `init` solo si `repo.reclamarCargaAutomatica()` devuelve `true` (1 por proceso).
7. Estado expuesto solo como `StateFlow` de lectura (`_x` privado).

### UI
8. **`MainActivity.kt`**: sacar `recargar()` de `onStart()`. Aplicar el extra `servicio_disponible` sobre `Servicios` de la Application **antes** de crear el ViewModel (evita la carrera).
9. **`ui/Catalogo.kt`**: sacar el `LaunchedEffect`; leer filtros y `uiState` del ViewModel; `when` exhaustivo:
   - `Listo` + `usandoGuardados` → aviso «Sin conexión: se muestran los datos guardados» + lista + Reintentar,
   - `Listo` con edición vacía → «Sin charlas programadas»,
   - `Listo` con filtros sin coincidencias → «No hay charlas con estos filtros»,
   - `SinDatos` → mensaje + Reintentar; `Cargando` → indicador.
   - Botón Actualizar deshabilitado mientras descarga (feedback visual; el ViewModel igual protege). Los chips de filtro **siempre** habilitados.
10. **`ui/Portada.kt`**: ficha y Mi agenda siguen leyendo de Room (no cambian salvo adaptar nombres). Si la ficha abierta ya no existe en la edición, mostrar «Esta charla ya no está en el programa».
11. **`app/build.gradle.kts`**: `versionCode 13` / `versionName "1.3"` para instalar como actualización sobre la v1.2 conservando datos.

### Lo que NO voy a tocar
`lab/ServicioLocal.kt`, `ui/Mesa.kt` (solo cambia de dónde sale la instancia), `legacy/AgendaImpresa.kt`, los JSON de assets, estética/tema.

---

## 4. Cómo voy a verificar cada pedido

- **Compila**: `gradlew.bat assembleDebug` → `app/build/outputs/apk/debug/`.
- **Actualización**: instalar la versión original, marcar favoritas, instalar la nueva con `adb install -r` → las marcas siguen.
- **41**: v1 ok → apagar servicio → Actualizar → lista, ficha y Mi agenda intactas + aviso. `pm clear` + arranque con `servicio_disponible false` → Reintentar. v4 → «Sin charlas programadas»; matar proceso, apagar servicio, reabrir → sigue «Sin charlas programadas» (no pide reintento).
- **42**: marcar 11 y 23 en v1 → v2: 11 marcada con título/sala nuevos → v3: 11 no está en ningún lado, 90 sin marcar, 23 marcada → v1: la 11 vuelve sin marcar. Con demora larga: pedir v-actual, durante los 8 s marcar/desmarcar una vigente → al llegar se respeta.
- **43**: Día 2 + Datos → abrir «Cachés para viajeros» → volver → rotar → 2 resultados. Ficha abierta → Inicio → `adb shell am kill ar.jornadas` → Recientes → ficha abierta → Volver → ambos filtros. Abrir desde el launcher tras cerrar la tarea → sin filtros.
- **44**: mirar «Peticiones en esta sesión» en Mesa: arranque = 1; rotar, ficha, pestañas = +0; dos toques en Actualizar con demora larga = +1; después de éxito/falla Actualizar vuelve a sumar; cambiar filtros durante la espera.

## 5. Riesgos / decisiones a confirmar
- **Migración de Room** (v1→v2): es el punto más delicado; si el esquema de `edicion` no coincide exacto con la entidad, Room tira excepción al abrir. Alternativa más simple pero menos prolija: guardar el marcador de edición en SharedPreferences (sin migración), tratando "hay charlas en la BD" como edición previa para usuarios que actualizan.
- **"Una carga por proceso"**: se resuelve con un flag en el Repository singleton (no en el ViewModel), porque si el usuario cierra la Activity con Atrás y la vuelve a abrir en el mismo proceso, el ViewModel es nuevo pero el proceso no.
