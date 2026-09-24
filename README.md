# PaupeSpend

Un pequeño proyecto de una App de gastos, pues, como buenos universitarios andamos cortos de dinero y duele ver como la cuenta baja sin parar.

Es una app formato .apk sencilla que permite clasificar tus gastos por categorias, añadir presupuestos con sus alertas, consultar un histórico de gastos y exportar los datos en formato excel.

## Qué puedes hacer

- **Añadir gastos** a mano o escribir un **ingreso** en segundos.
- Escanear el **ticket con la cámara**: la app reconoce los productos por OCR y los guarda casi sin tocarte el bolsillo.
- **Categorías** personalizadas (con su icono y color) para tener los gastos bien ordenados.
- **Presupuestos mensuales** con barra de progreso y aviso de notificación cuando vas muy apurado (al 90% "casi agotado" y al 100% "superado").
- **Gastos fijos y suscripciones** para que no se te olviden esas cuotas recurrentes.
- **Gráficas** de barras o de líneas por días, semanas o meses, y un gráfico radial por categoría para ver dónde se va el dinero de un vistazo.
- **Exportar e importar** tus datos en **Excel (.xlsx)**.
- **Notificaciones** diarias para avisarte del estado de tus presupuestos.

## Cómo montarlo

Puedes clonar este repositorio y trabajar tu mismo en el para personalizarla a tu gusto:

```bash
git clone https://github.com/iagolays/PaupeSpend.git
```

Compilar el APK (necesitas Android Studio o el SDK de Android):

```bash
./gradlew :app:assembleDebug
```

El APK de debug queda en `app/build/outputs/apk/debug/app-debug.apk`: instálalo en tu móvil o emulador.

## Detalles técnicos (por si te interesa)

Kotlin + Jetpack Compose, Room (SQLite) para los datos, ML Kit + CameraX para el escaneo del ticket, y navegación con Navigation Compose. El export a Excel se genera sin librerías externas.