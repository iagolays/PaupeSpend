package com.tuusuario.gastos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tuusuario.gastos.util.ItemTicketParseado
import com.tuusuario.gastos.util.SoporteTicket
import com.tuusuario.gastos.util.formatearDinero
import com.tuusuario.gastos.util.parsearTicket
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.guava.await
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun PantallaEscaner(onAtras: () -> Unit, onConfirmar: () -> Unit) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permitido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> permitido = concedido }

    var items by remember { mutableStateOf<List<ItemTicketParseado>>(emptyList()) }
    var procesando by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(contexto).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    val reconocedor = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val analizador = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    val preview = remember { Preview.Builder().build() }
    val handler = remember { Handler(Looper.getMainLooper()) }

    var ultimoProxy by remember { mutableLongStateOf(0L) }

    fun analizar(proxy: ImageProxy) {
        val imagen = proxy.image
        val ahora = SystemClock.uptimeMillis()
        if (imagen == null || ahora - ultimoProxy < 700L) {
            proxy.close()
            return
        }
        ultimoProxy = ahora
        val input = InputImage.fromMediaImage(imagen, proxy.imageInfo.rotationDegrees)
        reconocedor.process(input)
            .addOnSuccessListener { resultado ->
                val texto = resultado.text
                val parseado = parsearTicket(texto)
                if (parseado.isNotEmpty()) {
                    SoporteTicket.items = parseado
                    SoporteTicket.textoBruto = texto
                    handler.post {
                        items = parseado
                        procesando = false
                    }
                }
            }
            .addOnCompleteListener { proxy.close() }
    }

    LaunchedEffect(permitido) {
        if (!permitido) return@LaunchedEffect
        procesando = true
        try {
            val proveedor = ProcessCameraProvider.getInstance(contexto).await()
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            preview.setSurfaceProvider(previewView.surfaceProvider)
            analizador.setAnalyzer(ejecutor) { analizar(it) }
            proveedor.unbindAll()
            proveedor.bindToLifecycle(lifecycleOwner, selector, preview, analizador)
        } catch (e: Exception) {
            // Sin camara o sin permisos: se muestra la tarjeta de error.
        }
        awaitCancellation()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (permitido) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Se necesita la camara para escanear tickets")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { pedirPermiso.launch(Manifest.permission.CAMERA) }) {
                        Text("Permitir camara")
                    }
                }
            }
        }

        // Cabecera flotante.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color.Black.copy(alpha = 0.35f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onAtras) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White,
                )
            }
            Text(
                "Escanea tu ticket",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // Panel inferior con lo reconocido.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Aparece un ticket en el encuadre para analizarlo",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    items = emptyList()
                    SoporteTicket.items = emptyList()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Limpiar", tint = Color.White)
                }
            }

            if (items.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(items) { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.descripcion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    item.monto.formatearDinero(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onConfirmar,
                enabled = items.isNotEmpty() && !procesando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(if (items.isEmpty()) "Analizando..." else "Revisar y guardar ${items.size} gastos")
            }
        }
    }
}