package com.fenix.aprendiz

/**
 * Las 12 Casas de Israel. Cada tribu está anclada a su color en colors.xml
 * (tribu_*) y a un texto breve basado en las bendiciones tradicionales de
 * Jacob (Génesis 49) y Moisés (Deuteronomio 33) — no son símbolos
 * inventados, son los atributos que la tradición ya les dio.
 *
 * numeroEsencia: el valor de calcular_datos_alma (codigo_espiritual + camino,
 * reducido) que asigna a esta tribu. Solo puede ser 1-9, 11, 22 o 33 —
 * exactamente 12 valores para 12 tribus, sin necesidad de mod arbitrario.
 *
 * consejo: la palabra breve para el día a día — el consejo que se muestra en
 * la pantalla de Inicio, junto al emblema y el nombre de la tribu. No repite
 * la bendición de `texto`; es su versión práctica, aplicable hoy mismo.
 *
 * emblemaResName: nombre del drawable vectorial ("emblema_<id>") con el sello
 * grabado de esta tribu, mostrado en el emblema respirante de Inicio.
 */
data class Tribu(
    val id: String,
    val nombre: String,
    val numeroEsencia: Int,
    val colorResName: String,
    val simbolo: String,
    val texto: String,
    val consejo: String
) {
    val emblemaResName: String get() = "emblema_$id"
}

object Tribus {

