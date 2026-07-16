package com.fenix.aprendiz

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Primera pantalla del Jardín (aparte del chat de Fénix): identidad ligera
 * con nombre + PIN de 4 dígitos. No reemplaza nada de ChatActivity/MainActivity;
 * es la puerta de entrada para lo que viene después (Templo de Gratitud,
 * Círculo de Historias, Río de Conversación).
 *
 * Si el dispositivo ya recuerda un nombre guardado (Prefs.leerNombreJardin),
 * se entra directo sin pedir nada — el PIN solo hace falta la primera vez o
 * si la persona cambia de celular y quiere recuperar su lugar en el Jardín.
 */
class IdentidadActivity : AppCompatActivity() {

    private lateinit var inputNombre: EditText
    private lateinit var inputPin: EditText
    private lateinit var progress: View
    private lateinit var estado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identidad)

        inputNombre = findViewById(R.id.inputNombreJardin)
        inputPin = findViewById(R.id.inputPinJardin)
        progress = findViewById(R.id.progressJardin)
        estado = findViewById(R.id.estadoJardin)

        // Si el dispositivo ya tiene una identidad guardada, la mostramos
        // precargada (la persona puede seguir de largo con el mismo PIN,
        // o cambiar de nombre si quiere entrar como alguien más).
        Prefs.leerNombreJardin(this)?.let { inputNombre.setText(it) }

        findViewById<Button>(R.id.btnEntrarJardin).setOnClickListener { intentarEntrar() }
    }

    private fun intentarEntrar() {
        val nombre = inputNombre.text.toString().trim()
        val pin = inputPin.text.toString().trim()

        if (nombre.isEmpty()) {
            mostrarEstado(getString(R.string.jardin_error_nombre_vacio))
            return
        }
        if (!Regex("^\\d{4}$").matches(pin)) {
            mostrarEstado(getString(R.string.jardin_error_pin))
            return
        }

        progress.visibility = View.VISIBLE
        estado.visibility = View.GONE

        IdentidadApiClient.entrarORegistrar(
            nombre = nombre,
            pin = pin,
            onResultado = { resultado ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    mostrarEstado(resultado.mensaje)
                    if (resultado.ok) {
                        Prefs.guardarNombreJardin(this, resultado.nombreVisible)
                        Toast.makeText(this, resultado.mensaje, Toast.LENGTH_LONG).show()
                        // Aquí, cuando exista, se abrirá la siguiente pantalla
                        // del Jardín (por ahora esta es la única pieza construida).
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    mostrarEstado(getString(R.string.jardin_error_conexion, error))
                }
            }
        )
    }

    private fun mostrarEstado(texto: String) {
        estado.text = texto
        estado.visibility = View.VISIBLE
    }
}
