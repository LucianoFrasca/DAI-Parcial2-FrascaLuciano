package ar.jornadas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import ar.jornadas.ui.*

class MainActivity : ComponentActivity() {
    private val model: ProgramaModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.hasExtra("servicio_disponible")) {
            // Se aplica sobre los servicios del proceso ANTES de crear el ViewModel,
            // porque su init dispara la carga automática.
            (application as JornadasApp).servicios.mesa.disponible =
                intent.getBooleanExtra("servicio_disponible", true)
            intent.removeExtra("servicio_disponible")
        }
        setContent { Portada(model) }
    }
    // Pedido 44: se quitó recargar() de onStart(): corría en cada rotación y al volver del fondo.
}
