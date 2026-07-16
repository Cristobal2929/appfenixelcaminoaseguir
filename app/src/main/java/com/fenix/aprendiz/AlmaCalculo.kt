package com.fenix.aprendiz

import java.text.Normalizer

/**
 * Cálculo del alma — RÉPLICA EXACTA de calcular_datos_alma del Descifrador de
 * Código Teshuá (main.py). Se hace en el dispositivo para que sea instantáneo
 * y sin depender de que el Space esté encendido; da el mismo resultado que el
 * Descifrador oficial.
 *
 * Tabla, limpieza y reducción idénticas al Space:
 *   - VALORES_HEBREOS: valor de cada letra latina.
 *   - limpiar: MAYÚSCULAS, quita tildes (NFD), deja solo A-Z (Ñ→N).
 *   - codigo_espiritual: suma de letras, reducida (maestros 11/22/33 intactos).
 *   - camino: suma de los dígitos de la fecha, reducida igual.
 * La casa (tribu) se asigna con Tribus.asignar(codigo, camino), como ya hacía
 * la app.
 */
object AlmaCalculo {

    private val VALORES_HEBREOS: Map<Char, Int> = mapOf(
        'A' to 1, 'B' to 2, 'C' to 20, 'D' to 4, 'E' to 5, 'F' to 80, 'G' to 3,
        'H' to 5, 'I' to 10, 'J' to 10, 'K' to 20, 'L' to 30, 'M' to 40, 'N' to 50,
        'O' to 70, 'P' to 80, 'Q' to 100, 'R' to 200, 'S' to 60, 'T' to 400,
        'U' to 6, 'V' to 6, 'W' to 6, 'X' to 60, 'Y' to 10, 'Z' to 7
    )

    private fun limpiar(texto: String): String =
        Normalizer.normalize(texto.uppercase(), Normalizer.Form.NFD)
            .filter { it in 'A'..'Z' }

    private fun reducir(n: Int): Int {
        var x = n
        while (x > 9 && x != 11 && x != 22 && x != 33) {
            x = x.toString().sumOf { it.digitToInt() }
        }
        return x
    }

    /** Suma bruta de las letras del nombre (la "suma" que muestra el Descifrador). */
    fun sumaNombre(nombreCompleto: String): Int =
        limpiar(nombreCompleto).sumOf { VALORES_HEBREOS[it] ?: 0 }

    /** Código espiritual = suma del nombre, reducida. Es "tu código / número del alma". */
    fun codigoEspiritual(nombreCompleto: String): Int = reducir(sumaNombre(nombreCompleto))

    /** Camino de vida = suma de los dígitos de la fecha, reducida. */
    fun caminoDesdeFecha(dia: Int, mes: Int, anio: Int): Int {
        val suma = (dia.toString() + mes.toString() + anio.toString()).sumOf { it.digitToInt() }
        return reducir(suma)
    }

    data class DatosAlma(
        val tribu: Tribu,
        val codigo: Int,   // codigo_espiritual (lo que el Descifrador llama "tu código")
        val camino: Int,
        val suma: Int
    )

    fun descifrar(nombre: String, apellidos: String, dia: Int, mes: Int, anio: Int): DatosAlma {
        val completo = "$nombre $apellidos"
        val suma = sumaNombre(completo)
        val codigo = reducir(suma)
        val camino = caminoDesdeFecha(dia, mes, anio)
        val tribu = Tribus.asignar(codigo, camino)
        return DatosAlma(tribu = tribu, codigo = codigo, camino = camino, suma = suma)
    }
}
