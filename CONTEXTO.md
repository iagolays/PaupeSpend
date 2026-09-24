# PaupeSpend — Contexto del proyecto

Documento de contexto y historial de desarrollo de **PaupeSpend**, una app Android de
control de gastos personales en **Kotlin + Jetpack Compose**. Este `.md` recoge todo lo
que se ha hecho hasta ahora: objetivo, stack, arquitectura, modelo de datos, pantallas,
menú radial, notificaciones, historial de cambios y notas de depuración.

---

## 1. Qué es la app

- **Nombre:** PaupeSpend
- **Paquete / applicationId:** `com.tuusuario.gastos`
- **Idioma de la UI:** español (acentos incluidos en textos visibles).
- **Qué hace:**
  - Registrar gastos a mano y por **OCR de tickets** (cámara + ML Kit).
  - Registrar ingresos.
  - Categorías con icono, color y límite mensual opcional.
  - Presupuestos mensuales (generales o por categoría) con barra de progreso y avisos.
  - Gastos fijos / suscripciones recurrentes.
  - Gráficas: barras o líneas por meses/semanas/días, y gráfico radial por categoría.
  - Exportar e importar **Excel (.xlsx)** vía SAF (Storage Access Framework), sin librerías POI.
  - Notificaciones de aviso de presupuesto (alarma diaria + comprobación al abrir la app).

## 2. Stack técnico

| Componente | Versión / valor |
|---|---|
| Build tool | Gradle wrapper **9.6.0** |
| Android Gradle Plugin | **9.4.1** |
| Kotlin | **2.2.10** (plugin nativo de AGP, no se usa `kotlin.android`) |
| Compose compiler | **2.2.10** (plugin `org.jetbrains.kotlin.plugin.compose`) |
| KSP | **2.2.10-2.0.2** |
| Compose BOM | **2026.09.00** |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| Room | 2.8.5 |
| navigation-compose | 2.10.2 |
| CameraX | 1.6.2 |
| ML Kit text-recognition | 16.0.1 |
| material-icons-extended | yes (BOM) |

- `gradle.properties` contiene `android.disallowKotlinSourceSets=false` (obligatorio para
  que KSP funcione con el build-in de Kotlin; imprime un warning experimental inofensivo).
- `local.properties` NO se sube a git (contiene `sdk.dir=/home/iagoleis/Android/Sdk`).

## 3. Arquitectura y estructura

```
app/src/main/java/com/tuusuario/gastos/
├── MainActivity.kt          # Activity: edge-to-edge, pide permiso de notificaciones
├── GastosApp.kt             # Application: inicializa Repositorio, canal y alarma; siembra categorias
├── Navegacion.kt            # Rutas + AppNavHost (navigation-compose)
├── data/
│   ├── AppDatabase.kt       # Room DB "gastos.db" v2 (migracion 1->2), Conversores
│   ├── Modelos.kt           # Categoria, Gasto, Ingreso, Suscripcion, Presupuesto
│   ├── Repositorio.kt       # Objeto singleton con repos de DAOs + helpers
│   └── dao/                 # DAOs por entidad
├── ui/
│   ├── components/
│   │   ├── RadialMenu.kt    # Menu abanico (Bencho)
│   │   ├── Graficos.kt      # GraficoBarras, GraficoLineas, GraficoRadial (N-gono)
│   │   └── ...              # Encabezado, BurbujaCategoria, etc.
│   ├── screens/             # Pantallas (ver seccion 5)
│   └── theme/
└── util/
    ├── Notificaciones.kt    # Canal, alarma, notifica, comprobacion
    ├── RecibidorPresupuesto.kt  # BroadcastReceiver de la alarma
    ├── RecibidorArranque.kt     # Re-programa la alarma tras reboot/actualizacion
    ├── Excel.kt             # Exportar/importar .xlsx (ZipOutputStream + XML pull)
    ├── ParseoTexto.kt       # OCR: parseo de tickets + clasificacion en categoria
    └── Util.kt              # formatearDinero, fechas, aColor...
```

Flujo de arranque:
1. `GastosApp.onCreate` → `Repositorio.inicializar(this)` (abre Room), crea canal de
   notificación, programa la alarma diaria, y siembra las categorías iniciales si la BD
   está vacía.
2. `MainActivity` → pide `POST_NOTIFICATIONS` (Android 13+) y monta `TemaGastos { AppNavHost() }`.

## 4. Modelo de datos (Room, `gastos.db`)

- **Tablas:** `categorias`, `gastos`, `ingresos`, `suscripciones`, `presupuestos`.
- **`Conversores`:** `LocalDate` ↔ epochDay (`Long`), `MetodoPago` ↔ `String` (enum `EFECTIVO|TARJETA`).
- **`Gasto`:** id, descripcion, monto, fecha, metodoPago, categoriaId (FK `SET_NULL`), rutaTicket?, esFijo.
- **`Categoria`:** id, nombre, icono, colorHex, limiteMensual?.
- **`Ingreso`:** id, descripcion, monto, fecha, esRecurrente.
- **`Suscripcion`:** id, nombre, monto, diaDeCobro (1-31), categoriaId?, activa.
- **`Presupuesto`:** id, categoriaId? (null = general), limite, mes, anio, **notificar** (campana).
- **Esquema: versión 2.** La migración `1 -> 2` añade:
  `ALTER TABLE presupuestos ADD COLUMN notificar INTEGER NOT NULL DEFAULT 0`

