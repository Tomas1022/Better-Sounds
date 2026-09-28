package com.example.bettersounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private const val FREC_MIN = 20f
private const val FREC_MAX = 20000f

// Posición horizontal de una frecuencia (escala logarítmica), de 0 a 1
private fun fraccionX(frecuencia: Int): Float {
    return (log10(frecuencia.toFloat()) - log10(FREC_MIN)) /
            (log10(FREC_MAX) - log10(FREC_MIN))
}

private fun yDeGanancia(ganancia: Float, alto: Float, margen: Float): Float =
    margen + (12f - ganancia) / 24f * (alto - 2 * margen)

// Convierte la posición del dedo en ganancia (-12 a +12), en pasos de 0.5 dB
private fun gananciaDeY(y: Float, alto: Float, margen: Float): Float {
    val g = 12f - (y - margen) / (alto - 2 * margen) * 24f
    return (g.coerceIn(-12f, 12f) * 2f).roundToInt() / 2f
}

private fun textoFrecuencia(f: Int): String {
    if (f < 1000) return "$f Hz"
    val k = f / 1000f
    return if (k % 1f == 0f) "${k.toInt()} kHz" else "$k kHz"
}

private fun textoDb(v: Float): String {
    val signo = if (v > 0f) "+" else ""
    val numero = if (v % 1f == 0f) v.toInt().toString() else v.toString()
    return "$signo$numero dB"
}