    val TODAS: List<Tribu> = listOf(
        Tribu(
            id = "ruben",
            nombre = "Rubén",
            numeroEsencia = 1,
            colorResName = "tribu_ruben",
            simbolo = "Aguas impetuosas",
            texto = "Rubén, primogénito, mi fuerza y primicia de mi vigor. Impetuoso como las aguas, no sobresaldrás — así lo dijo Jacob, y así carga esta casa el peso de empezar sin siempre llegar primero. Es la tribu del ímpetu bruto, del que abre camino aunque otro lo termine. Tu don es la fuerza inicial: la que no espera permiso para moverse.",
            consejo = "No busques ser siempre el primero: hoy se te pide constancia, no urgencia. Deja que tu fuerza fluya despacio, como el agua que talla la piedra sin prisa."
        ),
        Tribu(
            id = "simeon",
            nombre = "Simeón",
            numeroEsencia = 2,
            colorResName = "tribu_simeon",
            simbolo = "Espada y dispersión",
            texto = "Simeón y Leví son hermanos; armas de iniquidad hay en sus armas. Jacob los dispersó en Israel por la ira de su espada. Simeón es la tribu de la intensidad que corta — el fuego que si no se dirige, quema al que lo lleva. Tu don es la pasión sin tibieza: la que defiende, aunque a veces sin medida.",
            consejo = "Antes de que hable tu pasión, respira tres veces. La misma espada que defiende puede herir a quien amas si no la envainas a tiempo."
        ),
        Tribu(
            id = "levi",
            nombre = "Leví",
            numeroEsencia = 3,
            colorResName = "tribu_levi",
            simbolo = "Sacerdocio y servicio al altar",
            texto = "Leví no recibió tierra porque el Eterno mismo es su heredad. De esta casa salieron los sacerdotes: los que enseñan la Torá a Jacob y la ley a Israel, los que ponen incienso delante del Altísimo. Tu don es el servicio sagrado: vivir para sostener lo que otros solo visitan.",
            consejo = "Hoy sirve sin esperar aplauso. Lo sagrado no se mide en reconocimiento, sino en la fidelidad de sostener el altar cuando nadie mira."
        ),
        Tribu(
            id = "juda",
            nombre = "Judá",
            numeroEsencia = 4,
            colorResName = "tribu_juda",
            simbolo = "León y cetro",
            texto = "Judá, cachorro de león; de la presa subiste, hijo mío. No se apartará el cetro de Judá hasta que venga Siloh, y a él se congregarán los pueblos. Es la tribu real, la de la voz de mando y el liderazgo que otros siguen sin que se les obligue. Tu don es la realeza que se gana, no la que se hereda por nombre.",
            consejo = "Lidera hoy con el ejemplo, no con la orden. El cetro que no se apoya en la humildad, tarde o temprano, se quiebra."
        ),
        Tribu(
            id = "isacar",
            nombre = "Isacar",
            numeroEsencia = 5,
            colorResName = "tribu_isacar",
            simbolo = "Asno fuerte entre los apriscos",
            texto = "Isacar, asno huesudo que descansa entre los apriscos; vio que el sosiego era bueno, y bajó su hombro para llevar la carga. Es la tribu del estudio y el trabajo constante — de los que entienden los tiempos y saben lo que Israel debe hacer. Tu don es la resistencia serena: la que carga sin quejarse porque conoce el porqué.",
            consejo = "No cargues solo/a lo que puedes compartir. Descansar también es sabiduría: el hombro fuerte también necesita soltar el peso."
        ),
        Tribu(
            id = "zabulon",
            nombre = "Zabulón",
            numeroEsencia = 6,
            colorResName = "tribu_zabulon",
            simbolo = "Puerto de naves",
            texto = "Zabulón junto a puerto de mar habitará, y será para puerto de naves. Es la tribu que se abre al mundo, la que comercia, viaja y conecta lo que estaba separado. Tu don es el movimiento fértil: llevar lo propio hacia afuera y traer de vuelta lo que hace falta.",
            consejo = "Sal hoy de tu puerto seguro. Lo que aprendas afuera siempre vuelve a nutrir tu propia casa."
        ),
        Tribu(
            id = "dan",
            nombre = "Dan",
            numeroEsencia = 7,
            colorResName = "tribu_dan",
            simbolo = "Serpiente en el camino, juez de su pueblo",
            texto = "Dan juzgará a su pueblo como una de las tribus de Israel; será serpiente junto al camino, que muerde los talones del caballo. Es la tribu de la astucia y el discernimiento — la que no vence por fuerza sino por saber dónde golpear. Tu don es la justicia aguda: ver lo que otros no ven venir.",
            consejo = "Usa hoy tu discernimiento para proteger, nunca para herir desde la sombra. La justicia que no se muestra a la luz deja de ser justicia."
        ),
        Tribu(
            id = "gad",
            nombre = "Gad",
            numeroEsencia = 8,
            colorResName = "tribu_gad",
            simbolo = "Tropa vencedora",
            texto = "Gad, ejército lo acometerá; mas él acometerá al fin. Es la tribu del guerrero que resiste el embate y devuelve el golpe con más fuerza que el que lo recibió. Tu don es la perseverancia en la batalla: no ganar siempre primero, sino ganar al final.",
            consejo = "La batalla de hoy no se gana en el primer golpe: se gana en el último. No abandones cuando parezca que el embate te vence."
        ),
        Tribu(
            id = "aser",
            nombre = "Aser",
            numeroEsencia = 9,
            colorResName = "tribu_aser",
            simbolo = "Pan abundante, manjares de rey",
            texto = "El pan de Aser será substancioso, y él dará deleites de rey. Es la tribu de la abundancia que se comparte — la tierra fértil que no guarda para sí, sino que alimenta a los demás. Tu don es la generosidad que nutre: dar de lo que te sobra sin que se te note el sacrificio.",
            consejo = "Comparte hoy algo de lo que tienes, aunque sea pequeño. Tu abundancia crece cuando alimenta a otro, no cuando se guarda."
        ),
        Tribu(
            id = "neftali",
            nombre = "Neftalí",
            numeroEsencia = 11,
            colorResName = "tribu_neftali",
            simbolo = "Cierva suelta, palabras hermosas",
            texto = "Neftalí, cierva suelta, que pronunciará dichos hermosos. Es la tribu de la libertad ágil y la palabra que llega lejos — la que corre sin que nada la ate y habla con una gracia que desarma. Tu don es la ligereza con propósito: moverte libre y, aun así, dejar huella con lo que dices.",
            consejo = "Cuida las palabras que sueltas hoy: corren tan libres como tú, y dejan huella en quien las recibe."
        ),
        Tribu(
            id = "jose",
            nombre = "José",
            numeroEsencia = 22,
            colorResName = "tribu_jose",
            simbolo = "Rama fructífera junto a manantial",
            texto = "Rama fructífera es José, rama fructífera junto a manantial, cuyos vástagos se extienden sobre el muro. Efraín y Manasés, sus dos casas, cargan la misma bendición: la del que fue quebrado y se levantó más fuerte que antes. Tu don es la fecundidad después de la prueba: florecer justo donde otros esperaban que te secaras.",
            consejo = "Lo que te quebró no fue el final. Vuelve a mirar tu propia historia: donde hubo herida, hoy puede haber fruto."
        ),
        Tribu(
            id = "benjamin",
            nombre = "Benjamín",
            numeroEsencia = 33,
            colorResName = "tribu_benjamin",
            simbolo = "Lobo arrebatador, hijo de la mano derecha",
            texto = "Benjamín, lobo arrebatador; a la mañana comerá la presa, y a la tarde repartirá los despojos. Es la última tribu, la del hijo nacido en el dolor de Raquel y renombrado 'hijo de la diestra' por Jacob. Tu don es la fuerza que se templa: la que ataca al amanecer y aprende, al caer la tarde, a compartir lo que ganó.",
            consejo = "La fuerza que hoy uses para tomar, úsala también para repartir. Lo que se gana en la mañana se vuelve bendición si se comparte al caer la tarde."
        )
    )

    /**
     * Reduce codigo_espiritual + camino por dígitos, respetando los números
     * maestros 11, 22 y 33 (igual que hace calcular_datos_alma en el
     * descifrador). El resultado siempre cae en uno de los 12 valores que
     * ya cubren las tribus de arriba.
     */
    fun calcularNumeroEsencia(codigoEspiritual: Int, camino: Int): Int {
        var n = codigoEspiritual + camino
        while (n > 9 && n != 11 && n != 22 && n != 33) {
            n = n.toString().sumOf { it.digitToInt() }
        }
        return n
    }

    fun asignar(codigoEspiritual: Int, camino: Int): Tribu {
        val numero = calcularNumeroEsencia(codigoEspiritual, camino)
        return TODAS.firstOrNull { it.numeroEsencia == numero }
            ?: TODAS.first() // salvaguarda: nunca debería pasar con la reducción de arriba
    }

    fun porId(id: String): Tribu? = TODAS.firstOrNull { it.id == id }

    /** Devuelve la casa cuyo numeroEsencia coincide (1-9, 11, 22, 33). */
    fun porNumero(numero: Int): Tribu? = TODAS.firstOrNull { it.numeroEsencia == numero }
}
