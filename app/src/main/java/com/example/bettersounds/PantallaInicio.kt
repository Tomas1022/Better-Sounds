package com.example.bettersounds

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

class Preset(
    val nombre: String,
    val graves: Float,
    val medios: Float,
    val agudos: Float,
    val bandas: List<Float>? = null // null = al cargarlo no toca los 5 puntos
)

private val presets = listOf(
    Preset("Plano", 0f, 0f, 0f),
    Preset("Graves", 8f, 0f, -2f),
    Preset("Vocal", -2f, 6f, 3f),
    Preset("Gamer", 3f, 2f, 6f)
)

private val frecuenciasAvanzado = listOf(60, 230, 910, 3600, 14000)

// Dibuja una barra de scroll a la derecha, solo si el contenido no cabe
fun Modifier.barraScroll(estado: ScrollState, color: Color): Modifier = drawWithContent {
    drawContent()
    if (estado.maxValue > 0) {
        val altoVisible = size.height
        val altoTotal = altoVisible + estado.maxValue
        val altoBarra = altoVisible * altoVisible / altoTotal
        val posicion = estado.value.toFloat() / estado.maxValue * (altoVisible - altoBarra)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - 8.dp.toPx(), posicion),
            size = Size(4.dp.toPx(), altoBarra),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

@Composable
fun PantallaInicio(modoAvanzado: Boolean) {
    var activo by remember { mutableStateOf(true) }
    var presetActual by remember { mutableStateOf(presets[0]) }

    // Graves / Voz (Medios) / Agudos: modifican la curva final, no los puntos
    var graves by remember { mutableFloatStateOf(0f) }
    var medios by remember { mutableFloatStateOf(0f) }
    var agudos by remember { mutableFloatStateOf(0f) }

    // Las 5 bandas (puntos de la gráfica): solo se mueven a mano
    val bandas = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }

    fun aplicarPreset(p: Preset) {
        presetActual = p
        graves = p.graves
        medios = p.medios
        agudos = p.agudos
        p.bandas?.forEachIndexed { i, v -> bandas[i] = v }
    }

    var perfilPendiente by remember { mutableStateOf<Preset?>(null) }
    val misPerfiles = remember {
        mutableStateListOf(
            Preset("Mis audífonos", 5f, 1f, 4f, listOf(3f, 1f, 0f, 2f, 4f)),
            Preset("Noche", 3f, -2f, -4f, listOf(2f, 0f, -2f, -3f, -4f)),
            Preset("Podcast", -4f, 6f, 2f),
            Preset("Gym", 6f, 0f, 3f),
            Preset("Viaje", 2f, -1f, 1f)
        )
    }

    // Diálogo de "Guardar perfil"
    var mostrarGuardar by remember { mutableStateOf(false) }
    var nombreNuevo by remember { mutableStateOf("") }

    val scrollPantalla = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {

        // Título: barra de borde a borde que cubre también la barra de estado
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Text(
                "Better Sounds",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 18.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (modoAvanzado) Modifier.verticalScroll(scrollPantalla) else Modifier)
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ecualizador activo")
                Switch(checked = activo, onCheckedChange = { activo = it })
            }

            if (modoAvanzado) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ecualizador de 5 bandas",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = {
                                for (i in bandas.indices) bandas[i] = 0f
                                aplicarPreset(presets[0])
                            },
                            enabled = activo
                        ) {
                            Text("Restablecer")
                        }
                    }
                    Text(
                        "Arrastra los puntos para ajustar cada banda. Graves, Voz y Agudos moldean la curva sin mover los puntos.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                GraficaEQ(
                    frecuencias = frecuenciasAvanzado,
                    ganancias = bandas,
                    graves = graves,
                    voz = medios,
                    agudos = agudos,
                    habilitado = activo,
                    onCambio = { indice, valor -> bandas[indice] = valor },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CampoDb("Graves", graves, activo, Modifier.weight(1f)) { graves = it }
                    CampoDb("Voz", medios, activo, Modifier.weight(1f)) { medios = it }
                    CampoDb("Agudos", agudos, activo, Modifier.weight(1f)) { agudos = it }
                }

                PanelPerfiles(
                    perfiles = misPerfiles,
                    onElegir = { perfilPendiente = it },
                    onGuardar = {
                        nombreNuevo = ""
                        mostrarGuardar = true
                    },
                    modifier = Modifier.height(260.dp)
                )
            } else {
                ChipsRecomendados(presetActual) { aplicarPreset(it) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ControlVertical("Graves", graves, activo) { graves = it }
                    ControlVertical("Medios", medios, activo) { medios = it }
                    ControlVertical("Agudos", agudos, activo) { agudos = it }
                }

                PanelPerfiles(
                    perfiles = misPerfiles,
                    onElegir = { perfilPendiente = it },
                    onGuardar = {
                        nombreNuevo = ""
                        mostrarGuardar = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Confirmación al cambiar de perfil
    val pendiente = perfilPendiente
    if (pendiente != null) {
        AlertDialog(
            onDismissRequest = { perfilPendiente = null },
            title = { Text("¿Estás seguro de cambiar de perfil?") },
            text = { Text("Se cargará \"${pendiente.nombre}\" y se reemplazarán los ajustes actuales.") },
            confirmButton = {
                TextButton(onClick = {
                    aplicarPreset(pendiente)
                    perfilPendiente = null
                }) {
                    Text("Cambiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { perfilPendiente = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Guardar los ajustes actuales como perfil nuevo
    if (mostrarGuardar) {
        val nombreLimpio = nombreNuevo.trim()
        val yaExiste = misPerfiles.any { it.nombre.equals(nombreLimpio, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { mostrarGuardar = false },
            title = { Text("Guardar perfil") },
            text = {
                OutlinedTextField(
                    value = nombreNuevo,
                    onValueChange = { if (it.length <= 20) nombreNuevo = it },
                    label = { Text("Nombre del perfil") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    supportingText = {
                        if (yaExiste) Text("Ya existe: se reemplazará")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nombreLimpio.isNotEmpty(),
                    onClick = {
                        // bandas.toList() guarda una copia, no la lista viva
                        val nuevo = Preset(nombreLimpio, graves, medios, agudos, bandas.toList())
                        val posicion = misPerfiles.indexOfFirst {
                            it.nombre.equals(nombreLimpio, ignoreCase = true)
                        }
                        if (posicion >= 0) misPerfiles.removeAt(posicion)
                        misPerfiles.add(0, nuevo)
                        presetActual = nuevo
                        mostrarGuardar = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarGuardar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ChipsRecomendados(presetActual: Preset, onElegir: (Preset) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            "Perfiles Recomendados",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                FilterChip(
                    selected = preset == presetActual,
                    onClick = { onElegir(preset) },
                    label = { Text(preset.nombre) }
                )
            }
        }
    }
}

// Panel de "Mis perfiles": lo usan los dos modos, cambia solo su altura
@Composable
fun PanelPerfiles(
    perfiles: List<Preset>,
    onElegir: (Preset) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorPrimario = MaterialTheme.colorScheme.primary
    val sombra = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val scrollLista = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(colorPrimario.copy(alpha = 0.45f), Color.Transparent)
            )
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mis perfiles (${perfiles.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onGuardar) {
                    Text("+ Guardar")
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .barraScroll(scrollLista, colorPrimario.copy(alpha = 0.7f))
                        .verticalScroll(scrollLista)
                ) {
                    perfiles.forEach { perfil ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onElegir(perfil) }
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Text(perfil.nombre)
                            Text(
                                "Graves ${perfil.graves.toInt()} · Medios ${perfil.medios.toInt()} · Agudos ${perfil.agudos.toInt()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            perfil.bandas?.let { b ->
                                Text(
                                    "Bandas " + b.joinToString(" / ") { formatoDb(it) },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Brush.verticalGradient(listOf(sombra, Color.Transparent)))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, sombra)))
                )
            }
        }
    }
}

private fun formatoDb(v: Float): String {
    val r = (v * 10).roundToInt() / 10f
    return if (r % 1f == 0f) r.toInt().toString() else r.toString()
}

// Campo numérico en dB (-12 a +12). Acepta coma o punto como decimal.
@Composable
fun CampoDb(
    etiqueta: String,
    valor: Float,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    onCambio: (Float) -> Unit
) {
    val foco = LocalFocusManager.current
    var texto by remember(valor) { mutableStateOf(formatoDb(valor)) }

    OutlinedTextField(
        value = texto,
        onValueChange = { nuevo ->
            val limpio = nuevo.replace(',', '.')
            if (limpio.length <= 5 && limpio.all { it.isDigit() || it == '-' || it == '.' }) {
                texto = limpio
                val numero = limpio.toFloatOrNull()
                if (numero != null) {
                    val limitado = numero.coerceIn(-12f, 12f)
                    onCambio(limitado)
                    if (limitado != numero) texto = formatoDb(limitado)
                }
            }
        },
        label = { Text(etiqueta) },
        suffix = { Text("dB") },
        singleLine = true,
        enabled = habilitado,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }),
        modifier = modifier.onFocusChanged { estado ->
            // Al salir del campo, muestra el valor real (por si quedó vacío o a medias)
            if (!estado.isFocused) texto = formatoDb(valor)
        }
    )
}

@Composable
fun ControlVertical(nombre: String, valor: Float, habilitado: Boolean, onCambio: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${valor.toInt()} dB")
        SliderVertical(
            valor = valor,
            habilitado = habilitado,
            onCambio = onCambio,
            modifier = Modifier
                .height(170.dp)
                .width(56.dp)
        )
        Text(nombre, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SliderVertical(
    valor: Float,
    habilitado: Boolean,
    onCambio: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = valor,
        onValueChange = onCambio,
        valueRange = -12f..12f,
        enabled = habilitado,
        modifier = modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
    )
}