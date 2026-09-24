package com.tuusuario.gastos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.ui.components.Encabezado
import com.tuusuario.gastos.ui.components.GraficoBarras
import com.tuusuario.gastos.ui.components.GraficoLineas
import com.tuusuario.gastos.ui.components.ParBarras
import com.tuusuario.gastos.util.formatearDinero
import com.tuusuario.gastos.util.ultimoDelMes
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private enum class VistaGrafico { DIAS, SEMANAS, MESES }

private val formatoCorto = DateTimeFormatter.ofPattern("dd/MM")

@Composable
fun PantallaGrafica(onAtras: () -> Unit, onRadial: () -> Unit) {
    val hoy = remember { LocalDate.now() }
    val anioActual = remember { YearMonth.from(hoy) }
    val desde = remember { anioActual.minusMonths(11).atDay(1) }
    var vista by remember { mutableStateOf(VistaGrafico.MESES) }
    var vistaLinea by remember { mutableStateOf(false) }

    val gastos by Repositorio.gastos.observarEntreFechas(desde, hoy).collectAsState(initial = emptyList())
    val ingresos by Repositorio.ingresos.observarTodos().collectAsState(initial = emptyList())

    val datos = when (vista) {
        VistaGrafico.MESES -> (0..11).map { offset ->
            val ym = anioActual.minusMonths((11 - offset).toLong())
            ParBarras(
                ym.month.name.take(3).uppercase().take(3),
                gastos.filter { YearMonth.from(it.fecha) == ym }.sumOf { it.monto }.toFloat(),
                ingresos.filter { YearMonth.from(it.fecha) == ym }.sumOf { it.monto }.toFloat(),
            )
        }
        VistaGrafico.SEMANAS -> {
            val inicioSemanaActual = hoy.minusDays((hoy.dayOfWeek.value - 1).toLong())
            (7 downTo 0).map { offset ->
                val inicio = inicioSemanaActual.minusWeeks(offset.toLong())
                val fin = inicio.plusWeeks(1)
                ParBarras(
                    inicio.format(formatoCorto),
                    gastos.filter { !it.fecha.isBefore(inicio) && it.fecha.isBefore(fin) }.sumOf { it.monto }.toFloat(),
                    ingresos.filter { !it.fecha.isBefore(inicio) && it.fecha.isBefore(fin) }.sumOf { it.monto }.toFloat(),
                )
            }
        }
        VistaGrafico.DIAS -> (14 downTo 0).map { offset ->
            val dia = hoy.minusDays(offset.toLong())
            ParBarras(
                dia.format(formatoCorto),
                gastos.filter { it.fecha == dia }.sumOf { it.monto }.toFloat(),
                ingresos.filter { it.fecha == dia }.sumOf { it.monto }.toFloat(),
            )
        }
    }

    val tituloPeriodo = when (vista) {
        VistaGrafico.MESES -> "Ultimos 12 meses"
        VistaGrafico.SEMANAS -> "Ultimas 8 semanas"
        VistaGrafico.DIAS -> "Ultimos 15 dias"
    }
    val gastoTotalPeriodo = datos.sumOf { it.gasto.toDouble() }
    val ingresoTotalPeriodo = datos.sumOf { it.ingreso.toDouble() }
    val gastoTotalMes = gastos.filter { YearMonth.from(it.fecha) == anioActual }.sumOf { it.monto }
    val ingresoMes = ingresos.filter { YearMonth.from(it.fecha) == anioActual }.sumOf { it.monto }
    val mayorGastoPeriodo = datos.maxByOrNull { it.gasto }

    Scaffold(
        topBar = { Encabezado(titulo = "Grafica de gastos", onAtras = onAtras) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TarjetaMini("Gastos este mes", gastoMes(gastos, anioActual), Color(0xFFFF6B6B), Modifier.weight(1f))
                TarjetaMini("Ingresos este mes", ingresoMes, Color(0xFF55EFC4), Modifier.weight(1f))
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tituloPeriodo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = !vistaLinea,
                            onClick = { vistaLinea = false },
                            label = { Text("Barras") },
                        )
                        Spacer(Modifier.width(6.dp))
                        FilterChip(
                            selected = vistaLinea,
                            onClick = { vistaLinea = true },
                            label = { Text("Linea") },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = vista == VistaGrafico.DIAS,
                            onClick = { vista = VistaGrafico.DIAS },
                            label = { Text("Dias") },
                        )
                        FilterChip(
                            selected = vista == VistaGrafico.SEMANAS,
                            onClick = { vista = VistaGrafico.SEMANAS },
                            label = { Text("Semanas") },
                        )
                        FilterChip(
                            selected = vista == VistaGrafico.MESES,
                            onClick = { vista = VistaGrafico.MESES },
                            label = { Text("Meses") },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Leyenda(color = Color(0xFFFF6B6B), texto = "Gastos")
                    Spacer(Modifier.height(4.dp))
                    Leyenda(color = Color(0xFF55EFC4), texto = "Ingresos")
                    Spacer(Modifier.height(14.dp))
                    if (vistaLinea) {
                        GraficoLineas(datos = datos, colorGasto = Color(0xFFFF6B6B), colorIngreso = Color(0xFF55EFC4))
                    } else {
                        GraficoBarras(datos = datos, colorGasto = Color(0xFFFF6B6B), colorIngreso = Color(0xFF55EFC4))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Total gastado en el periodo: ${gastoTotalPeriodo.formatearDinero()}")
                    Text("Total ingresado en el periodo: ${ingresoTotalPeriodo.formatearDinero()}")
                    mayorGastoPeriodo?.let {
                        if (it.gasto > 0f) Text("Periodo con mas gasto: ${it.etiqueta} (${it.gasto.toDouble().formatearDinero()})")
                    }
                    Text("Balance de este mes: ${(ingresoMes - gastoMes(gastos, anioActual)).formatearDinero()}")
                }
            }

            Button(
                onClick = onRadial,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ver grafico radial por categoria")
            }
        }
    }
}

private fun gastoMes(gastos: List<com.tuusuario.gastos.data.Gasto>, ym: YearMonth): Double =
    gastos.filter { YearMonth.from(it.fecha) == ym }.sumOf { it.monto }

@Composable
private fun TarjetaMini(etiqueta: String, valor: Double, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = color)
            Spacer(Modifier.height(4.dp))
            Text(valor.formatearDinero(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}