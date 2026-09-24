package com.tuusuario.gastos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuusuario.gastos.data.Ingreso
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.util.formatear
import com.tuusuario.gastos.ui.components.Encabezado
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAnadirIngreso(onAtras: () -> Unit) {
    val scope = rememberCoroutineScope()
    val contexto = LocalContext.current

    var descripcion by remember { mutableStateOf("") }
    var montoTexto by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(LocalDate.now()) }
    var esRecurrente by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var mostrarFecha by remember { mutableStateOf(false) }

    val monto = montoTexto.replace(",", ".").toDoubleOrNull()
    val valido = (monto != null) && monto > 0 && descripcion.isNotBlank()

    Scaffold(
        topBar = { Encabezado(titulo = "Nuevo ingreso", onAtras = onAtras) }
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
                label = { Text("Concepto (beca, paga...)") },
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
                Text("   Fecha: ${fecha.formatear()}")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = esRecurrente, onCheckedChange = { esRecurrente = it })
                Text("Ingreso recurrente (nómina, beca fija...)")
            }

            Button(
                onClick = {
                    val importe = monto ?: return@Button
                    if (guardando) return@Button
                    guardando = true
                    scope.launch {
                        Repositorio.ingresos.insertar(
                            Ingreso(
                                descripcion = descripcion.trim(),
                                monto = importe,
                                fecha = fecha,
                                esRecurrente = esRecurrente,
                            )
                        )
                        onAtras()
                    }
                },
                enabled = valido && !guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar ingreso")
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