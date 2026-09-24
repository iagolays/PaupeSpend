package com.tuusuario.gastos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import com.tuusuario.gastos.data.Categoria
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.data.Suscripcion
import com.tuusuario.gastos.ui.components.Encabezado
import com.tuusuario.gastos.util.aColor
import com.tuusuario.gastos.util.formatear
import com.tuusuario.gastos.util.formatearDinero
import kotlinx.coroutines.launch

@Composable
fun PantallaSuscripciones(onAtras: () -> Unit, onAnadirFijo: () -> Unit) {
    val scope = rememberCoroutineScope()
    val suscripciones by Repositorio.suscripciones.observarActivas().collectAsState(initial = emptyList())
    val gastosFijos by Repositorio.gastos.observarFijos().collectAsState(initial = emptyList())
    val categorias by Repositorio.categorias.observarTodas().collectAsState(initial = emptyList())

    var crearDialogo by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Suscripcion?>(null) }

    Scaffold(
        topBar = { Encabezado(titulo = "Fijos y suscripciones", onAtras = onAtras) },
        floatingActionButton = {
            FloatingActionButton(onClick = { crearDialogo = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva suscripcion")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { TituloSeccion("Suscripciones (Netflix, Spotify...)") }
            items(suscripciones, key = { "susc-${it.id}" }) { s ->
                val cat = categorias.firstOrNull { it.id == s.categoriaId }
                FilaSuscripcion(
                    nombre = s.nombre,
                    monto = s.monto,
                    diaDeCobro = s.diaDeCobro,
                    icono = cat?.icono ?: "\uD83D\uDD11",
                    color = cat?.colorHex?.aColor() ?: Color(0xFF0984E3),
                    activa = s.activa,
                    onToggle = { activa ->
                        scope.launch { Repositorio.suscripciones.actualizar(s.copy(activa = activa)) }
                    },
                    onBorrar = { scope.launch { Repositorio.suscripciones.actualizar(s.copy(activa = false)) } },
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
            item { TituloSeccion("Gastos fijos (alquiler, recibos...)") }
            if (gastosFijos.isEmpty()) {
                item {
                    Text("No hay gastos fijos anadidos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(gastosFijos, key = { "fijo-${it.id}" }) { g ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFFFDCB6E),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(g.descripcion, fontWeight = FontWeight.Medium)
                            Text(
                                "${g.metodoPago.name.lowercase()} · ${g.fecha.formatear()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(g.monto.formatearDinero(), fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { scope.launch { Repositorio.gastos.borrar(g) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color(0xFFE17055))
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onAnadirFijo,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Anadir gasto fijo")
                }
            }
        }
    }

    if (crearDialogo) {
        DialogoSuscripcion(
            suscripcion = null,
            categorias = categorias,
            onGuardar = { s ->
                crearDialogo = false
                scope.launch { Repositorio.suscripciones.insertar(s) }
            },
            onCerrar = { crearDialogo = false },
        )
    }

    editando?.let { s ->
        DialogoSuscripcion(
            suscripcion = s,
            categorias = categorias,
            onGuardar = { actualizada ->
                editando = null
                scope.launch { Repositorio.suscripciones.actualizar(actualizada) }
            },
            onCerrar = { editando = null },
        )
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
private fun FilaSuscripcion(
    nombre: String,
    monto: Double,
    diaDeCobro: Int,
    icono: String,
    color: Color,
activa: Boolean,
                    onToggle: (Boolean) -> Unit,
                    onBorrar: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icono, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nombre, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Se cobra el dia $diaDeCobro cada mes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(monto.formatearDinero(), fontWeight = FontWeight.SemiBold)
            Switch(checked = activa, onCheckedChange = onToggle)
            IconButton(onClick = onBorrar) {
                Icon(Icons.Default.Delete, contentDescription = "Desactivar", tint = Color(0xFFE17055))
            }
        }
    }
}

@Composable
private fun DialogoSuscripcion(
    suscripcion: Suscripcion?,
    categorias: List<Categoria>,
    onGuardar: (Suscripcion) -> Unit,
    onCerrar: () -> Unit,
) {
    var nombre by remember { mutableStateOf(suscripcion?.nombre ?: "") }
    var montoTexto by remember { mutableStateOf(suscripcion?.monto?.let { "%.0f".format(it) } ?: "") }
    var diaTexto by remember { mutableStateOf(suscripcion?.diaDeCobro?.toString() ?: "1") }
    var categoriaId by remember { mutableStateOf(suscripcion?.categoriaId) }
    var activa by remember { mutableStateOf(suscripcion?.activa ?: true) }
    var mostrandoCategorias by remember { mutableStateOf(false) }

    val monto = montoTexto.replace(",", ".").toDoubleOrNull()
    val dia = diaTexto.toIntOrNull()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (suscripcion == null) "Nueva suscripcion" else "Editar suscripcion") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (Netflix, Spotify...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = montoTexto,
                        onValueChange = { montoTexto = it },
                        label = { Text("Importe") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = diaTexto,
                        onValueChange = { diaTexto = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Dia del cobro") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Box {
                    OutlinedButton(onClick = { mostrandoCategorias = true }, modifier = Modifier.fillMaxWidth()) {
                        val cat = categorias.firstOrNull { it.id == categoriaId }
                        Text(cat?.let { "${it.icono} ${it.nombre}" } ?: "Sin categoria")
                    }
                    DropdownMenu(expanded = mostrandoCategorias, onDismissRequest = { mostrandoCategorias = false }) {
                        DropdownMenuItem(text = { Text("Sin categoria") }, onClick = { categoriaId = null; mostrandoCategorias = false })
                        categorias.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.icono} ${c.nombre}") },
                                onClick = { categoriaId = c.id; mostrandoCategorias = false },
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = activa, onCheckedChange = { activa = it })
                    Spacer(Modifier.width(10.dp))
                    Text("Activa", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val importe = monto ?: return@TextButton
                    val diaValido = (dia != null) && dia in 1..31
                    if (nombre.isBlank() || !diaValido) return@TextButton
                    onGuardar(
                        (suscripcion ?: Suscripcion(nombre = "", monto = 0.0, diaDeCobro = 1, categoriaId = null)).copy(
                            nombre = nombre.trim(),
                            monto = importe,
                            diaDeCobro = dia!!,
                            categoriaId = categoriaId,
                            activa = activa,
                        )
                    )
                },
                enabled = nombre.isNotBlank() && monto != null && (dia != null && dia in 1..31),
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        },
    )
}