Datos actuales en el emulador (referencia): 7 categorías, 5 gastos, 2 presupuestos
(Comida límite 20, Transporte límite 200), sin ingresos.

## 5. Pantallas

| Pantalla | Archivo | Contenido |
|---|---|---|
| Principal | `PantallaPrincipal.kt` | Resumen del mes (balance/ingresos/gastos) con **engranaje de Ajustes** arriba a la derecha; accesos rápidos (Categorías, Presupuestos, Gastos fijos, Excel); barras de presupuesto; lista de gastos del mes (con botón borrar); **menú radial** abajo-centro |
| Gastos | `PantallaAnadirGasto.kt` | Alta manual / gasto fijo, con `guardando` anti doble-tap |
| Ingresos | `PantallaAnadirIngreso.kt` | Alta de ingreso |
| Escaner | `PantallaEscaner.kt` | CameraX + ML Kit, análisis con `setAnalyzer`, coordenadas de la cámara |
| Confirmar ticket | `PantallaConfirmarTicket.kt` | Muestra items parseados del OCR para guardar |
| Categorías | `PantallaCategorias.kt` | CRUD con diálogo (borrar en diálogo y papelera por fila) |
| Presupuestos | `PantallaPresupuestos.kt` | Listado + diálogo crear/editar, barra gruesa (12dp), **campana `BotonNotify`** |
| Gráfica | `PantallaGrafica.kt` | Chips `Barras/Línea` y `Días/Semanas/Meses`; tarjetas de resumen; botón al gráfico radial |
| Radial por categoría | `PantallaHexagono.kt` | Gráfico radial adaptativo (3→triángulo, 6→hexágono…) |
| Suscripciones | `PantallaSuscripciones.kt` | Gastos fijos recurrentes |
| Reporte | `PantallaReporte.kt` | Exportar/Importar Excel vía SAF |
| Ajustes | `PantallaAjustes.kt` | Placeholder futuro + explicación de avisos + **"Enviar notificación de prueba"** |

## 6. Menú radial (estilo Bencho)

- Adaptado del original en React/CSS (`github.com/lorenzo04us/Bencho`, `src/lab/GlassKit.tsx`).
- 4 opciones: Añadir gasto (mano), Añadir ingreso, Escanear ticket, Ver gráfica.
- **Gestos (estado controlado `abierto`/`onAbiertoChange`):**
  - Pulsar el centro desplega el abanico.
  - Arrastrar y soltar sobre una opción la ejecuta.
  - Tocar de nuevo el centro (o cualquier punto fuera) la cierra.
- **Feedback:** cada botón crece ~12% al mantenerlo pulsado.
- **Animación:** apertura con `stagger` por ítem; **cierre con retorno + fundido** (la
  alpha se anima junto a la posición/escala, nunca se pone bruscamente a 0).
- **Insignia esquinero:** círculo blanco sobresaliendo en la esquina con símbolo gris claro
  (`#9E9E9E`); el "+" interior de la cruceta central es gris claro (`#BDBDBD`) con icono oscuro.
- Datos: `RadialMenuItem(icono, etiqueta, color, badgeIcono?, onClick)`; `spread=180º`,
  `radius=90dp`, `stagger=28ms`, límite de 2..6 opciones.

**Error corregido (cierre):** el gesto capturaba el valor de `abierto` de la composición
inicial dentro de `pointerInput`; como `abierto` no estaba entre las claves, la corrutina
conservaba siempre `false` y nunca detectaba que debía cerrar. **Solución:** `abierto`
entra en las claves de `pointerInput(items, spread, abierto)`.

## 7. Notificaciones

- Canal `presupuesto` ("Alertas de presupuesto", importancia DEFAULT).
- Alarma **diaria 21:00** con `AlarmManager.setAndAllowWhileIdle` *de un solo disparo*, que
  el propio `RecibidorPresupuesto` re-programa al día siguiente (más fiable que
  `setInexactRepeating`). `RecibidorArranque` la re-programa tras boot/actualización.
- **Comprobación** también al abrir la app principal (`LaunchedEffect`).
- **Regla:** solo avisa de presupuestos con la **campana activada** (`notificar=true`).
  - `>= 90%` → "Presupuesto casi agotado"
  - `>= 100%` → "Presupuesto superado"
- **Permiso:** la app solicita `POST_NOTIFICATIONS` en tiempo de ejecución (Android 13+) y
  al concederlo hace una comprobación inmediata. Ya estaba declarado en el manifest junto a
  `RECEIVE_BOOT_COMPLETED`.
- Verificado de extremo a extremo en el emulador: con la campana ON y presupuesto superado,
  la notificación (id 2001, canal `presupuesto`) se publica correctamente.

## 8. Gráficos

