package ar.jornadas.legacy

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// Plantilla utilizada en la edición de un solo día.
internal data class Turno(val inicio: Int, val actividad: String, val aula: String)

internal fun filasParaImprimir(turnos: List<Turno>): List<String> =
    turnos.sortedBy { it.inicio }.map {
        "%02d:%02d  %s / %s".format(it.inicio / 60, it.inicio % 60, it.actividad, it.aula)
    }

@Preview(showBackground = true)
@Composable
private fun AgendaDeBolsillo() {
    Column {
        filasParaImprimir(listOf(Turno(540, "Apertura", "Hall"),
            Turno(570, "Primeros pasos con Kotlin", "Aula A"))).forEach { Text(it) }
    }
}
