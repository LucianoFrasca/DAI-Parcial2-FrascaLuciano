package ar.jornadas.data

import java.util.concurrent.atomic.AtomicBoolean

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao) {
    val programa = dao.observar()

    /** null = nunca se descargó una edición (pedido 41). */
    val edicion = dao.observarEdicion()

    // Pedido 44: el repositorio vive una vez por proceso (ver JornadasApp), así que este
    // flag también. Un ViewModel nuevo en el mismo proceso no vuelve a disparar la carga.
    private val cargaAutomaticaPendiente = AtomicBoolean(true)

    /** Devuelve true solo la primera vez en el proceso. */
    fun reclamarCargaAutomatica(): Boolean = cargaAutomaticaPendiente.getAndSet(false)

    suspend fun sincronizar() {
        val fichas = api.consultar().map {
            Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
        }
        dao.aplicarEdicion(fichas, System.currentTimeMillis())
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
