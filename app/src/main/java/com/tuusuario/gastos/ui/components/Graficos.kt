package com.tuusuario.gastos.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class ParBarras(val etiqueta: String, val gasto: Float, val ingreso: Float)

@Composable
fun GraficoBarras(
    datos: List<ParBarras>,
    colorGasto: Color,
    colorIngreso: Color,
    altura: Dp = 200.dp,
) {
    val densidad = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxWidth().height(altura)) {
        if (datos.isEmpty()) return@Canvas
        val maximo = max(datos.maxOf { it.gasto }, datos.maxOf { it.ingreso }).coerceAtLeast(1f)
        val anchoGrupo = size.width / datos.size
        val base = size.height - 14f
        val maxAltura = size.height - 18f

        val textoPaint = Paint().apply {
            color = android.graphics.Color.rgb(140, 140, 150)
            textSize = with(densidad) { 11.dp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        datos.forEachIndexed { i, d ->
            val centro = anchoGrupo * i + anchoGrupo / 2f
            val anchoBarra = anchoGrupo * 0.26f

            val altGasto = if (d.gasto <= 0f) 0f else (d.gasto / maximo) * maxAltura
            val altIngreso = if (d.ingreso <= 0f) 0f else (d.ingreso / maximo) * maxAltura

            drawRect(
                color = colorGasto,
                topLeft = Offset(centro - anchoBarra - anchoBarra * 0.45f, base - altGasto),
                size = androidx.compose.ui.geometry.Size(anchoBarra, altGasto),
            )
            drawRect(
                color = colorIngreso,
                topLeft = Offset(centro + anchoBarra * 0.45f, base - altIngreso),
                size = androidx.compose.ui.geometry.Size(anchoBarra, altIngreso),
            )

            drawContext.canvas.nativeCanvas.drawText(
                d.etiqueta,
                centro,
                base + 12f,
                textoPaint,
            )
        }
    }
}

@Composable
fun GraficoLineas(
    datos: List<ParBarras>,
    colorGasto: Color,
    colorIngreso: Color,
    altura: Dp = 200.dp,
) {
    val densidad = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxWidth().height(altura)) {
        if (datos.isEmpty()) return@Canvas
        val maximo = max(datos.maxOf { it.gasto }, datos.maxOf { it.ingreso }).coerceAtLeast(1f)
        val maxAltura = size.height - 24f
        val base = size.height - 14f
        val grosor = with(densidad) { 2.5.dp.toPx() }
        val radioPunto = with(densidad) { 3.dp.toPx() }

        fun punto(i: Int, valor: Float): Offset {
            val x = if (datos.size == 1) size.width / 2f else size.width * i / (datos.size - 1)
            val y = base - (valor / maximo) * maxAltura
            return Offset(x, y)
        }

        fun trazar(color: Color, selector: (ParBarras) -> Float) {
            datos.forEachIndexed { i, d ->
                drawCircle(color, radius = radioPunto, center = punto(i, selector(d)))
            }
            if (datos.size < 2) return
            val ruta = Path()
            datos.forEachIndexed { i, d ->
                val p = punto(i, selector(d))
                if (i == 0) ruta.moveTo(p.x, p.y) else ruta.lineTo(p.x, p.y)
            }
            drawPath(ruta, color = color, style = Stroke(width = grosor, cap = StrokeCap.Round))
        }

        trazar(colorGasto) { it.gasto }
        trazar(colorIngreso) { it.ingreso }

        val textoPaint = Paint().apply {
            color = android.graphics.Color.rgb(140, 140, 150)
            textSize = with(densidad) { 11.dp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        datos.forEachIndexed { i, d ->
            drawContext.canvas.nativeCanvas.drawText(
                d.etiqueta,
                punto(i, 0f).x,
                size.height - 2f,
                textoPaint,
            )
        }
    }
}

data class AroRadar(val etiqueta: String, val valor: Float, val color: Color)

/**
 * Grafico radial de categoria (tipo "telarana"/radar): se dibuja un poligono
 * con tantos vertices como categorias haya, asi que con 3 categorias es un
 * triangulo, con 6 un hexagono, etc. Se adapta al numero real de categorias.
 */
@Composable
fun GraficoRadial(items: List<AroRadar>, radioMax: Dp = 120.dp) {
    val densidad = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxWidth().height(radioMax * 2.3f)) {
        val top = items.take(12)
        if (top.isEmpty()) return@Canvas
        val n = top.size
        val centro = Offset(size.width / 2f, size.height / 2.05f)
        val radio = min(size.width, size.height) * 0.38f
        val maximo = top.maxOf { it.valor }.coerceAtLeast(1f)

        fun vertice(indice: Int, escala: Float): Offset {
            val angulo = Math.toRadians(-90.0 + 360.0 / n * indice)
            return Offset(
                centro.x + (cos(angulo) * radio * escala).toFloat(),
                centro.y + (sin(angulo) * radio * escala).toFloat(),
            )
        }

        // Rejas de referencia (33%, 66%, 100%).
        listOf(0.33f, 0.66f, 1f).forEach { escala ->
            val path = Path()
            for (i in 0 until n) {
                val p = vertice(i, escala)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, color = Color.LightGray.copy(alpha = 0.4f), style = Stroke(width = 1f))
        }

        // Ejes a cada categoria.
        for (i in 0 until n) {
            val p = vertice(i, 1f)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = centro,
                end = p,
                strokeWidth = 1f,
            )
        }

        // Poligono de datos.
        val datos = Path()
        top.forEachIndexed { i, aro ->
            val p = vertice(i, (aro.valor / maximo).coerceIn(0f, 1f))
            if (i == 0) datos.moveTo(p.x, p.y) else datos.lineTo(p.x, p.y)
        }
        datos.close()
        drawPath(datos, color = top[0].color.copy(alpha = 0.25f))
        drawPath(datos, color = top[0].color, style = Stroke(width = 2.5f))

        // Puntos + etiquetas.
        val etiquetaPaint = Paint().apply {
            color = android.graphics.Color.rgb(90, 90, 110)
            textSize = with(densidad) { 12.dp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        top.forEachIndexed { i, aro ->
            val p = vertice(i, 1f)
            drawCircle(color = aro.color, radius = 3f, center = p)

            val etiqueta = if (aro.etiqueta.length > 9) aro.etiqueta.take(9) else aro.etiqueta
            var textoY = p.y + (if (p.y >= centro.y) 16f else -8f)
            if (p.x == centro.x && p.y < centro.y) textoY = p.y - 10f

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    etiqueta,
                    p.x.coerceIn(30f, size.width - 30f),
                    textoY,
                    etiquetaPaint,
                )
            }
        }
    }
}