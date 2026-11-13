package ar.jornadas

import android.app.Application

/**
 * Dueña de las dependencias de todo el proceso: una base Room, un Retrofit y un
 * servicio de prueba (con su contador) por proceso, compartidos por cualquier ViewModel.
 * Antes, cada ProgramaModel creaba los suyos.
 */
class JornadasApp : Application() {
    val servicios by lazy { Servicios(this) }
}