// Transición suave de 0 a 1 entre dos bordes
private fun suavizado(borde0: Float, borde1: Float, x: Float): Float {
    val t = ((x - borde0) / (borde1 - borde0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

// Aporte de Graves / Voz / Agudos a una frecuencia:
// Graves = repisa en la zona baja, Voz = campana cerca de 1 kHz, Agudos = repisa en la zona alta
private fun aporteMacros(frecuencia: Float, graves: Float, voz: Float, agudos: Float): Float {
    val x = log10(frecuencia)
    val bajo = 1f - suavizado(log10(80f), log10(600f), x)
    val alto = suavizado(log10(2500f), log10(9000f), x)
    val d = (x - log10(1000f)) / 0.3f
    val campana = exp(-0.5f * d * d)
    return graves * bajo + voz * campana + agudos * alto
}

// Ganancia de la línea de bandas (recta entre puntos) en la posición t (0 a 1)
private fun gananciaBandas(t: Float, xs: List<Float>, gs: List<Float>): Float {
    if (t <= xs.first()) return gs.first()
    if (t >= xs.last()) return gs.last()
    for (i in 0 until xs.size - 1) {
        if (t <= xs[i + 1]) {
            val u = (t - xs[i]) / (xs[i + 1] - xs[i])
            return gs[i] + (gs[i + 1] - gs[i]) * u
        }
    }
    return gs.last()
}

// Traza líneas rectas entre los puntos (sin suavizado)
private fun Path.trazarCurva(puntos: List<Offset>, ancho: Float) {
    lineTo(puntos.first().x, puntos.first().y)
    for (i in 1 until puntos.size) {
        lineTo(puntos[i].x, puntos[i].y)
    }
    lineTo(ancho, puntos.last().y)
}

@Composable
fun GraficaEQ(
    frecuencias: List<Int>,
    ganancias: List<Float>,
    graves: Float,
    voz: Float,
    agudos: Float,
    habilitado: Boolean,
    onCambio: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val primario = MaterialTheme.colorScheme.primary
    val colorLinea = if (habilitado) primario else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val colorGrid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val colorCero = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
    val colorTexto = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val colorFondo = MaterialTheme.colorScheme.surface

    var bandaActiva by remember { mutableIntStateOf(-1) }
    val alCambiar by rememberUpdatedState(onCambio)

    BoxWithConstraints(modifier = modifier) {
        val anchoTotal = maxWidth

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(habilitado) {
                            if (!habilitado) return@pointerInput
                            val margen = 16.dp.toPx()

                            fun aplicar(y: Float) {
                                if (bandaActiva >= 0) {
                                    alCambiar(
                                        bandaActiva,
                                        gananciaDeY(y, size.height.toFloat(), margen)
                                    )
                                }
                            }

                            detectDragGestures(
                                onDragStart = { toque ->
                                    // Agarra la banda más cercana en horizontal
                                    bandaActiva = frecuencias.indices.minByOrNull { i ->
                                        abs(fraccionX(frecuencias[i]) * size.width - toque.x)
                                    } ?: -1
                                    aplicar(toque.y)
                                },
                                onDrag = { cambio, _ ->
                                    cambio.consume()
                                    aplicar(cambio.position.y)
                                },
                                onDragEnd = { bandaActiva = -1 },
                                onDragCancel = { bandaActiva = -1 }
                            )
                        }
                ) {
                    val margen = 16.dp.toPx()
                    val ancho = size.width
                    val alto = size.height

                    // Líneas horizontales de referencia
                    listOf(12f, 6f, 0f, -6f, -12f).forEach { g ->
                        val y = yDeGanancia(g, alto, margen)
                        drawLine(
                            color = if (g == 0f) colorCero else colorGrid,
                            start = Offset(0f, y),
                            end = Offset(ancho, y),
                            strokeWidth = if (g == 0f) 1.5.dp.toPx() else 1.dp.toPx()
                        )
                    }

                    // Líneas verticales punteadas, una por banda
                    frecuencias.forEach { f ->
                        val x = fraccionX(f) * ancho
                        drawLine(
                            color = colorGrid,
                            start = Offset(x, margen),
                            end = Offset(x, alto - margen),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    }

                    val xs = frecuencias.map { fraccionX(it) }
                    val puntos = frecuencias.mapIndexed { i, f ->
                        Offset(fraccionX(f) * ancho, yDeGanancia(ganancias[i], alto, margen))
                    }

                    // Curva final = bandas + Graves/Voz/Agudos (limitada a ±12 dB al dibujar)
                    val muestras = 120
                    val curvaFinal = Path()
                    val rellenoFinal = Path()
                    for (i in 0..muestras) {
                        val t = i / muestras.toFloat()
                        val f = FREC_MIN * 10f.pow(3f * t)
                        val g = (gananciaBandas(t, xs, ganancias) +
                                aporteMacros(f, graves, voz, agudos)).coerceIn(-12f, 12f)
                        val x = t * ancho
                        val y = yDeGanancia(g, alto, margen)
                        if (i == 0) {
                            curvaFinal.moveTo(x, y)
                            rellenoFinal.moveTo(0f, alto)
                            rellenoFinal.lineTo(x, y)
                        } else {
                            curvaFinal.lineTo(x, y)
                            rellenoFinal.lineTo(x, y)
                        }
                    }
                    rellenoFinal.lineTo(ancho, alto)
                    rellenoFinal.close()

                    // Línea de bandas (tenue), pasa por los puntos
                    val lineaBandas = Path().apply {
                        moveTo(0f, puntos.first().y)
                        trazarCurva(puntos, ancho)
                    }

                    drawPath(
                        path = rellenoFinal,
                        brush = Brush.verticalGradient(
                            listOf(colorLinea.copy(alpha = 0.30f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = lineaBandas,
                        color = colorLinea.copy(alpha = 0.55f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    drawPath(
                        path = curvaFinal,
                        color = colorLinea,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Puntos arrastrables
                    puntos.forEachIndexed { i, p ->
                        if (i == bandaActiva) {
                            drawCircle(colorLinea.copy(alpha = 0.25f), radius = 22.dp.toPx(), center = p)
                        }
                        drawCircle(colorLinea, radius = 9.dp.toPx(), center = p)
                        drawCircle(colorFondo, radius = 4.dp.toPx(), center = p)
                    }
                }

                // Etiquetas del eje vertical
                Text(
                    "+12",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorTexto,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 4.dp)
                )
                Text(
                    "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorTexto,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(y = (-10).dp)
                        .padding(start = 4.dp)
                )
                Text(
                    "-12",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorTexto,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 4.dp)
                )
            }

            // Frecuencia y valor de cada banda, alineados con su punto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                frecuencias.forEachIndexed { i, f ->
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .offset(x = anchoTotal * fraccionX(f) - 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            textoFrecuencia(f),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorTexto,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            textoDb(ganancias[i]),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorLinea,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}