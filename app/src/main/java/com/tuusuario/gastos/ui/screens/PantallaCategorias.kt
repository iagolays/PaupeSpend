package com.tuusuario.gastos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.tuusuario.gastos.data.Categoria
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.ui.components.BurbujaCategoria
import com.tuusuario.gastos.ui.components.Encabezado
import com.tuusuario.gastos.util.aColor
import com.tuusuario.gastos.util.formatearDinero
import com.tuusuario.gastos.util.primeroDelMes
import com.tuusuario.gastos.util.ultimoDelMes
import kotlinx.coroutines.launch
import java.time.LocalDate

private val COLORES = listOf(
    "#FF6B6B", "#00B894", "#0984E3", "#FDCB6E", "#E17055",
    "#6C5CE7", "#00CEC9", "#FD79A8", "#636E72", "#E84393",
)

private val ICONOS = listOf(
    "\uD83C\uDF54", "\uD83D\uDE97", "\uD83C\uDFAE", "\uD83C\uDFE0", "\uD83D\uDC8A",
    "\uD83D\uDCE6", "\u2615", "\uD83C\uDFB8", "\uD83C\uDFC3", "\uD83D\uDC84",
)

@Composable
fun PantallaCategorias(onAtras: () -> Unit) {
    val hoy = remember { LocalDate.now() }
    val categorias by Repositorio.categorias.observarTodas().collectAsState(initial = emptyList())
    val totales by Repositorio.gastos
        .observarTotalesPorCategoria(hoy.primeroDelMes, hoy.ultimoDelMes)
        .collectAsState(initial = emptyList())

    var editando by remember { mutableStateOf<Categoria?>(null) }
    var creando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { Encabezado(titulo = "Categorias", onAtras = onAtras) },
        floatingActionButton = {
            FloatingActionButton(onClick = { creando = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir categoria")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(categorias, key = { it.id }) { cat ->
                val gastado = totales.firstOrNull { it.categoriaId == cat.id }?.total ?: 0.0
                Card(modifier = Modifier.fillMaxWidth().clickable { editando = cat }) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BurbujaCategoria(icono = cat.icono, color = cat.colorHex.aColor())
                        androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.nombre, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                            val limite = cat.limiteMensual
                            val texto = if (limite != null) {
                                "Este mes: ${gastado.formatearDinero()} de ${limite.formatearDinero()}"
                            } else {
                                "Este mes: ${gastado.formatearDinero()}"
                            }
                            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { scope.launch { Repositorio.categorias.borrar(cat) } }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Borrar categoria",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Box(Modifier.size(14.dp).background(cat.colorHex.aColor(), CircleShape))
                    }
                }
            }
        }
    }

    if (creando) {
        DialogoCategoria(
            categoria = null,
            onGuardar = { nueva ->
                creando = false
                scope.launch { Repositorio.categorias.insertar(nueva) }
            },
            onBorrar = { creando = false },
            onCerrar = { creando = false },
        )
    }

    editando?.let { cat ->
        DialogoCategoria(
            categoria = cat,
            onGuardar = { actualizado ->
                editando = null
                scope.launch { Repositorio.categorias.actualizar(actualizado) }
            },
            onBorrar = { borrar ->
                editando = null
                scope.launch { Repositorio.categorias.borrar(borrar) }
            },
            onCerrar = { editando = null },
        )
    }
}

@Composable
private fun DialogoCategoria(
    categoria: Categoria?,
    onGuardar: (Categoria) -> Unit,
    onBorrar: (Categoria) -> Unit,
    onCerrar: () -> Unit,
) {
    var nombre by remember { mutableStateOf(categoria?.nombre ?: "") }
    var icono by remember { mutableStateOf(categoria?.icono ?: ICONOS[0]) }
    var color by remember { mutableStateOf(categoria?.colorHex ?: COLORES[0]) }
    var limiteTexto by remember { mutableStateOf(categoria?.limiteMensual?.let { "%.0f".format(it) } ?: "") }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (categoria == null) "Nueva categoria" else "Editar categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ICONOS.take(5).forEach { i ->
                        Text(
                            i,
                            modifier = Modifier
                                .background(if (i == icono) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { icono = i }
                                .padding(6.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ICONOS.drop(5).forEach { i ->
                        Text(
                            i,
                            modifier = Modifier
                                .background(if (i == icono) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { icono = i }
                                .padding(6.dp),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLORES.take(5).forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(c.aColor(), CircleShape)
                                .clickable { color = c }
                                .padding(0.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLORES.drop(5).take(5).forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(c.aColor(), CircleShape)
                                .clickable { color = c }
                                .padding(0.dp),
                        )
                    }
                }

                OutlinedTextField(
                    value = limiteTexto,
                    onValueChange = { limiteTexto = it },
                    label = { Text("Limite mensual opcional (€)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.isBlank()) return@TextButton
                    val limite = limiteTexto.replace(",", ".").toDoubleOrNull()
                    val base = categoria ?: Categoria(nombre = "", icono = "", colorHex = "")
                    onGuardar(
                        base.copy(
                            nombre = nombre.trim(),
                            icono = icono,
                            colorHex = color,
                            limiteMensual = limite,
                        )
                    )
                },
                enabled = nombre.isNotBlank(),
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (categoria != null) {
                    TextButton(onClick = { onBorrar(categoria) }) { Text("Borrar") }
                }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        },
    )
}