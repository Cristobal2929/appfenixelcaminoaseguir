package com.fenix.aprendiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Río de Conversación — cuarta pieza del Jardín. Chat con Fénix (el mismo
 * ciclo que la web del Space, vía el endpoint explícito "fenix_chat_movil"),
 * mostrado como burbujas de usuario/Fénix con el mismo lenguaje visual del
 * resto del Jardín (Templo, Altar).
 *
 * El historial que se muestra es el "río" completo guardado en Supabase
 * (fenix_conversaciones) — compartido, no filtrado por persona — tal como
 * lo expone cargar_conversaciones() del lado del Space.
 */
class RioConversacionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOMBRE = "extra_nombre_jardin"
    }

    private lateinit var nombre: String
    private lateinit var scroll: ScrollView
    private lateinit var contenedorChat: LinearLayout
    private lateinit var inputMensaje: EditText
    private lateinit var btnEnviar: Button
    private lateinit var progress: View
    private lateinit var estado: TextView

    private var historialParaEnviar: List<RioApiClient.Mensaje> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rio)

        nombre = intent.getStringExtra(EXTRA_NOMBRE)
            ?: Prefs.leerNombreJardin(this)
            ?: run {
                Toast.makeText(this, "Entra primero al Jardín.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

        scroll = findViewById(R.id.scrollRio)
        contenedorChat = findViewById(R.id.contenedorChat)
        inputMensaje = findViewById(R.id.inputMensajeRio)
        btnEnviar = findViewById(R.id.btnEnviarRio)
        progress = findViewById(R.id.progressRio)
        estado = findViewById(R.id.estadoRio)

        btnEnviar.setOnClickListener { enviar() }
        findViewById<TextView>(R.id.btnVolverJardinRio).setOnClickListener { finish() }

        cargarHistorialInicial()
    }

    /** Al entrar, pide el río actual sin enviar mensaje (mensaje=""). */
    private fun cargarHistorialInicial() {
        mostrarCargando(true)
        RioApiClient.enviar(
            mensaje = "",
            historialActual = emptyList(),
            nombreJardin = nombre,
            onResultado = { historialVista, historialSiguiente ->
                runOnUiThread {
                    mostrarCargando(false)
                    historialParaEnviar = historialSiguiente
                    pintarHistorial(historialVista)
                }
            },
            onError = {
                runOnUiThread {
                    mostrarCargando(false)
                    mostrarEstado("No se pudo cargar el río todavía. Escribe igual, se reintentará al enviar.")
                }
            }
        )
    }

    private fun enviar() {
        val texto = inputMensaje.text.toString().trim()
        if (texto.isEmpty()) {
            mostrarEstado(getString(R.string.rio_error_vacio))
            return
        }

        // Burbuja del usuario aparece de inmediato, sin esperar al Space.
        agregarBurbujaUsuario(texto)
        inputMensaje.setText("")
        mostrarCargando(true)
        btnEnviar.isEnabled = false

        RioApiClient.enviar(
            mensaje = texto,
            historialActual = historialParaEnviar,
            nombreJardin = nombre,
            onResultado = { historialVista, historialSiguiente ->
                runOnUiThread {
                    mostrarCargando(false)
                    btnEnviar.isEnabled = true
                    historialParaEnviar = historialSiguiente
                    pintarHistorial(historialVista)
                }
            },
            onError = { error ->
                runOnUiThread {
                    mostrarCargando(false)
                    btnEnviar.isEnabled = true
                    mostrarEstado(getString(R.string.rio_error_conexion, error))
                }
            }
        )
    }

    /** Repinta todo el chat desde el historial_view_out más reciente del Space. */
    private fun pintarHistorial(mensajes: List<RioApiClient.Mensaje>) {
        contenedorChat.removeAllViews()
        if (mensajes.isEmpty()) {
            contenedorChat.addView(crearBurbujaFenixView(getString(R.string.rio_sin_mensajes)))
        } else {
            for (m in mensajes) {
                if (m.role == "user") {
                    contenedorChat.addView(crearBurbujaUsuarioView(m.content))
                } else {
                    contenedorChat.addView(crearBurbujaFenixView(m.content))
                }
            }
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun agregarBurbujaUsuario(texto: String) {
        contenedorChat.addView(crearBurbujaUsuarioView(texto))
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun crearBurbujaUsuarioView(texto: String): View {
        val item = LayoutInflater.from(this)
            .inflate(R.layout.item_burbuja_usuario, contenedorChat, false)
        item.findViewById<TextView>(R.id.tvBurbujaUsuario).text = texto
        return item
    }

    private fun crearBurbujaFenixView(texto: String): View {
        val item = LayoutInflater.from(this)
            .inflate(R.layout.item_burbuja_fenix, contenedorChat, false)
        item.findViewById<TextView>(R.id.tvBurbujaFenix).text = texto
        return item
    }

    private fun mostrarCargando(cargando: Boolean) {
        progress.visibility = if (cargando) View.VISIBLE else View.GONE
        if (cargando) estado.visibility = View.GONE
    }

    private fun mostrarEstado(texto: String) {
        estado.text = texto
        estado.visibility = View.VISIBLE
    }
}
