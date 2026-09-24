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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuusuario.gastos.data.Gasto
import com.tuusuario.gastos.data.MetodoPago
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.ui.components.Encabezado
import com.tuusuario.gastos.util.Notificaciones
import com.tuusuario.gastos.util.formatear
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAnadirGasto(esFijoInicial: Boolean, onAtras: () -> Unit) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val categorias by Repositorio.categorias.observarTodas().collectAsState(initial = emptyList())

    var descripcion by remember { mutableStateOf("") }
    var montoTexto by remember { mutableStateOf("") }
    var metodo by remember { mutableStateOf(MetodoPago.TARJETA) }
    var categoriaId by remember { mutableStateOf<Long?>(null) }
    var fecha by remember { mutableStateOf(LocalDate.now()) }
    var esFijo by remember { mutableStateOf(esFijoInicial) }
    var mostrarFecha by remember { mutableStateOf(false) }
    var mostrandoCategorias by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    val monto = montoTexto.replace(",", ".").toDoubleOrNull()
    val valido = (monto != null) && monto > 0 && descripcion.isNotBlank()

    Scaffold(
        topBar = {
            Encabezado(
                titulo = if (esFijo) "Nuevo gasto fijo" else "Nuevo gasto",
                onAtras = onAtras,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripcion") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = montoTexto,
                onValueChange = { montoTexto = it },
                label = { Text("Importe (€)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = { mostrarFecha = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Today, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Fecha: ${fecha.formatear()}")
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

            Box {
                OutlinedButton(
                    onClick = { mostrandoCategorias = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val seleccionada = categorias.firstOrNull { it.id == categoriaId }
                    Text(seleccionada?.let { "${it.icono} ${it.nombre}" } ?: "Sin categoria")
                }
                DropdownMenu(
                    expanded = mostrandoCategorias,
                    onDismissRequest = { mostrandoCategorias = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Sin categoria") },
                        onClick = { categoriaId = null; mostrandoCategorias = false },
                    )
                    categorias.forEach { c ->
                        DropdownMenuItem(
                            text = { Text("${c.icono} ${c.nombre}") },
                            onClick = { categoriaId = c.id; mostrandoCategorias = false },
                        )
                    }
                }
            }

            if (esFijoInicial) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = esFijo, onCheckedChange = { esFijo = it })
                    Text("Marcar como gasto fijo (se muestra en el apartado de fijos)")
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val importe = monto ?: return@Button
                    if (guardando) return@Button
                    guardando = true
                    scope.launch {
                        Repositorio.gastos.insertar(
                            Gasto(
                                descripcion = descripcion.trim(),
                                monto = importe,
                                fecha = fecha,
                                metodoPago = metodo,
                                categoriaId = categoriaId,
                                esFijo = esFijo,
                            )
                        )
                        Notificaciones.comprobarAhora(contexto)
                        onAtras()
                    }
                },
                enabled = valido && !guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar gasto")
            }
        }
    }

    if (mostrarFecha) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = fecha.toEpochDay() * 24L * 3600_000L,
        )
        DatePickerDialog(
            onDismissRequest = { mostrarFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let {
                        fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    mostrarFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarFecha = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}