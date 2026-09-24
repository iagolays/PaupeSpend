package com.tuusuario.gastos.util

import com.tuusuario.gastos.data.Categoria
import java.util.Locale

/**
 * Resultado de una linea de ticket: descripcion libre y un importe.
 */
data class ItemTicketParseado(
    var descripcion: String,
    var monto: Double = 0.0,
    var categoriaId: Long? = null,
)

/**
 * Soporte en memoria para pasar los items reconocidos por OCR desde
 * la pantalla del escaner hasta el formulario de confirmacion.
 */
object SoporteTicket {
    var items: List<ItemTicketParseado> = emptyList()
    var textoBruto: String = ""
}

/**
 * Heuristicas sencillas para convertir el texto que devuelve ML Kit
 * en una lista de (descripcion, importe).
 *
 * Se quedan con las lineas que contienen un importe en formato
 * espanol (1.234,56 / 12,34) y una descripcion con letras. La linea
 * que parece el TOTAL se convierte en el ultimo gasto y sirve como
 * comprobacion.
 */
fun parsearTicket(texto: String): List<ItemTicketParseado> {
    val lineas = texto.lines().map { it.trim() }.filter { it.isNotBlank() }
    val patronMonto =
        Regex("""(?<!\d)\d{1,3}(?:[.,]\d{3})*[.,]\d{2}\s*[€$]?""")
    val items = mutableListOf<ItemTicketParseado>()
    var encontradoTotal = false

    for (linea in lineas) {
        val limpiada = linea.replace(Regex("""[A-Z]\d{8}[A-Z]|\bCIF\b|^\d+\s*$"""), "").trim()
        if (limpiada.isBlank()) continue

        val esTotal = limpiada.contains(Regex("""(?i)\b(TOTAL|IMPORTE|EFECTIVO|CONTADO|TARJETA|ENTREGA|DEBE)\b"""))
        val coincidencia = patronMonto.find(limpiada) ?: continue

        val importe = importeDe(coincidencia.value) ?: continue
        val descripcion = limpiada
            .replace(coincidencia.value, "")
            .replace(Regex("""(?i)\b(TOTAL|IMPORTE|EFECTIVO|CONTADO|TARJETA|ENTREGA|DEBE)\b"""), "")
            .trim(' ', ':', '.')
            .trim()

        if (importe <= 0.0) continue

        val claves = descripcion.lowercase(Locale.ROOT)
        val hayLetra = claves.any { it in 'a'..'z' }

        if (esTotal) {
            encontradoTotal = true
        }

        // Evita duplicados exactos y lineas solo-numero.
        if (!hayLetra && descripcion.isBlank()) continue

        val yaExiste = items.any {
            it.descripcion.equals(descripcion, true) && kotlin.math.abs(it.monto - importe) < 0.01
        }
        if (!yaExiste) {
            items.add(ItemTicketParseado(descripcion.ifBlank { "Compra" }, importe))
        }
    }

    // Sin descripciones validas pero con un importe unico: un solo gasto.
    if (items.isEmpty()) {
        lineas.forEach { linea ->
            patronMonto.find(linea)?.let { m ->
                importeDe(m.value)?.let { importe ->
                    if (importe > 0.0 && items.none { kotlin.math.abs(it.monto - importe) < 0.01 }) {
                        items.add(ItemTicketParseado("Compra", importe))
                    }
                }
            }
        }
    }

    return items.take(30)
}

private fun importeDe(txt: String): Double? {
    val numero = txt.filter { it.isDigit() || it == ',' || it == '.' }
    return try {
        when {
            numero.contains(",") && numero.contains(".") ->
                // 1.234,56 o 1,234.56
                if (numero.indexOf(',') > numero.indexOf('.'))
                    numero.replace(".", "").replace(",", ".").toDouble()
                else
                    numero.replace(",", "").toDouble()
            numero.contains(",") -> numero.replace(",", ".").toDouble()
            numero.contains(".") -> numero.toDouble()
            else -> numero.toDoubleOrNull()
        }
    } catch (e: NumberFormatException) {
        null
    }
}

/**
 * Clasificacion automatica por palabras clave: intenta asignar el
 * gasto a la categoria existente cuyo nombre coincida mejor con el
 * texto del item.
 */
fun clasificarEnCategoria(item: ItemTicketParseado, categorias: List<Categoria>): Long? {
    val texto = item.descripcion.lowercase(Locale.ROOT)
    val sinonimos = mapOf(
        "comida" to listOf("mercado", "supermercado", "hipermercado", "alimentacion", "pan", "fruta", "carn", "pesc"),
        "transporte" to listOf("gasolina", "combustible", "repsol", "cepsa", "carburante", "gasolinera", "autobus", "metro", "tren", "uber", "cabify", "taxi"),
        "ocio" to listOf("cine", "netflix", "spotify", "videojuego", "restaurante", "bar", "cafe", "concierto", "steam", "hbo", "disney"),
        "salud" to listOf("farmacia", "parafarmacia", "medicamento", "doctor", "clinica", "drogueria"),
        "hogar" to listOf("alquiler", "luz", "agua", "internet", "movistar", "orange", "vodafone", "ikea", "ferreteria"),
    )
    categorias.forEach { c ->
        if (texto.contains(c.nombre.lowercase(Locale.ROOT))) return c.id
        val sins = sinonimos[c.nombre.lowercase(Locale.ROOT)] ?: emptyList()
        if (sins.any { texto.contains(it) }) return c.id
    }
    return null
}