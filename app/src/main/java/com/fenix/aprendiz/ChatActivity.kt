package com.fenix.aprendiz

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.util.Locale

/**
 * Chat en vivo con Fénix Aprendiz, accesible desde el botón flotante Teshuá.
 *
 * FASE D: ahora soporta voz completa dentro del propio chat flotante:
 *  - Botón 🎤 -> pide permiso de micrófono si falta, abre el reconocedor de
 *    voz del sistema, y al terminar envía el texto reconocido directo al
 *    Space (igual que si lo hubieras escrito).
 *  - Cada respuesta de Fénix, si el Space devolvió audio_respuesta, se
 *    reproduce automáticamente con MediaPlayer (misma voz/personalidad que
 *    configuraste en el Space, ej. "Voz de Teshuá").
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var inputMensaje: EditText
    private lateinit var progress: ProgressBar
    private lateinit var panelAjustes: View
    private lateinit var estadoClave: TextView
    private lateinit var btnMic: TextView

    // Historial en el formato que espera Gradio (lista de mensajes {"role","content"})
    private val historialGradio = JSONArray()

    private var mediaPlayer: MediaPlayer? = null

    // --- Permiso de micrófono ---
    private val pedirPermisoMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            lanzarReconocedorVoz()
        } else {
            Toast.makeText(this, "Necesito el micrófono para escucharte.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Reconocedor de voz del sistema ---
    private val reconocedorVoz = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val texto = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!texto.isNullOrBlank()) {
            inputMensaje.setText(texto)
            enviar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        rv = findViewById(R.id.rvChat)
        inputMensaje = findViewById(R.id.inputMensaje)
        progress = findViewById(R.id.progressChat)
        panelAjustes = findViewById(R.id.panelAjustes)
        estadoClave = findViewById(R.id.estadoClave)
        btnMic = findViewById(R.id.btnMic)

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

        btnMic.setOnClickListener { onMicPulsado() }

        actualizarEstadoClave()
    }

    private fun onMicPulsado() {
        val tienePermiso = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) {
            lanzarReconocedorVoz()
        } else {
            pedirPermisoMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun lanzarReconocedorVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "MX"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla con Fénix…")
        }
        try {
            reconocedorVoz.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Este dispositivo no tiene reconocimiento de voz disponible.", Toast.LENGTH_LONG).show()
        }
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
            onResultado = { respuesta, historialActualizado, audioUrl ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    adapter.agregar(Mensaje(respuesta, esUsuario = false))
                    rv.scrollToPosition(adapter.itemCount - 1)
                    sincronizarHistorial(historialActualizado)
                    if (audioUrl != null) reproducirAudio(audioUrl)
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

    private fun reproducirAudio(url: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnCompletionListener { it.release() }
                setOnErrorListener { mp, _, _ -> mp.release(); true }
                prepareAsync()
            }
        } catch (e: Exception) {
            // Si falla la reproducción, el texto ya se mostró igual; no interrumpimos el chat.
        }
    }

    private fun sincronizarHistorial(nuevo: JSONArray) {
        historialGradio.let {
            while (it.length() > 0) it.remove(0)
        }
        for (i in 0 until nuevo.length()) historialGradio.put(nuevo.get(i))
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
