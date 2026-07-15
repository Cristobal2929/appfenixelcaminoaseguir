package com.fenix.aprendiz

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

/**
 * Chat en vivo con Fénix Aprendiz, accesible desde el botón flotante Teshuá.
 * Solo texto en esta versión (la voz sigue disponible completa en el WebView
 * principal). Incluye la pestaña de Ajustes para la clave personal de
 * Cerebras.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var inputMensaje: EditText
    private lateinit var progress: ProgressBar
    private lateinit var panelAjustes: View
    private lateinit var estadoClave: TextView

    // Historial en el formato que espera Gradio: lista de [usuario, fenix]
    private val historialGradio = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        rv = findViewById(R.id.rvChat)
        inputMensaje = findViewById(R.id.inputMensaje)
        progress = findViewById(R.id.progressChat)
        panelAjustes = findViewById(R.id.panelAjustes)
        estadoClave = findViewById(R.id.estadoClave)

        adapter = ChatAdapter(mutableListOf())
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<TextView>(R.id.btnCerrarChat).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnAjustes).setOnClickListener {
            panelAjustes.visibility = if (panelAjustes.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        findViewById<TextView>(R.id.linkCerebras).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cloud.cerebras.ai")))
        }

        findViewById<Button>(R.id.btnGuardarClave).setOnClickListener {
            val clave = findViewById<EditText>(R.id.inputClaveCerebras).text.toString().trim()
            if (clave.isNotEmpty()) {
                Prefs.guardarClaveCerebras(this, clave)
                actualizarEstadoClave()
                Toast.makeText(this, "Clave guardada en este dispositivo", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnOlvidarClave).setOnClickListener {
            Prefs.olvidarClaveCerebras(this)
            findViewById<EditText>(R.id.inputClaveCerebras).setText("")
            actualizarEstadoClave()
        }

        findViewById<Button>(R.id.btnEnviar).setOnClickListener { enviar() }

        actualizarEstadoClave()
    }

    private fun actualizarEstadoClave() {
        estadoClave.text = if (Prefs.leerClaveCerebras(this) != null)
            getString(R.string.ajustes_estado_propia) else getString(R.string.ajustes_estado_pool)
    }

    private fun enviar() {
        val texto = inputMensaje.text.toString().trim()
        if (texto.isEmpty()) return
        inputMensaje.setText("")
        adapter.agregar(Mensaje(texto, esUsuario = true))
        rv.scrollToPosition(adapter.itemCount - 1)
        progress.visibility = View.VISIBLE

        val clave = Prefs.leerClaveCerebras(this)
        FenixApiClient.enviarMensaje(
            mensaje = texto,
            historialGradio = historialGradio,
            claveUsuario = clave,
            onResultado = { respuesta, historialActualizado ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    adapter.agregar(Mensaje(respuesta, esUsuario = false))
                    rv.scrollToPosition(adapter.itemCount - 1)
                    sincronizarHistorial(historialActualizado)
                }
            },
            onError = { error ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    Toast.makeText(this, "Fénix no responde: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun sincronizarHistorial(nuevo: JSONArray) {
        historialGradio.let {
            while (it.length() > 0) it.remove(0)
        }
        for (i in 0 until nuevo.length()) historialGradio.put(nuevo.get(i))
    }
}
