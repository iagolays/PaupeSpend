package com.tuusuario.gastos.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tuusuario.gastos.data.Categoria
import com.tuusuario.gastos.data.Presupuesto
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.ui.components.Encabezado
import com.tuusuario.gastos.util.aColor
import com.tuusuario.gastos.util.formatearDinero
import com.tuusuario.gastos.util.primeroDelMes
import com.tuusuario.gastos.util.ultimoDelMes
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun PantallaPresupuestos(onAtras: () -> Unit) {
    val hoy = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()

    val categorias by Repositorio.categorias.observarTodas().collectAsState(initial = emptyList())
    val presus by Repositorio.presupuestos.observarDelMes(hoy.monthValue, hoy.year).collectAsState(initial = emptyList())
    val totalGastado by Repositorio.gastos
        .observarTotalEntre(hoy.primeroDelMes, hoy.ultimoDelMes)
        .collectAsState(initial = 0.0)
    val totalesPorCategoria by Repositorio.gastos
        .observarTotalesPorCategoria(hoy.primeroDelMes, hoy.ultimoDelMes)
        .collectAsState(initial = emptyList())

    var nuevoDialogo by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Presupuesto?>(null) }

    val nombreCategoria = categorias.associateBy { it.id }

    Scaffold(
        topBar = { Encabezado(titulo = "Presupuestos", onAtras = onAtras) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nuevoDialogo = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo presupuesto")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Presupuestos de ${hoy.month.name.lowercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            items(presus, key = { "presu-${it.id}" }) { p ->
                val gastado = if (p.categoriaId == null) {
                    totalGastado
                } else {
                    totalesPorCategoria.firstOrNull { it.categoriaId == p.categoriaId }?.total ?: 0.0
                }
                val cat = p.categoriaId?.let { nombreCategoria[it] }
                TarjetaPresupuesto(
                    nombre = p.categoriaId?.let { cat?.nombre ?: "Categoria" } ?: "General",
                    icono = cat?.icono ?: "\uD83D\uDCCA",
                    color = cat?.colorHex?.aColor() ?: Color(0xFF6C5CE7),
                    gastado = gastado,
                    limite = p.limite,
                    notificar = p.notificar,
                    onNotificarChange = { avisar ->
                        scope.launch { Repositorio.presupuestos.actualizar(p.copy(notificar = avisar)) }
                    },
                    onClick = { editando = p },
                )
            }

            if (presus.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("No hay presupuestos este mes")
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Pulsa + para crear un limite global o por categoria",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { nuevoDialogo = true }) { Text("Crear presupuesto") }
                        }
                    }
                }
            }
        }
    }

    if (nuevoDialogo) {
        DialogoPresupuesto(
            categorias = categorias,
            esGeneral = false,
            categoriaInicial = null,
            onGuardar = { nuevo ->
                nuevoDialogo = false
                scope.launch { Repositorio.presupuestos.insertar(nuevo) }
            },
            onBorrar = null,
            onCerrar = { nuevoDialogo = false },
        )
    }

    editando?.let { p ->
        DialogoPresupuesto(
            categorias = categorias,
            esGeneral = p.categoriaId == null,
            categoriaInicial = p.categoriaId?.let { nombreCategoria[it] },
            presupuesto = p,
            onGuardar = { actualizado ->
                editando = null
                scope.launch { Repositorio.presupuestos.actualizar(actualizado) }
            },
            onBorrar = {
                editando = null
                scope.launch { Repositorio.presupuestos.borrar(it) }
            },
            onCerrar = { editando = null },
        )
    }
}

