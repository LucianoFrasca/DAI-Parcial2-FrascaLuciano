package ar.jornadas.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.jornadas.data.Charla

@Composable
fun Catalogo(model: ProgramaModel, abrir: (String) -> Unit) {
    // Pedido 44: se quitó LaunchedEffect(Unit) { model.recargar() }, que volvía a pedir el
    // programa cada vez que el Catalogo entraba en pantalla (al volver de una ficha o de otra pestaña).
    // Pedido 43: día y tema se leen del ViewModel (SavedStateHandle) en vez de un remember local.
    val dia by model.dia.collectAsStateWithLifecycle()
    val tema by model.tema.collectAsStateWithLifecycle()
    val estado by model.uiState.collectAsStateWithLifecycle()
    val cargando by model.descargando.collectAsStateWithLifecycle()
    Selector(listOf("Ambos días", "Día 1", "Día 2"), dia, model::elegirDia)
    val temas = listOf(ProgramaModel.TODOS, "Android", "Datos", "Calidad")
    Selector(temas, temas.indexOf(tema)) { model.elegirTema(temas[it]) }
    TextButton(onClick = model::recargar) { Text("Actualizar") }
    if (cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
    // Pedido 41: la falla ya no reemplaza la lista; cada estado se dibuja por separado.
    when (val e = estado) {
        ProgramaUiState.Cargando -> {}
        is ProgramaUiState.SinDatos -> {
            Text(e.mensaje)
            OutlinedButton(onClick = model::recargar) { Text("Reintentar") }
        }
        is ProgramaUiState.Listo ->
            if (e.edicionVacia) Text("Sin charlas programadas.") else Grilla(e.charlas, abrir)
    }
}

@Composable
private fun Selector(opciones: List<String>, indice: Int, elegir: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opciones.forEachIndexed { i, texto ->
            FilterChip(selected = i == indice, onClick = { elegir(i) }, label = { Text(texto) })
        }
    }
}

@Composable
fun Grilla(fichas: List<Charla>, abrir: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)) {
        items(fichas, key = { it.id }) { ficha ->
            ElevatedCard(onClick = { abrir(ficha.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Día ${ficha.dia} · ${ficha.hora} · ${ficha.sala}", style = MaterialTheme.typography.labelMedium)
                    Text(ficha.titulo, style = MaterialTheme.typography.titleLarge)
                    Text(if (ficha.favorita) "${ficha.tema} · En mi agenda" else ficha.tema)
                }
            }
        }
    }
}
