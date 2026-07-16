package com.fenix.aprendiz

import android.content.Intent
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
    private lateinit var btnIrTemplo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identidad)

        inputNombre = findViewById(R.id.inputNombreJardin)
        inputPin = findViewById(R.id.inputPinJardin)
        progress = findViewById(R.id.progressJardin)
        estado = findViewById(R.id.estadoJardin)
        btnIrTemplo = findViewById(R.id.btnIrTemplo)

        // Si el dispositivo ya tiene una identidad guardada, la mostramos
        // precargada (la persona puede seguir de largo con el mismo PIN,
        // o cambiar de nombre si quiere entrar como alguien más), y ya le
        // damos acceso directo al Templo sin pedirle el PIN de nuevo.
        Prefs.leerNombreJardin(this)?.let {
            inputNombre.setText(it)
            btnIrTemplo.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btnEntrarJardin).setOnClickListener { intentarEntrar() }

        btnIrTemplo.setOnClickListener {
            val nombre = Prefs.leerNombreJardin(this) ?: inputNombre.text.toString().trim()
            startActivity(Intent(this, JardinActivity::class.java).putExtra(JardinActivity.EXTRA_NOMBRE, nombre))
        }
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
                        btnIrTemplo.visibility = View.VISIBLE
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
