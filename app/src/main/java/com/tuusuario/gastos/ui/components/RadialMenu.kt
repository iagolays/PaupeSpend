package com.tuusuario.gastos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Una opcion del abanico: icono, texto (para accesibilidad) y accion.
 * El insignia (opcional) es un simbolo pequeno que se dibuja sobresaliendo
 * de la esquina (p.ej. un "+"/"-" que acompana al simbolo del dolar).
 */
data class RadialMenuItem(
    val icono: ImageVector,
    val etiqueta: String,
    val color: Color = Color(0xFF6C5CE7),
    val badgeIcono: ImageVector? = null,
    val onClick: () -> Unit
)

/**
 * Boton de accion en forma de abanico, adaptado del original en React/CSS
 * (github.com/lorenzo04us/Bencho - src/lab/GlassKit.tsx - Radial).
 *
 * Gesto completo: pulsas el boton central, arrastras hacia la opcion y sueltas
 * = se ejecuta. Un toque simple abre el abanico; otro toque simple lo cierra
 * (o tambien se cierra pulsando fuera del fan).
 *
 * @param items opciones del abanico (entre 2 y 6, como en el original)
 * @param abierto estado controlado desde fuera (para poder cerrar con un toque exterior)
 * @param onAbiertoChange cambio de estado
 * @param stagger retraso en ms entre la animacion de una opcion y la siguiente
 * @param radius distancia del centro a cada opcion
 * @param spread angulo total que cubre el abanico, en grados
 * @param coreColor color de la cruceta central (el boton "+")
 * @param coreIconColor color del icono de la cruceta central
 */
@Composable
fun RadialMenu(
    items: List<RadialMenuItem>,
    abierto: Boolean,
    onAbiertoChange: (Boolean) -> Unit,
    stagger: Int = 28,
    radius: Dp = 68.dp,
    spread: Float = 180f,
    coreIcon: ImageVector = Icons.Default.Add,
    coreColor: Color = Color(0xFFBDBDBD),
    coreIconColor: Color = Color(0xFF37474F),
) {
    require(items.size in 2..6) { "RadialMenu admite entre 2 y 6 opciones" }
    val n = items.size

    var apuntando by remember { mutableStateOf<Int?>(null) }
    var pulsado by remember { mutableStateOf(false) }
    val radiusPx = with(LocalDensity.current) { radius.toPx() }

    fun anguloDeIndice(i: Int): Double {
        val paso = if (n > 1) spread / (n - 1) else 0f
        val gradoInicial = -90.0 - spread / 2.0
        return Math.toRadians(gradoInicial + paso * i)
    }

    fun indiceMasCercano(dx: Float, dy: Float): Int {
        val anguloToque = atan2(dy.toDouble(), dx.toDouble())
        var mejor = 0
        var menorDiferencia = Double.MAX_VALUE
        for (i in 0 until n) {
            var diferencia = abs(anguloToque - anguloDeIndice(i))
            if (diferencia > PI) diferencia = 2 * PI - diferencia
            if (diferencia < menorDiferencia) {
                menorDiferencia = diferencia
                mejor = i
            }
        }
        return mejor
    }

    Box(contentAlignment = Alignment.Center) {
        items.forEachIndexed { i, item ->
            val angulo = anguloDeIndice(i)
            val destinoX = if (abierto) (cos(angulo) * radiusPx).toFloat() else 0f
            val destinoY = if (abierto) (sin(angulo) * radiusPx).toFloat() else 0f
            val demora = if (abierto) i * stagger else 0

            val offsetX by animateFloatAsState(destinoX, tween(220, delayMillis = demora), label = "x$i")
            val offsetY by animateFloatAsState(destinoY, tween(220, delayMillis = demora), label = "y$i")
            val escala by animateFloatAsState(if (abierto) 1f else 0.4f, tween(220, delayMillis = demora), label = "s$i")
            // el fundido tambien animado: al cerrar los botones se encogen y se
            // desvanecen hacia el centro en vez de desaparecer de golpe
            val alfa by animateFloatAsState(if (abierto) 1f else 0f, tween(220, delayMillis = demora), label = "a$i")
            // feedback de pulsacion: crece un poquito mientras se mantiene presionado
            val escalaPulsado by animateFloatAsState(
                if (pulsado && apuntando == i) 1.12f else 1f,
                tween(110),
                label = "p$i",
            )

            FloatingActionButton(
                onClick = {
                    item.onClick()
                    onAbiertoChange(false)
                    apuntando = null
                },
                shape = CircleShape,
                containerColor = if (abierto && apuntando == i) item.color else item.color.copy(alpha = 0.95f),
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        translationX = offsetX
                        translationY = offsetY
                        scaleX = escala * escalaPulsado
                        scaleY = escala * escalaPulsado
                        // al llegar a alpha 0 quedan inertes bajo el boton central
                        alpha = alfa
                    }
            ) {
                Box {
                    Icon(item.icono, contentDescription = item.etiqueta)
                    item.badgeIcono?.let { insignia ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 7.dp, y = (-7).dp)
                                .size(16.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                insignia,
                                contentDescription = null,
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                }
            }
        }

        // Boton central: aqui vive el gesto completo (pulsar -> arrastrar -> soltar).
        // `abierto` va dentro de las keyes para que el gesto vuelva a leerse con el
        // valor actual: si no, la corrutina conserva el valor inicial y nunca cierra.
        Box(
            modifier = Modifier
                .size(56.dp)
                .pointerInput(items, spread, abierto) {
                    awaitEachGesture {
                        // 1) esperamos la pulsacion sobre el boton central
                        var bajada: PointerInputChange? = null
                        while (bajada == null) {
                            val evento = awaitPointerEvent()
                            bajada = evento.changes.firstOrNull { it.pressed }
                        }
                        val origen = bajada.position
                        val puntero = bajada.id
                        val estabaAbierto = abierto
                        var seMovio = false
                        pulsado = true
                        apuntando = null
                        // al pulsar siempre dejamos el fan desplegado (o ya lo esta)
                        onAbiertoChange(true)

                        while (true) {
                            val evento = awaitPointerEvent()
                            val cambio = evento.changes.firstOrNull { it.id == puntero } ?: break
                            if (!cambio.pressed) break

                            val dx = cambio.position.x - origen.x
                            val dy = cambio.position.y - origen.y
                            if (hypot(dx, dy) < 14f) {
                                apuntando = null
                            } else {
                                seMovio = true
                                apuntando = indiceMasCercano(dx, dy)
                            }
                            cambio.consume()
                        }
                        pulsado = false

                        when {
                            // arrastre que termina sobre una opcion: se ejecuta
                            seMovio && apuntando != null -> {
                                items[apuntando!!].onClick()
                                onAbiertoChange(false)
                            }
                            // toque simple sin arrastre: si estaba abierto se cierra,
                            // si estaba cerrado queda abierto (esperando segundo toque o gesto)
                            !seMovio && estabaAbierto -> onAbiertoChange(false)
                            // arrastre que no llego a ninguna opcion: se cierra
                            seMovio -> onAbiertoChange(false)
                        }
                        apuntando = null
                    }
                }
        ) {
            FloatingActionButton(
                onClick = { /* el gesto real esta en pointerInput de arriba */ },
                containerColor = coreColor,
                contentColor = coreIconColor,
            ) {
                Icon(coreIcon, contentDescription = if (abierto) "Cerrar menu" else "Abrir menu")
            }
        }
    }
}