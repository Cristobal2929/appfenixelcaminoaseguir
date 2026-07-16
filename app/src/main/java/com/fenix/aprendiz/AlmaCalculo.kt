package com.fenix.aprendiz

import java.text.Normalizer

/**
 * Cálculo del "número del alma" a partir del nombre + apellidos + fecha de
 * nacimiento, y su reducción a una de las 12 casas (vía Tribus.asignar).
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  OJO CRISTÓBAL — LA PARTE PROVISIONAL                                │
 * │  `codigoEspiritualProvisional` es un cálculo TEMPORAL de relleno     │
 * │  para poder ver el flujo funcionando ya. NO es tu gematría del       │
 * │  Descifrador de Código Teshuá. Cuando me pases cómo calcula el       │
 * │  Descifrador el `codigo_espiritual` (o su api_name para llamarlo     │
 * │  directo), se sustituye SOLO esta función y todo lo demás queda      │
 * │  igual. `camino` (fecha) sí es numerología estándar y es canónico.   │
 * └─────────────────────────────────────────────────────────────────────┘
 */
object AlmaCalculo {

    /** Reduce por dígitos respetando los maestros 11, 22 y 33 (igual que Tribu.kt). */
    private fun reducir(n: Int): Int {
        var x = n
        while (x > 9 && x != 11 && x != 22 && x != 33) {
            x = x.toString().sumOf { it.digitToInt() }
        }
        return x
    }

    /** Camino de vida: suma de todos los dígitos de la fecha, reducido. CANÓNICO. */
    fun caminoDesdeFecha(dia: Int, mes: Int, anio: Int): Int {
        val digitos = (dia.toString() + mes.toString() + anio.toString())
            .sumOf { it.digitToInt() }
        return reducir(digitos)
    }

    /** PROVISIONAL — reemplazar por la gematría real del Descifrador. */
    fun codigoEspiritualProvisional(nombreCompleto: String): Int {
        val limpio = Normalizer.normalize(nombreCompleto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("[^a-z]"), "")            // quita tildes, espacios, ñ→n ya normalizada
        val suma = limpio.sumOf { (it - 'a' + 1) }   // a=1 … z=26
        return reducir(suma)
    }

    /** Devuelve la casa (tribu) asignada + el número del alma ya reducido. */
    data class DatosAlma(val tribu: Tribu, val numeroAlma: Int, val codigo: Int, val camino: Int)

    fun descifrar(nombre: String, apellidos: String, dia: Int, mes: Int, anio: Int): DatosAlma {
        val codigo = codigoEspiritualProvisional("$nombre $apellidos")
        val camino = caminoDesdeFecha(dia, mes, anio)
        val tribu = Tribus.asignar(codigo, camino)
        return DatosAlma(tribu = tribu, numeroAlma = tribu.numeroEsencia, codigo = codigo, camino = camino)
    }
}
