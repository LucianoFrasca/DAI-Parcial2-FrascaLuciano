package ar.jornadas.lab

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.*
import java.io.IOException

class ServicioLocal(private val app: Context) : Interceptor {
    private val config = app.getSharedPreferences("mesa", Context.MODE_PRIVATE)
    val llamadas = MutableStateFlow(0)
    var revision: Int
        get() = config.getInt("revision", 1)
        set(value) { config.edit().putInt("revision", value).apply() }
    var disponible: Boolean
        get() = config.getBoolean("disponible", true)
        set(value) { config.edit().putBoolean("disponible", value).apply() }

    var demoraLarga: Boolean
        get() = config.getBoolean("demoraLarga", false)
        set(value) { config.edit().putBoolean("demoraLarga", value).apply() }

    override fun intercept(chain: Interceptor.Chain): Response {
        synchronized(this) { llamadas.value += 1 }
        val online = disponible
        val version = revision
        Thread.sleep(if (demoraLarga) 8000L else 1400L)
        if (!online) throw IOException("Servicio desconectado")
        val json = app.assets.open("programa-$version.json").bufferedReader().use { it.readText() }
        return Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
            .code(200).message("OK")
            // Retrofit 2.11 trae OkHttp 3.14 (API Java): no existen toResponseBody()/toMediaType() de OkHttp 4.
            .body(ResponseBody.create(MediaType.get("application/json"), json)).build()
    }
}
