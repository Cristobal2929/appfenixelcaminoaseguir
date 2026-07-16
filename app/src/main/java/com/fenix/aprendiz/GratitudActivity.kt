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
 * Templo de Gratitud — segunda pieza del Jardín (aparte del chat de
 * Fénix). Por ahora solo la parte funcional: escribir y guardar una
 * gratitud, y ver la lista de las anteriores como texto simple. La vista
 * tipo "constelación de luces" se construye después, en la capa visual,
 * una vez que todas las piezas del Jardín funcionen bien.
 */
class GratitudActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOMBRE = "extra_nombre_jardin"
    }

    private lateinit var nombre: String
    private lateinit var inputGratitud: EditText
    private lateinit var progress: View
    private lateinit var estado: TextView
    private lateinit var listaGratitudes: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gratitud)

        nombre = intent.getStringExtra(EXTRA_NOMBRE)
            ?: Prefs.leerNombreJardin(this)
            ?: run {
                Toast.makeText(this, "Entra primero al Jardín.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

        inputGratitud = findViewById(R.id.inputGratitud)
        progress = findViewById(R.id.progressGratitud)
        estado = findViewById(R.id.estadoGratitud)
        listaGratitudes = findViewById(R.id.listaGratitudes)

        findViewById<Button>(R.id.btnGuardarGratitud).setOnClickListener { guardar() }

        cargarHistorial()
    }

    private fun guardar() {
        val texto = inputGratitud.text.toString().trim()
        if (texto.isEmpty()) {
            mostrarEstado(getString(R.string.templo_error_vacio))
            return
        }
        progress.visibility = View.VISIBLE
        estado.visibility = View.GONE

        GratitudApiClient.guardar(
            nombre = nombre,
            texto = texto,
            onResultado = { ok, mensaje ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    mostrarEstado(mensaje)
                    if (ok) {
                        inputGratitud.setText("")
                        cargarHistorial()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    mostrarEstado(getString(R.string.templo_error_conexion, error))
                }
            }
        )
    }

    private fun cargarHistorial() {
        GratitudApiClient.listar(
            nombre = nombre,
            onResultado = { gratitudes ->
                runOnUiThread {
                    listaGratitudes.removeAllViews()
                    if (gratitudes.isEmpty()) {
                        listaGratitudes.addView(crearItemTexto(getString(R.string.templo_sin_gratitudes)))
                        return@runOnUiThread
                    }
                    for (g in gratitudes) {
                        listaGratitudes.addView(crearItemTexto("• ${g.texto}"))
                    }
                }
            },
            onError = {
                // Si falla el refresco del historial no interrumpimos el guardado;
                // la persona ya vio si su gratitud se guardó o no.
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
