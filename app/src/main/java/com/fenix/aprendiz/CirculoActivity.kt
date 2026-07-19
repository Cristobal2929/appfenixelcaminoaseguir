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
 * Círculo de Historias — el prado comunal del Jardín. La persona comparte
 * su experiencia y ve fluir las historias de TODA la comunidad (más
 * recientes arriba). El Space solo admite historias con palabras de luz
 * (amor, perdón, esperanza y afines); si no, devuelve un mensaje que
 * invita a reescribirla desde esa luz.
 *
 * Por ahora es texto (igual que el Templo empezó siendo texto). Imágenes y
 * audio son una capa posterior, cuando toque el diseño visual definitivo.
 */
class CirculoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOMBRE = "extra_nombre_jardin"
    }

    private lateinit var nombre: String
    private lateinit var inputHistoria: EditText
    private lateinit var progress: View
    private lateinit var estado: TextView
    private lateinit var listaHistorias: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circulo)

        nombre = intent.getStringExtra(EXTRA_NOMBRE)
            ?: Prefs.leerNombreJardin(this)
            ?: run {
                Toast.makeText(this, "Entra primero al Jardín.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

        inputHistoria = findViewById(R.id.inputHistoria)
        progress = findViewById(R.id.progressCirculo)
        estado = findViewById(R.id.estadoCirculo)
        listaHistorias = findViewById(R.id.listaHistorias)

        findViewById<Button>(R.id.btnCompartirHistoria).setOnClickListener { compartir() }
        findViewById<TextView>(R.id.btnVolverJardinCirculo).setOnClickListener { finish() }

        cargarHistorias()
    }

    private fun compartir() {
        val texto = inputHistoria.text.toString().trim()
        if (texto.isEmpty()) {
            mostrarEstado(getString(R.string.circulo_error_vacio))
            return
        }
        progress.visibility = View.VISIBLE
        estado.visibility = View.GONE

        CirculoApiClient.publicar(
            nombre = nombre,
            texto = texto,
            onResultado = { ok, mensaje ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    mostrarEstado(mensaje)
                    if (ok) {
                        inputHistoria.setText("")
                        cargarHistorias()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    mostrarEstado(getString(R.string.circulo_error_conexion, error))
                }
            }
        )
    }

    private fun cargarHistorias() {
        CirculoApiClient.listar(
            onResultado = { historias ->
                runOnUiThread {
                    notificarSiHayHistoriaNueva(historias)
                    listaHistorias.removeAllViews()
                    if (historias.isEmpty()) {
                        listaHistorias.addView(crearItem(getString(R.string.circulo_sin_historias), esVacio = true))
                        return@runOnUiThread
                    }
                    for (h in historias) {
                        listaHistorias.addView(crearItemHistoria(h.autor, h.texto))
                    }
                }
            },
            onError = {
                // Si falla el refresco no interrumpimos: la persona ya vio si
                // su historia se compartió o no.
            }
        )
    }

    /**
     * Compara con la última historia vista (guardada en Prefs) y, si hay
     * una nueva de OTRA persona, avisa con notificación (ícono + sonido).
     * No avisa la primera vez que se abre el Círculo en el dispositivo
     * (para no bombardear con todo el histórico de golpe).
     */
    private fun notificarSiHayHistoriaNueva(historias: List<CirculoApiClient.Historia>) {
        val masReciente = historias.firstOrNull() ?: return
        val marca = masReciente.creadaEn.ifBlank { masReciente.autor + "|" + masReciente.texto }
        val anterior = Prefs.leerUltimaMarcaCirculo(this)
        Prefs.guardarUltimaMarcaCirculo(this, marca)
        if (anterior != null && anterior != marca && masReciente.autor != nombre) {
            NotificationHelper.notificarHistoriaCirculo(this, masReciente.autor, masReciente.texto)
        }
    }

    /** Una historia = autor en dorado + texto debajo, con separación. */
    private fun crearItemHistoria(autor: String, texto: String): View {
        val bloque = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        bloque.addView(TextView(this).apply {
            text = autor
            setTextColor(getColor(R.color.teshua_dorado_suave))
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        bloque.addView(TextView(this).apply {
            text = texto
            setTextColor(getColor(R.color.teshua_texto))
            textSize = 15f
            setPadding(0, 4, 0, 0)
        })
        return bloque
    }

    private fun crearItem(texto: String, esVacio: Boolean): TextView {
        return TextView(this).apply {
            text = texto
            setTextColor(getColor(if (esVacio) R.color.teshua_texto_apagado else R.color.teshua_texto))
            textSize = 14f
            setPadding(0, 12, 0, 12)
        }
    }

    private fun mostrarEstado(texto: String) {
        estado.text = texto
        estado.visibility = View.VISIBLE
    }
}
