package com.fenix.aprendiz

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Altar de la Luz — tercera pieza del Jardín. Mismo patrón exacto que
 * GratitudActivity: por ahora solo la parte funcional, escribir y guardar
 * una intención y ver el historial como texto simple. La vista tipo
 * "altar con velas encendidas" se construye después, en la capa visual,
 * una vez que todas las piezas del Jardín funcionen bien.
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
                        listaLuces.addView(crearItemTexto("🕯️ ${l.intencion}"))
                    }
                }
            },
            onError = {
                // Si falla el refresco del historial no interrumpimos el encendido;
                // la persona ya vio si su luz se guardó o no.
            }
        )
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