- `GraficoBarras` / `GraficoLineas`: doble serie (gasto/ingreso) por periodo; etiquetas
  dibujadas con `nativeCanvas`.
- `GraficoRadial` (antes "GraficoHexagono"): poligono con **N vértices según el número real
  de categorías** (rejas 33/66/100%, ejes, polígono de datos con su color, etiquetas).
  Se renombró en la UI como *Gráfico radial por categoría* (se eliminó el término "hexagonal").

## 9. Excel (`util/Excel.kt`)

- Exportación e importación `.xlsx` **sin POI**: el xlsx es un ZIP, se escribe con
  `ZipOutputStream` (sheet XML + shared strings) y se lee con XML pull.
- `PantallaReporte` usa SAF (`ActivityResultContracts.CreateDocument` /
  `OpenDocument`) con constante MIME; el export crea hojas con cabeceras, filas y estilos
  básicos; el import lee valores y reconstruye los registros.
- Tras el guardado se fuerza un `checkpoint()` de Room para que la BD en WAL quede al día.

## 10. Historial de cambios (sesiones)

1. **Scaffold y primer build CLI**
   - Faltaba `local.properties` (sdk) → la primera compilación desde terminal no arrancaba.
   - Se arreglaron ~14 errores de compilación: import de `nativeCanvas`, elevación de
     `LocalDensity` fuera del `Canvas`, sustitución de `awaitFirstDown()` por bucle de
     eventos (`awaitEachGesture`), lambda de `setAnalyzer`, imports de `Icon`/`formatear`,
     `categoriaId` en constructores de Presupuesto/Suscripcion, `Icons.Filled.Tally` → `PieChart`,
     `AccesoRapido` como `RowScope`, `idCategoria` suspend en Excel.
   - **Crash al arrancar:** el manifest no tenía `android:name=".GastosApp"` → la BD no se
     inicializaba ("Repositorio no inicializado"). Se añadió.
2. **UX ronda 1:** radial abajo-centro, etiqueta "Gastos fijos", textos centrados, `$`
   verde/rojo con insignias +/−.
3. **UX ronda 2 + crash:** pantalla en blanco y gastos duplicados/crash → colisión de claves
   del `LazyColumn` ("Key 1 already used") entre `item{}` sin clave e `items(key={it.id})`;
   **solución:** prefijos `"presu-", "gasto-", "susc-", "fijo-"`. Duplicados por doble-tap →
   estados `guardando` en los botones de guardar. Además: insignia a la esquina (círculo
   blanco), colores `#E53935`/`#00C853`, alpha 0.95, resumen con importes coloreados,
   diálogo de presupuesto centrado.
4. **UX ronda 3:** borrar gasto; cierre del radial (toque + exterior); barras de progreso
   gruesas; radial adaptativo y renombrado; borrar categorías; conmutador barras/línea;
   campana `notificar` por presupuesto (con migración Room v1→v2); cruceta interior gris;
   feedback de presión. Se reparó el import de `androidx.room.migration.Migration`.
5. **UX ronda 4 + notificaciones:** corregido el cierre del radial (clave `abierto` en
   `pointerInput`); vista Días/Semanas/Meses en la gráfica; cruceta central gris claro real;
   **notificaciones funcionales** (permiso en runtime, alarma fiable, pantalla Ajustes con
   explicación y botón de prueba); engranaje de Ajustes en el resumen del mes.
6. **UX ronda 5:** animación de cierre del radial (fade + contracción en 220ms; la alpha
   dejó de ponerse a 0 instantáneamente).

## 11. Compilar e instalar

```bash
# Compilar (APK debug)
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

# Instalar y lanzar en emulador/dispositivo
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tuusuario.gastos/.MainActivity
```

## 12. Notas de depuración (emulador)

- Emulador de referencia: `emulator-5554` (AVD `Pixel_10`, `sdk_gphone16k_x86_64`).
  `adb = /home/iagoleis/Android/Sdk/platform-tools/adb`.
- Comprobación rápida: `adb shell pidof com.tuusuario.gastos` y
  `adb logcat -d | grep -E "FATAL.*gastos"` (debe dar 0).
- **`adb pull` NO puede leer `databases/`** de la app. Para extraer la BD entera:
  `adb exec-out run-as com.tuusuario.gastos cat databases/gastos.db > gastos.db`
- Al copiar por `run-as` conviene incluir `-wal`/`-shm` o abrir la BD con un proceso
  detenido; con WAL, el fichero principal por sí solo puede verse como "vacío".
- Quirks de esta versión de Compose: no existe `awaitFirstDown()`, no hay `easing` dentro de
  `keyframes` (usar los puntos clave), `Icons.Filled.Tally` no existe, `Migration` vive en
  `androidx.room.migration`, y un estado leído dentro de `pointerInput` debe estar en las
  claves para no quedarse estancado.

## 13. Pendiente / ideas futuras

- Más opciones reales en la pantalla de **Ajustes** (placeholder actual).
- Decidir si deduplicar el aviso diario cuando un presupuesto sigue superado.
- Revisar el estado de las coordenadas del previsualizador del escáner (mismatch de ejes).