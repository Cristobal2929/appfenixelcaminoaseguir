package com.fenix.aprendiz

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Altar de la Luz — tercera pieza del Jardín.
 *
 * Capa visual: cada intención encendida se muestra como una vela propia
 * (llama con parpadeo suave + intención + fecha) en vez de una lista de
 * texto plano. La lógica de red y el flujo de encendido no cambian.
 */
class AltarActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOMBRE = "extra_nombre_jardin"
    }

    private lateinit var nombre: String
    private lateinit var inputIntencion: EditText
    private lateinit var progress: View
    private lateinit var estado: TextView
    private lateinit var listaLuces: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_altar)

        nombre = intent.getStringExtra(EXTRA_NOMBRE)
            ?: Prefs.leerNombreJardin(this)
            ?: run {
                Toast.makeText(this, "Entra primero al Jardín.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

        inputIntencion = findViewById(R.id.inputIntencion)
        progress = findViewById(R.id.progressAltar)
        estado = findViewById(R.id.estadoAltar)
        listaLuces = findViewById(R.id.listaLuces)

        findViewById<Button>(R.id.btnEncenderLuz).setOnClickListener { encender() }

        findViewById<TextView>(R.id.btnVolverJardinAltar).setOnClickListener { finish() }

        cargarHistorial()
    }

    private fun encender() {
        val texto = inputIntencion.text.toString().trim()
        if (texto.isEmpty()) {
            mostrarEstado(getString(R.string.altar_error_vacio))
            return
        }
        progress.visibility = View.VISIBLE
        estado.visibility = View.GONE

        AltarApiClient.encender(
            nombre = nombre,
            intencion = texto,
            onResultado = { ok, mensaje ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    mostrarEstado(mensaje)
                    if (ok) {
                        inputIntencion.setText("")
                        cargarHistorial()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    mostrarEstado(getString(R.string.altar_error_conexion, error))
                }
            }
        )
    }

    private fun cargarHistorial() {
        AltarApiClient.listar(
            nombre = nombre,
            onResultado = { luces ->
                runOnUiThread {
                    listaLuces.removeAllViews()
                    if (luces.isEmpty()) {
                        listaLuces.addView(crearItemTexto(getString(R.string.altar_sin_luces)))
                        return@runOnUiThread
                    }
                    for (l in luces) {
                        listaLuces.addView(crearItemVela(l.intencion, l.creadaEn))
                    }
                }
            },
            onError = {
                // Si falla el refresco del historial no interrumpimos el encendido;
                // la persona ya vio si su luz se guardó o no.
            }
        )
    }

    /** Crea una vela: llama con parpadeo suave + intención + fecha formateada. */
    private fun crearItemVela(intencion: String, creadaEnIso: String): View {
        val item = LayoutInflater.from(this)
            .inflate(R.layout.item_luz_vela, listaLuces, false)

        item.findViewById<TextView>(R.id.tvIntencionVela).text = intencion
        item.findViewById<TextView>(R.id.tvFechaVela).text = formatearFecha(creadaEnIso)

        val flama = item.findViewById<ImageView>(R.id.ivFlama)
        iniciarParpadeo(flama)

        return item
    }

    /** Animación sutil de parpadeo: la llama varía su opacidad y escala en bucle infinito. */
    private fun iniciarParpadeo(flama: ImageView) {
        flama.pivotY = flama.height.toFloat()

        val alpha = ObjectAnimator.ofFloat(flama, View.ALPHA, 1f, 0.65f, 1f).apply {
            duration = 900 + (0..400).random().toLong()
            repeatCount = ValueAnimator.INFINITE
        }
        val escalaX = ObjectAnimator.ofFloat(flama, View.SCALE_X, 1f, 0.92f, 1.05f, 1f).apply {
            duration = 900 + (0..400).random().toLong()
            repeatCount = ValueAnimator.INFINITE
        }
        val escalaY = ObjectAnimator.ofFloat(flama, View.SCALE_Y, 1f, 0.95f, 1.03f, 1f).apply {
            duration = 900 + (0..400).random().toLong()
            repeatCount = ValueAnimator.INFINITE
        }
        alpha.start()
        escalaX.start()
        escalaY.start()
    }

    /** Convierte el ISO timestamp de Supabase (UTC) a algo legible en español, hora local. */
    private fun formatearFecha(iso: String): String {
        return try {
            val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale("es", "MX")).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val fecha = entrada.parse(iso.take(19)) ?: return iso
            val salida = SimpleDateFormat("d 'de' MMMM, HH:mm", Locale("es", "MX"))
            salida.format(fecha)
        } catch (e: Exception) {
            iso
        }
    }

    private fun crearItemTexto(texto: String): TextView {
        return TextView(this).apply {
            text = texto
            setTextColor(getColor(R.color.teshua_texto))
            textSize = 14f
            setPadding(0, 12, 0, 12)
        }
    }

    private fun mostrarEstado(texto: String) {
        estado.text = texto
        estado.visibility = View.VISIBLE
    }
}
