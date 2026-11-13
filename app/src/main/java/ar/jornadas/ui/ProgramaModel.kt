package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.JornadasApp
import ar.jornadas.data.Charla
import ar.jornadas.data.Edicion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

/** Estados excluyentes de la pantalla Programa (pedido 41). */
sealed interface ProgramaUiState {
    /** Nunca se descargó una edición y hay una descarga en curso. */
    data object Cargando : ProgramaUiState
    /** Nunca se descargó una edición y no hay descarga en curso: se ofrece reintentar. */
    data class SinDatos(val mensaje: String) : ProgramaUiState
    /** Hay una edición guardada (puede tener cero charlas). [charlas] ya viene filtrada. */
    data class Listo(val charlas: List<Charla>, val edicionVacia: Boolean) : ProgramaUiState
}

/** Estado de la última descarga; coexiste con los datos guardados, por eso no es parte del UiState. */
private data class Descarga(val enCurso: Boolean = false, val fallo: Boolean = false)

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {
    // Pedido 44: Room, Retrofit y el servicio de prueba son únicos por proceso.
    private val servicios = (app as JornadasApp).servicios
    val mesa = servicios.mesa
    private val repo = servicios.agenda

    private val descarga = MutableStateFlow(Descarga())
    private var trabajo: Job? = null

    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val edicion: SharedFlow<Edicion?> =
        repo.edicion.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    // Pedido 43: los filtros viven en el SavedStateHandle → sobreviven a la rotación, a que el
    // Catalogo salga de pantalla al abrir una ficha y a la muerte del proceso. Una tarea nueva
    // trae un SavedStateHandle vacío, así que arranca sin filtros.
    val dia: StateFlow<Int> = estado.getStateFlow(CLAVE_DIA, 0)
    val tema: StateFlow<String> = estado.getStateFlow(CLAVE_TEMA, TODOS)
    fun elegirDia(valor: Int) { estado[CLAVE_DIA] = valor }
    fun elegirTema(valor: String) { estado[CLAVE_TEMA] = valor }

    val descargando: StateFlow<Boolean> = descarga.map { it.enCurso }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Pedido 41: hay una edición guardada pero la última descarga falló. */
    val usandoGuardados: StateFlow<Boolean> = combine(edicion, descarga) { e, d -> e != null && d.fallo }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val uiState: StateFlow<ProgramaUiState> =
        // Se combina el Flow de Room (no `charlas`) para no confundir su lista inicial vacía
        // con una edición vacía.
        combine(edicion, repo.programa, dia, tema, descarga, ::armar)
            .stateIn(viewModelScope, SharingStarted.Eagerly, ProgramaUiState.Cargando)

    private fun armar(edicion: Edicion?, todas: List<Charla>, dia: Int, tema: String,
                      descarga: Descarga): ProgramaUiState = when {
        edicion == null && descarga.enCurso -> ProgramaUiState.Cargando
        edicion == null && descarga.fallo ->
            ProgramaUiState.SinDatos("No se pudo descargar el programa. Revisá la conexión y reintentá.")
        edicion == null -> ProgramaUiState.SinDatos("Todavía no se descargó el programa.")
        else -> ProgramaUiState.Listo(
            charlas = todas.filter { (dia == 0 || it.dia == dia) && (tema == TODOS || it.tema == tema) },
            edicionVacia = todas.isEmpty(),
        )
    }

    /** Pedido 44: si ya hay una petición en curso, un segundo toque no lanza otra. */
    fun recargar() {
        if (trabajo?.isActive == true) return
        trabajo = viewModelScope.launch {
            descarga.update { it.copy(enCurso = true) }
            // Solo se captura IOException (como el original): una CancellationException no se
            // atrapa, así que se propaga y la corrutina se cancela bien.
            descarga.value = try {
                repo.sincronizar()
                Descarga()
            } catch (e: IOException) {
                Descarga(fallo = true)
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }

    // Va al final: tiene que ejecutarse después de inicializar `descarga` y `trabajo`.
    init {
        // Pedido 44: una sola carga automática por proceso (el flag vive en el repositorio).
        if (repo.reclamarCargaAutomatica()) recargar()
    }

    companion object {
        const val TODOS = "Todos"
        private const val CLAVE_DIA = "filtro_dia"
        private const val CLAVE_TEMA = "filtro_tema"
    }
}
