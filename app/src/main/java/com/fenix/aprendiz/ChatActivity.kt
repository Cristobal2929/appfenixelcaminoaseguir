package com.fenix.aprendiz

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.util.Locale

/**
 * Chat en vivo con Fénix Aprendiz — la única pantalla de conversación.
 * Nunca muestra el Space: todo pasa por FenixApiClient (API de Gradio).
 *
 * Voz completa: 🎤 pide permiso, abre el reconocedor del sistema y envía
 * el texto reconocido; cada respuesta se reproduce con el audio que genera
 * el Space (misma voz/personalidad configurada allá).
 *
 * Persistencia: el historial (tanto el formato Gradio que se manda al
 * Space como las burbujas ya pintadas) se guarda en este dispositivo
 * (Prefs) tras cada mensaje, así que cerrar y reabrir la app no pierde la
 * conversación. El botón "Reiniciar conversación" borra ese historial y
 * empieza de cero. El subtítulo muestra la "lección"/tema que el maestro
 * interno detectó en el último turno, cuando el Space la incluye.
 */
class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ABRIR_AJUSTES = "abrir_ajustes"
    }

    private lateinit var rv: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var inputMensaje: EditText
    private lateinit var progress: View
    private lateinit var panelAjustes: View
    private lateinit var estadoClave: TextView
    private lateinit var btnMic: ImageButton
    private lateinit var tvSubtitulo: TextView
    private lateinit var drawer: DrawerLayout
    private lateinit var rvHistorial: RecyclerView
    private lateinit var tvHistorialVacio: TextView
    private lateinit var convAdapter: ConversacionesAdapter

    private val historialGradio = JSONArray()
    private var mediaPlayer: MediaPlayer? = null

    private val pedirPermisoMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) lanzarReconocedorVoz()
        else Toast.makeText(this, "Necesito el micrófono para escucharte.", Toast.LENGTH_SHORT).show()
    }

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
        tvSubtitulo = findViewById(R.id.tvSubtitulo)

        // Carga lo que ya estaba guardado en este dispositivo (si lo hay).
        adapter = ChatAdapter(Prefs.leerMensajesUI(this))
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val historialGuardado = Prefs.leerHistorialGradio(this)
        for (i in 0 until historialGuardado.length()) historialGradio.put(historialGuardado.get(i))

        actualizarSubtitulo(Prefs.leerUltimaLeccion(this))

        if (adapter.itemCount > 0) rv.scrollToPosition(adapter.itemCount - 1)

        findViewById<ImageButton>(R.id.btnCerrarChat).setOnClickListener { cerrar() }

        findViewById<ImageButton>(R.id.btnAjustes).setOnClickListener {
            alternarPanelAjustes()
        }

        findViewById<ImageButton>(R.id.btnReiniciar).setOnClickListener {
            confirmarReinicioConversacion()
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

        findViewById<ImageButton>(R.id.btnEnviar).setOnClickListener { enviar() }

        btnMic.setOnClickListener { onMicPulsado() }

        // ── Historial lateral (cajón izquierdo) ──
        drawer = findViewById(R.id.drawerChat)
        rvHistorial = findViewById(R.id.rvHistorial)
        tvHistorialVacio = findViewById(R.id.tvHistorialVacio)
        convAdapter = ConversacionesAdapter(
            mutableListOf(),
            alAbrir = { abrirDelHistorial(it) },
            alBorrar = { confirmarBorrado(it) }
        )
        rvHistorial.layoutManager = LinearLayoutManager(this)
        rvHistorial.adapter = convAdapter

        findViewById<ImageButton>(R.id.btnHistorial).setOnClickListener {
            refrescarHistorial()
            drawer.openDrawer(GravityCompat.START)
        }
        findViewById<Button>(R.id.btnNuevaConversacion).setOnClickListener {
            nuevaConversacionUI()
        }

        actualizarEstadoClave()

        if (intent.getBooleanExtra(EXTRA_ABRIR_AJUSTES, false)) {
            panelAjustes.visibility = View.VISIBLE
        }
    }

    private fun alternarPanelAjustes() {
        panelAjustes.visibility = if (panelAjustes.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun cerrar() {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_down_out)
    }

    override fun onBackPressed() {
        if (::drawer.isInitialized && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            cerrar()
        }
    }

    private fun confirmarReinicioConversacion() {
        AlertDialog.Builder(this)
            .setTitle(R.string.chat_reiniciar_confirmar_titulo)
            .setMessage(R.string.chat_reiniciar_confirmar_mensaje)
            .setPositiveButton(R.string.chat_reiniciar_confirmar_boton) { dialog, _ ->
                reiniciarConversacion()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.chat_reiniciar_cancelar_boton) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun reiniciarConversacion() {
        // "Reiniciar" descarta del todo la conversación actual (también del
        // historial) y arranca una nueva y vacía. Para conservar la actual y
        // empezar otra, se usa "Nueva conversación" en el cajón lateral.
        Prefs.borrarConversacion(this, Prefs.idConversacionActual(this))
        Prefs.nuevaConversacion(this)
        adapter.limpiar()
        while (historialGradio.length() > 0) historialGradio.remove(0)
        actualizarSubtitulo(null)
        Toast.makeText(this, R.string.chat_reiniciar_hecho, Toast.LENGTH_SHORT).show()
    }

    /** Empieza una conversación nueva CONSERVANDO la actual en el historial. */
    private fun nuevaConversacionUI() {
        Prefs.archivarConversacionActual(
            this, adapter.obtenerTodos(), historialGradio, Prefs.leerUltimaLeccion(this)
        )
        Prefs.nuevaConversacion(this)
        adapter.limpiar()
        while (historialGradio.length() > 0) historialGradio.remove(0)
        actualizarSubtitulo(null)
        cerrarDrawerSiAbierto()
        Toast.makeText(this, "Nueva conversación", Toast.LENGTH_SHORT).show()
    }

    /** Abre una conversación guardada desde el historial (guardando antes la actual). */
    private fun abrirDelHistorial(resumen: Prefs.ResumenConversacion) {
        Prefs.archivarConversacionActual(
            this, adapter.obtenerTodos(), historialGradio, Prefs.leerUltimaLeccion(this)
        )
        val conv = Prefs.abrirConversacion(this, resumen.id) ?: return
        adapter.cargarGuardados(conv.mensajes)
        while (historialGradio.length() > 0) historialGradio.remove(0)
        for (i in 0 until conv.historial.length()) historialGradio.put(conv.historial.get(i))
        actualizarSubtitulo(conv.leccion)
        if (adapter.itemCount > 0) rv.scrollToPosition(adapter.itemCount - 1)
        cerrarDrawerSiAbierto()
    }

    private fun confirmarBorrado(resumen: Prefs.ResumenConversacion) {
        AlertDialog.Builder(this)
            .setTitle(R.string.chat_conv_borrar_titulo)
            .setMessage(R.string.chat_conv_borrar_mensaje)
            .setPositiveButton(R.string.chat_conv_borrar_boton) { d, _ ->
                Prefs.borrarConversacion(this, resumen.id)
                // Si borré la que estaba abierta, limpio la pantalla y arranco una nueva.
                if (resumen.id == Prefs.idConversacionActual(this)) {
                    Prefs.nuevaConversacion(this)
                    adapter.limpiar()
                    while (historialGradio.length() > 0) historialGradio.remove(0)
                    actualizarSubtitulo(null)
                }
                refrescarHistorial()
                d.dismiss()
            }
            .setNegativeButton(R.string.chat_conv_cancelar) { d, _ -> d.dismiss() }
            .show()
    }

    private fun refrescarHistorial() {
        val lista = Prefs.listarConversaciones(this)
        convAdapter.refrescar(lista)
        tvHistorialVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        rvHistorial.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun cerrarDrawerSiAbierto() {
        if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawer(GravityCompat.START)
    }

    private fun onMicPulsado() {
        val tienePermiso = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) lanzarReconocedorVoz()
        else pedirPermisoMic.launch(Manifest.permission.RECORD_AUDIO)
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

    /** Muestra la última lección/tema detectado en el subtítulo, o el texto por defecto si no hay ninguna. */
    private fun actualizarSubtitulo(leccion: String?) {
        tvSubtitulo.text = if (leccion.isNullOrBlank())
            getString(R.string.chat_subtitulo)
        else
            getString(R.string.chat_subtitulo_leccion, leccion)
    }

    private fun enviar() {
        val texto = inputMensaje.text.toString().trim()
        if (texto.isEmpty()) return
        inputMensaje.setText("")
        adapter.agregar(Mensaje(texto, esUsuario = true))
        rv.scrollToPosition(adapter.itemCount - 1)
        progress.visibility = View.VISIBLE

        // Se guarda ya el mensaje del usuario, por si la app se cierra antes de recibir respuesta.
        Prefs.guardarMensajesUI(this, adapter.obtenerTodos())

        val clave = Prefs.leerClaveCerebras(this)
        val nombreJardin = Prefs.leerNombreJardin(this)
        FenixApiClient.enviarMensaje(
            mensaje = texto,
            historialGradio = historialGradio,
            claveUsuario = clave,
            nombreJardin = nombreJardin,
            onResultado = { respuesta, historialActualizado, audioUrl, categoria ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    adapter.agregar(Mensaje(respuesta, esUsuario = false))
                    rv.scrollToPosition(adapter.itemCount - 1)
                    sincronizarHistorial(historialActualizado)
                    actualizarSubtitulo(categoria)
                    vibrarRespuesta()

                    // Guarda tras cada mensaje: historial Gradio, burbujas UI y lección detectada.
                    Prefs.guardarHistorialGradio(this, historialGradio)
                    Prefs.guardarMensajesUI(this, adapter.obtenerTodos())
                    Prefs.guardarUltimaLeccion(this, categoria)
                    // Refleja también la conversación activa en el historial lateral.
                    Prefs.archivarConversacionActual(this, adapter.obtenerTodos(), historialGradio, categoria)

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

    /** Vibración corta al llegar la respuesta de Fénix (avisa aunque no se esté mirando la pantalla). */
    private fun vibrarRespuesta() {
        try {
            val vibrador = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrador.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(60)
            }
        } catch (e: Exception) {
            // Si el dispositivo no soporta vibración, no interrumpimos el chat.
        }
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
            // El texto ya se mostró; si falla el audio, no interrumpimos el chat.
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
