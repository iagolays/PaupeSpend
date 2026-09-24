package com.tuusuario.gastos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuusuario.gastos.data.Gasto
import com.tuusuario.gastos.data.MetodoPago
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.ui.components.Encabezado
import com.tuusuario.gastos.util.Notificaciones
import com.tuusuario.gastos.util.SoporteTicket
import com.tuusuario.gastos.util.clasificarEnCategoria
import com.tuusuario.gastos.util.formatear
import com.tuusuario.gastos.util.formatearDinero
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

private data class Editable(
    var descripcion: String,
    var montoTexto: String,
    var categoriaId: Long?,
)

@Composable
fun PantallaConfirmarTicket(onGuardado: () -> Unit, onAtras: () -> Unit) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val categorias by Repositorio.categorias.observarTodas().collectAsState(initial = emptyList())

    val items by remember {
        mutableStateOf(
            SoporteTicket.items.map {
                Editable(
                    descripcion = it.descripcion,
                    montoTexto = "%.2f".format(it.monto),
                    categoriaId = it.categoriaId ?: clasificarEnCategoria(it, categorias),
                )
            }
        )
    }
    var metodo by remember { mutableStateOf(MetodoPago.TARJETA) }
    var fecha by remember { mutableStateOf(LocalDate.now()) }
    var guardando by remember { mutableStateOf(false) }

    val total = items.sumOf { it.montoTexto.replace(",", ".").toDoubleOrNull() ?: 0.0 }

    Scaffold(
        topBar = { Encabezado(titulo = "Confirmar ticket", onAtras = onAtras) }
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No hay items escaneados")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onAtras) { Text("Volver") }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Revisa lo que ha reconocido el OCR antes de guardarlo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            items.forEachIndexed { indice, item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = item.descripcion,
                                onValueChange = { item.descripcion = it },
                                label = { Text("Descripcion") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = item.montoTexto,
                                onValueChange = { item.montoTexto = it },
                                label = { Text("Importe") },
                                singleLine = true,
                                modifier = Modifier.width(110.dp),
                            )
                        }
                        SelectorCategoria(
                            categorias = categorias,
                            seleccionada = item.categoriaId,
                            onSeleccionar = { item.categoriaId = it },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = metodo == MetodoPago.EFECTIVO,
                    onClick = { metodo = MetodoPago.EFECTIVO },
                    label = { Text("Efectivo") },
                )
                FilterChip(
                    selected = metodo == MetodoPago.TARJETA,
                    onClick = { metodo = MetodoPago.TARJETA },
                    label = { Text("Tarjeta") },
                )
            }

            Text("Fecha de compra: ${fecha.formatear()}", style = MaterialTheme.typography.bodySmall)

            val validos = items.count {
                (it.montoTexto.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0
            }

            Button(
                onClick = {
                    if (guardando) return@Button
                    guardando = true
                    val hoy = fecha
                    val aGuardar = items.filter {
                        (it.montoTexto.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0
                    }
                    scope.launch {
                        aGuardar.forEach { e ->
                            Repositorio.gastos.insertar(
                                Gasto(
                                    descripcion = e.descripcion.ifBlank { "Ticket" }.trim(),
                                    monto = e.montoTexto.replace(",", ".").toDouble(),
                                    fecha = hoy,
                                    metodoPago = metodo,
                                    categoriaId = e.categoriaId,
                                )
                            )
                        }
                        SoporteTicket.items = emptyList()
                        SoporteTicket.textoBruto = ""
                        Notificaciones.comprobarAhora(contexto)
                        onGuardado()
                    }
                },
                enabled = validos > 0 && !guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Guardar $validos gasto${if (validos == 1) "" else "s"} (${total.formatearDinero()})"
                        .lowercase(Locale.ROOT)
                        .replaceFirstChar { it.uppercase() }
                )
            }
        }
    }
}

@Composable
private fun SelectorCategoria(
    categorias: List<com.tuusuario.gastos.data.Categoria>,
    seleccionada: Long?,
    onSeleccionar: (Long?) -> Unit,
) {
    var desplegado by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { desplegado = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            val cat = categorias.firstOrNull { it.id == seleccionada }
            Text(cat?.let { "${it.icono} ${it.nombre}" } ?: "Sin categoria")
        }
        DropdownMenu(expanded = desplegado, onDismissRequest = { desplegado = false }) {
            DropdownMenuItem(
                text = { Text("Sin categoria") },
                onClick = { onSeleccionar(null); desplegado = false },
            )
            categorias.forEach { c ->
                DropdownMenuItem(
                    text = { Text("${c.icono} ${c.nombre}") },
                    onClick = { onSeleccionar(c.id); desplegado = false },
                )
            }
        }
    }
}