package com.tuusuario.gastos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tuusuario.gastos.data.Categoria
import com.tuusuario.gastos.data.Gasto
import com.tuusuario.gastos.data.MetodoPago
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.ui.components.BurbujaCategoria
import com.tuusuario.gastos.ui.components.RadialMenu
import com.tuusuario.gastos.ui.components.RadialMenuItem
import com.tuusuario.gastos.util.Notificaciones
import com.tuusuario.gastos.util.aColor
import com.tuusuario.gastos.util.formatearDinero
import com.tuusuario.gastos.util.formatear
import com.tuusuario.gastos.util.primeroDelMes
import com.tuusuario.gastos.util.ultimoDelMes
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun PantallaPrincipal(
    onAnadirGastoManual: () -> Unit,
    onEscanearTicket: () -> Unit,
    onAnadirIngreso: () -> Unit,
    onVerGrafica: () -> Unit,
    onCategorias: () -> Unit,
    onPresupuestos: () -> Unit,
    onSuscripciones: () -> Unit,
    onReporte: () -> Unit,
    onAjustes: () -> Unit,
) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    var radialAbierto by rememberSaveable { mutableStateOf(false) }
    val hoy = remember { LocalDate.now() }

    val categorias by Repositorio.categorias.observarTodas().collectAsState(initial = emptyList())
    val gastosMes by Repositorio.gastos
        .observarEntreFechas(hoy.primeroDelMes, hoy.ultimoDelMes)
        .collectAsState(initial = emptyList())
    val totalGastado by Repositorio.gastos
        .observarTotalEntre(hoy.primeroDelMes, hoy.ultimoDelMes)
        .collectAsState(initial = 0.0)
    val totalIngresos by Repositorio.ingresos
        .observarTotalEntre(hoy.primeroDelMes, hoy.ultimoDelMes)
        .collectAsState(initial = 0.0)
    val presus by Repositorio.presupuestos
        .observarDelMes(hoy.monthValue, hoy.year)
        .collectAsState(initial = emptyList())
    val totalesPorCategoria by Repositorio.gastos
        .observarTotalesPorCategoria(hoy.primeroDelMes, hoy.ultimoDelMes)
        .collectAsState(initial = emptyList())

    val nombreCategoria = categorias.associateBy { it.id }

    LaunchedEffect(Unit) {
        Notificaciones.comprobarAhora(contexto)
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TarjetaResumen(
                        balance = totalIngresos - totalGastado,
                        ingresos = totalIngresos,
                        gastos = totalGastado,
                        mes = hoy,
                        onAjustes = onAjustes,
                    )
                }

                item {
                    FilaAccesosRapidos(
                        onCategorias = onCategorias,
                        onPresupuestos = onPresupuestos,
                        onSuscripciones = onSuscripciones,
                        onReporte = onReporte,
                    )
                }

                if (presus.isNotEmpty()) {
                    item { TituloSeccion("Presupuestos del mes") }
                    items(presus, key = { "presu-${it.id}" }) { p ->
                        val gastado = if (p.categoriaId == null) {
                            totalGastado
                        } else {
                            totalesPorCategoria.firstOrNull { it.categoriaId == p.categoriaId }?.total ?: 0.0
                        }
                        val nombre = if (p.categoriaId == null) "General" else nombreCategoria[p.categoriaId]?.nombre ?: "Categoria"
                        val color = if (p.categoriaId == null) Color(0xFF6C5CE7) else nombreCategoria[p.categoriaId]?.colorHex?.aColor() ?: Color(0xFF6C5CE7)
                        BarraPresupuesto(nombre = nombre, color = color, gastado = gastado, limite = p.limite)
                    }
                }

                item { TituloSeccion("Gastos del mes") }
                items(gastosMes, key = { "gasto-${it.id}" }) { gasto ->
                    FilaGasto(
                        gasto = gasto,
                        categoria = nombreCategoria[gasto.categoriaId],
                        onBorrar = { scope.launch { Repositorio.gastos.borrar(gasto) } },
                    )
                }
            }

            if (radialAbierto) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            radialAbierto = false
                        }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                RadialMenu(
                    items = listOf(
                        RadialMenuItem(
                            Icons.Default.AttachMoney, "Añadir gasto (mano)",
                            Color(0xFFE53935), Icons.Default.Remove, onAnadirGastoManual,
                        ),
                        RadialMenuItem(
                            Icons.Default.AttachMoney, "Añadir ingreso",
                            Color(0xFF00C853), Icons.Default.Add, onAnadirIngreso,
                        ),
                        RadialMenuItem(
                            Icons.Default.CameraAlt, "Escanear ticket",
                            Color(0xFF0984E3), null, onEscanearTicket,
                        ),
                        RadialMenuItem(
                            Icons.Default.PieChart, "Ver grafica",
                            Color(0xFF6C5CE7), null, onVerGrafica,
                        ),
                    ),
                    abierto = radialAbierto,
                    onAbiertoChange = { radialAbierto = it },
                    stagger = 28,
                    radius = 90.dp,
                    spread = 180f,
                )
            }
        }
    }
}

