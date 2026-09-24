package com.tuusuario.gastos.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import com.tuusuario.gastos.ui.components.Encabezado
import com.tuusuario.gastos.util.Excel
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun PantallaReporte(onAtras: () -> Unit) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    var mensaje by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    val colorOk = Color(0xFF00B894)
    val colorError = Color(0xFFE17055)

    val exportarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(Excel.MIME)
    ) { uri ->
        if (uri != null) {
            ocupado = true
            scope.launch {
                mensaje = try {
                    Excel.exportar(contexto, uri)
                    "Exportado correctamente"
                } catch (e: Exception) {
                    "Error al exportar: ${e.message}"
                }
                ocupado = false
            }
        }
    }

    val importarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            ocupado = true
            scope.launch {
                mensaje = try {
                    val n = Excel.importar(contexto, uri)
                    "Importados $n registros"
                } catch (e: Exception) {
                    "Error al importar: ${e.message}"
                }
                ocupado = false
            }
        }
    }

    Scaffold(
        topBar = { Encabezado(titulo = "Exportar / Importar Excel", onAtras = onAtras) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Exportar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Genera un fichero .xlsx con tus categorias, gastos, ingresos, suscripciones y presupuestos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val nombre = "PaupeSpend-${LocalDate.now()}.xlsx"
                            exportarLauncher.launch(nombre)
                        },
                        enabled = !ocupado,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text("   Guardar Excel (exportar)")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Importar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Lee un .xlsx exportado por la app (mismo formato de columnas) y anade los registros a la base.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { importarLauncher.launch(arrayOf(Excel.MIME, "*/*")) },
                        enabled = !ocupado,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Text("   Abrir Excel (importar)")
                    }
                }
            }

            when {
                ocupado -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
                mensaje != null -> {
                    val esError = mensaje!!.startsWith("Error")
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            mensaje!!,
                            modifier = Modifier.padding(16.dp),
                            color = if (esError) colorError else colorOk,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}