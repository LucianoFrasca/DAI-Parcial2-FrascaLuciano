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
    LaunchedEffect(Unit) { model.recargar() }
    val dia by model.dia.collectAsStateWithLifecycle()
    var tema by remember { mutableStateOf("Todos") }
    val fichas by model.charlas.collectAsStateWithLifecycle()
    val aviso by model.aviso.collectAsStateWithLifecycle()
    val cargando by model.cargando.collectAsStateWithLifecycle()
    Selector(listOf("Ambos días", "Día 1", "Día 2"), dia) { model.dia.value = it }
    val temas = listOf("Todos", "Android", "Datos", "Calidad")
    Selector(temas, temas.indexOf(tema)) { tema = temas[it] }
    TextButton(onClick = model::recargar) { Text("Actualizar") }
    if (cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (aviso.isNotEmpty()) {
        Text(aviso)
        OutlinedButton(onClick = model::recargar) { Text("Reintentar") }
    } else {
        Grilla(fichas.filter { (dia == 0 || it.dia == dia) && (tema == "Todos" || it.tema == tema) }, abrir)
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