@Composable
private fun TarjetaResumen(balance: Double, ingresos: Double, gastos: Double, mes: LocalDate, onAjustes: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44)),
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Resumen de ${mes.month.name.lowercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onAjustes) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = balance.formatearDinero(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp),
            )
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
                ColumnaMini("Ingresos", ingresos, Color(0xFF55EFC4))
                Spacer(Modifier.width(24.dp))
                ColumnaMini("Gastos", gastos, Color(0xFFFF6B6B))
            }
        }
    }
}

@Composable
private fun ColumnaMini(etiqueta: String, valor: Double, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(6.dp))
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(4.dp))
        Text(valor.formatearDinero(), style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FilaAccesosRapidos(
    onCategorias: () -> Unit,
    onPresupuestos: () -> Unit,
    onSuscripciones: () -> Unit,
    onReporte: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AccesoRapido(Icons.Default.Category, "Categorias", Color(0xFF6C5CE7), onCategorias)
        AccesoRapido(Icons.Default.PieChart, "Presupuestos", Color(0xFF00B894), onPresupuestos)
        AccesoRapido(Icons.Default.Savings, "Gastos fijos", Color(0xFF0984E3), onSuscripciones)
        AccesoRapido(Icons.Default.ReceiptLong, "Excel", Color(0xFFFDCB6E), onReporte)
    }
}

@Composable
private fun RowScope.AccesoRapido(icono: ImageVector, etiqueta: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icono, contentDescription = etiqueta, tint = color)
            Spacer(Modifier.height(6.dp))
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun BarraPresupuesto(nombre: String, color: Color, gastado: Double, limite: Double) {
    val porcentaje = if (limite <= 0) 0f else (gastado / limite).toFloat().coerceIn(0f, 1f)
    val colorBarra = if (gastado >= limite) Color(0xFFE17055) else color
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(nombre, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${gastado.formatearDinero()} de ${limite.formatearDinero()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { porcentaje },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = colorBarra,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun FilaGasto(gasto: Gasto, categoria: Categoria?, onBorrar: () -> Unit) {
    val color = categoria?.colorHex?.aColor() ?: Color(0xFF6C5CE7)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BurbujaCategoria(icono = categoria?.icono ?: "\u25AB", color = color)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(gasto.descripcion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                "${categoria?.nombre ?: "Sin categoria"} · ${gasto.fecha.formatear()}${if (gasto.esFijo) " · Fijo" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            if (gasto.metodoPago == MetodoPago.EFECTIVO) Icons.Default.Payments else Icons.Default.AttachMoney,
            contentDescription = gasto.metodoPago.name,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            gasto.monto.formatearDinero(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE53935),
        )
        IconButton(onClick = onBorrar, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Borrar gasto",
                tint = Color(0xFFE53935),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}