@Composable
private fun TarjetaPresupuesto(
    nombre: String,
    icono: String,
    color: Color,
    gastado: Double,
    limite: Double,
    notificar: Boolean,
    onNotificarChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val porcentaje = if (limite <= 0) 0f else (gastado / limite).toFloat().coerceIn(0f, 1f)
    val restante = (limite - gastado).coerceAtLeast(0.0)
    val encima = gastado > limite
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$icono ", style = MaterialTheme.typography.titleMedium)
                Text(nombre, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                BotonNotify(encendido = notificar, onCambiar = onNotificarChange)
                Spacer(Modifier.width(8.dp))
                if (encima) {
                    Text("Sobrepasado", color = Color(0xFFE17055), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text("Quedan ${restante.formatearDinero()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { porcentaje },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = if (encima) Color(0xFFE17055) else color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${gastado.formatearDinero()} de ${limite.formatearDinero()} (${(porcentaje * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Campana de notificacion estilo Bencho: pastilla con la campana a la izquierda
 * y la etiqueta a la derecha. Al activarla, la campana oscila suavemente pivoteando
 * desde su corona (50% 16%) durante 820ms con amortiguacion (0 > -17 > 14 > -9 > 6 > -3 > 0).
 * Solo suena cuando se enciende; la pastilla cambia de ancho segun la etiqueta.
 */
@Composable
private fun BotonNotify(
    encendido: Boolean,
    onCambiar: (Boolean) -> Unit,
) {
    var estado by remember { mutableStateOf(encendido) }
    val rotacion = remember { Animatable(0f) }

    LaunchedEffect(estado) {
        if (estado) {
            rotacion.snapTo(-17f)
            rotacion.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 820
                    -17f at 90
                    14f at 221
                    -9f at 360
                    6f at 500
                    -3f at 640
                    0f at 820
                },
            )
        }
    }

    Surface(
        onClick = {
            estado = !estado
            onCambiar(estado)
        },
        shape = RoundedCornerShape(50),
        color = if (estado) Color(0xFF2D2D44) else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (estado) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                contentDescription = if (estado) "Desactivar aviso" else "Activar aviso",
                tint = if (estado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        rotationZ = rotacion.value
                        transformOrigin = TransformOrigin(0.5f, 0.16f)
                    },
            )
            if (estado) {
                Spacer(Modifier.width(4.dp))
                Text("Avisar", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }
    }
}

@Composable
private fun DialogoPresupuesto(
    categorias: List<Categoria>,
    esGeneral: Boolean,
    categoriaInicial: Categoria?,
    presupuesto: Presupuesto? = null,
    onGuardar: (Presupuesto) -> Unit,
    onBorrar: ((Presupuesto) -> Unit)?,
    onCerrar: () -> Unit,
) {
    val hoy = remember { LocalDate.now() }
    var esGeneralState by remember { mutableStateOf(esGeneral) }
    var categoriaId by remember { mutableStateOf(categoriaInicial?.id) }
    var limiteTexto by remember { mutableStateOf(presupuesto?.limite?.let { "%.0f".format(it) } ?: "") }
    var mostrandoCategorias by remember { mutableStateOf(false) }

    val limite = limiteTexto.replace(",", ".").toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (presupuesto == null) "Nuevo presupuesto" else "Editar presupuesto", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = esGeneralState,
                        onClick = { esGeneralState = true },
                        label = { Text("General") },
                    )
                    FilterChip(
                        selected = !esGeneralState,
                        onClick = { esGeneralState = false },
                        label = { Text("Por categoria") },
                    )
                }

                if (!esGeneralState) {
                    Box {
                        OutlinedButton(
                            onClick = { mostrandoCategorias = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val seleccionada = categorias.firstOrNull { it.id == categoriaId }
                            Text(seleccionada?.let { "${it.icono} ${it.nombre}" } ?: "Elegir categoria")
                        }
                        DropdownMenu(expanded = mostrandoCategorias, onDismissRequest = { mostrandoCategorias = false }) {
                            categorias.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.icono} ${c.nombre}") },
                                    onClick = { categoriaId = c.id; mostrandoCategorias = false },
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limiteTexto,
                    onValueChange = { limiteTexto = it },
                    label = { Text("Limite mensual (€)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val importe = limite ?: return@TextButton
                    if (!esGeneralState && categoriaId == null) return@TextButton
                    onGuardar(
                        (presupuesto ?: Presupuesto(limite = 0.0, categoriaId = null, mes = hoy.monthValue, anio = hoy.year)).copy(
                            categoriaId = if (esGeneralState) null else categoriaId,
                            limite = importe,
                        )
                    )
                },
                enabled = (limite != null) && (esGeneralState || categoriaId != null),
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                presupuesto?.let { p ->
                    onBorrar?.let { borrar ->
                        TextButton(onClick = { borrar(p) }) { Text("Borrar") }
                    }
                }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        },
    )
}