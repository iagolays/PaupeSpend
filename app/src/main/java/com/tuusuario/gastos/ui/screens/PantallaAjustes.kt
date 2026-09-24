package com.tuusuario.gastos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuusuario.gastos.ui.components.Encabezado
import com.tuusuario.gastos.util.Notificaciones

@Composable
fun PantallaAjustes(onAtras: () -> Unit) {
    Scaffold(
        topBar = { Encabezado(titulo = "Ajustes", onAtras = onAtras) }
    ) { padding ->
        val contexto = LocalContext.current
        val actividad = contexto as? ComponentActivity
        val permisosAceptados =
            Build.VERSION.SDK_INT < 33 ||
                contexto.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        val pedirPermiso = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { concedido ->
            if (concedido) {
                Notificaciones.notificar(contexto, "Notificacion de prueba", "PaupeSpend te puede avisar cuando un presupuesto este cerca de agotarse.")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Aqui iran mas ajustes proximamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF6C5CE7),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Avisos de presupuesto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Cada dia a las 21:00 (y al abrir la app) se revisan los presupuestos. Si uno con la campana activada ha consumido el 90% o mas, llegara un aviso:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "\u2022  90-99%: \u201CPresupuesto casi agotado\u201D\n" +
                            "\u2022  100% o mas: \u201CPresupuesto superado\u201D",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Activa la campana en cada presupuesto desde la pantalla Presupuestos y permite las notificaciones del sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (permisosAceptados) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = if (permisosAceptados) Color(0xFF00B894) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (permisosAceptados) "Notificaciones permitidas" else "Notificaciones bloqueadas",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && !permisosAceptados) {
                        actividad?.let { pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    } else {
                        Notificaciones.notificar(
                            contexto,
                            "Notificacion de prueba",
                            "PaupeSpend te puede avisar cuando un presupuesto este cerca de agotarse.",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enviar notificacion de prueba")
            }
        }
    }
}