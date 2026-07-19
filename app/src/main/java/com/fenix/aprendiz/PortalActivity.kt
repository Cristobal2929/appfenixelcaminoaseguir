package com.fenix.aprendiz

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

/**
 * Portal de Teshuá — la puerta de entrada.
 *
 * Pide nombre, apellidos y fecha de nacimiento ("la llave que lo abre todo")
 * y descifra la casa (tribu) del alma. Guarda la casa en el dispositivo y
 * pasa a la Revelación. Si ya hay casa guardada, SplashActivity ni siquiera
 * muestra esta pantalla.
 */
class PortalActivity : AppCompatActivity() {

    private lateinit var inputNombre: EditText
    private lateinit var inputApellidos: EditText
    private lateinit var campoFecha: TextView
    private lateinit var grupoSexo: RadioGroup
    private lateinit var progress: ProgressBar
    private lateinit var estado: TextView

    private var dia = -1
    private var mes = -1     // 1-12 (humano)
    private var anio = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portal)

        inputNombre = findViewById(R.id.inputNombrePortal)
        inputApellidos = findViewById(R.id.inputApellidosPortal)
        campoFecha = findViewById(R.id.inputFechaPortal)
        grupoSexo = findViewById(R.id.grupoSexoPortal)
        progress = findViewById(R.id.progressPortal)
        estado = findViewById(R.id.estadoPortal)

        campoFecha.setOnClickListener { elegirFecha() }
        findViewById<Button>(R.id.btnDescifrar).setOnClickListener { descifrar() }
    }

    private fun elegirFecha() {
        val hoy = Calendar.getInstance()
        val anioIni = if (anio > 0) anio else 1990
        val mesIni = if (mes > 0) mes - 1 else 0
        val diaIni = if (dia > 0) dia else 1
        val dlg = DatePickerDialog(this, { _, y, m, d ->
            anio = y; mes = m + 1; dia = d
            campoFecha.text = String.format("%02d / %02d / %04d", dia, mes, anio)
            campoFecha.setTextColor(getColor(R.color.teshua_texto))
        }, anioIni, mesIni, diaIni)
        dlg.datePicker.maxDate = hoy.timeInMillis
        dlg.show()
    }

    private fun descifrar() {
        val nombre = inputNombre.text.toString().trim()
        val apellidos = inputApellidos.text.toString().trim()

        if (nombre.isEmpty() || apellidos.isEmpty() || dia < 1) {
            estado.text = getString(R.string.portal_error_incompleto)
            estado.visibility = View.VISIBLE
            return
        }
        estado.visibility = View.GONE
        progress.visibility = View.VISIBLE

        val sexo = if (grupoSexo.checkedRadioButtonId == R.id.radioHija) "m" else "h"

        // Cálculo local (rápido). Cuando conectemos el Descifrador real,
        // aquí se hará la llamada al Space en un hilo y se leerá su respuesta.
        val datos = AlmaCalculo.descifrar(nombre, apellidos, dia, mes, anio)
        val fechaIso = String.format("%04d-%02d-%02d", anio, mes, dia)
        val nombreCompleto = "$nombre $apellidos"

        Prefs.guardarCasa(this, datos.tribu.numeroEsencia, sexo)
        Prefs.guardarNombreJardin(this, nombre)

        progress.visibility = View.GONE

        startActivity(
            Intent(this, RevelacionActivity::class.java)
                .putExtra(RevelacionActivity.EXTRA_NUMERO, datos.tribu.numeroEsencia)
                .putExtra(RevelacionActivity.EXTRA_CODIGO, datos.codigo)
                .putExtra(RevelacionActivity.EXTRA_SEXO, sexo)
                .putExtra(RevelacionActivity.EXTRA_NOMBRE, nombre)
                .putExtra(RevelacionActivity.EXTRA_NOMBRE_COMPLETO, nombreCompleto)
                .putExtra(RevelacionActivity.EXTRA_FECHA, fechaIso)
        )
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
