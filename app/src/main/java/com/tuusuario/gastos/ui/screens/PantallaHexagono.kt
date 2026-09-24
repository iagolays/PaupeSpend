package com.tuusuario.gastos.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuusuario.gastos.data.Repositorio
import com.tuusuario.gastos.ui.components.AroRadar
import com.tuusuario.gastos.ui.components.Encabezado
import com.tuusuario.gastos.ui.components.GraficoRadial
import com.tuusuario.gastos.util.aColor
import com.tuusuario.gastos.util.formatearDinero
import com.tuusuario.gastos.util.primeroDelMes
import com.tuusuario.gastos.util.ultimoDelMes
import java.time.LocalDate

@Composable
fun PantallaHexagono(onAtras: () -> Unit) {
    val hoy = remember { LocalDate.now() }

    val categorias by Repositorio.categorias.observarTodas().collectAsState(initial = emptyList())
    val totales by Repositorio.gastos
        .observarTotalesPorCategoria(hoy.primeroDelMes, hoy.ultimoDelMes)
        .collectAsState(initial = emptyList())

    val aros = totales
        .filter { it.categoriaId != null && it.total > 0 }
        .sortedByDescending { it.total }
        .mapNotNull { t ->
            val cat = categorias.find { it.id == t.categoriaId } ?: return@mapNotNull null
            AroRadar(cat.nombre, t.total.toFloat(), cat.colorHex.aColor())
        }

    Scaffold(
        topBar = { Encabezado(titulo = "Grafico radial por categoria", onAtras = onAtras) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Hacia donde se inclinan tus gastos de ${hoy.month.name.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (aros.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Todavia no hay gastos este mes")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Anade gastos y aparecera aqui la distribucion por categoria",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GraficoRadial(items = aros, radioMax = 140.dp)
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Desglose", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        aros.forEach { aro ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.size(14.dp).background(aro.color, CircleShape))
                                Spacer(Modifier.width(10.dp))
                                Text(aro.etiqueta, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(aro.valor.toDouble().formatearDinero(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}