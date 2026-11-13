package ar.jornadas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.jornadas.lab.ServicioLocal

@Composable
fun Mesa(servicio: ServicioLocal) {
    val contador by servicio.llamadas.collectAsStateWithLifecycle()
    var online by remember { mutableStateOf(servicio.disponible) }
    var revision by remember { mutableIntStateOf(servicio.revision) }
    var lenta by remember { mutableStateOf(servicio.demoraLarga) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Control del servicio de prueba", style = MaterialTheme.typography.titleMedium)
        Text("Las opciones se aplican a la próxima petición.")
        Text("Servicio disponible")
        Switch(checked = online, onCheckedChange = { online = it; servicio.disponible = it })
        Text("Edición del programa")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3, 4).forEach {
                FilterChip(revision == it, { revision = it; servicio.revision = it }, { Text("v$it") })
            }
        }
        Text("Peticiones en esta sesión: $contador")
        Text("Demora larga (8 segundos)")
        Switch(checked = lenta, onCheckedChange = { lenta = it; servicio.demoraLarga = it })
    }
}
