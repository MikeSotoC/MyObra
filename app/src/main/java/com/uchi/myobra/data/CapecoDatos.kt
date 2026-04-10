package com.uchi.myobra.data

/** Tablas CAPECO de proporciones y coeficientes de rendimiento */
object CapecoDatos {

    // ── Concreto Normal (Zapatas, Columnas, Vigas, Losas) ──────────────────
    data class ConcreteNormal(val fc: Int, val prop: String, val cemento: Double, val arena: Double, val piedra: Double, val agua: Double)

    val concretoNormal = listOf(
        ConcreteNormal(140, "1:2.5:3.5", 7.01, 0.56, 0.64, 0.184),
        ConcreteNormal(175, "1:2.5:2.5", 8.43, 0.54, 0.55, 0.185),
        ConcreteNormal(210, "1:2:2",     9.73, 0.52, 0.53, 0.186),
        ConcreteNormal(245, "1:1.5:1.5",11.50, 0.50, 0.51, 0.187),
        ConcreteNormal(280, "1:1:1.5",  13.34, 0.45, 0.51, 0.189)
    )

    // ── Concreto Ciclópeo (Cimientos, Sobrecimientos) ──────────────────────
    data class ConcretoCiclopeo(val proporcion: String, val cemento: Double, val hormigon: Double, val piedraGrande: Double, val agua: Double)

    val concretoCiclopeo = listOf(
        ConcretoCiclopeo("1:8 + 25% PG",  3.70, 0.85, 0.40, 0.13),
        ConcretoCiclopeo("1:10 + 30% PG", 2.90, 0.83, 0.48, 0.10)
    )

    val ciclopeoSobrecimiento = listOf(
        ConcretoCiclopeo("1:8 + 25% PM",  3.70, 0.85, 0.42, 0.13),
        ConcretoCiclopeo("1:10 + 30% PM", 2.90, 0.83, 0.48, 0.10)
    )

    // ── Concreto Simple (Falso Piso) ───────────────────────────────────────
    data class ConcretoSimple(val proporcion: String, val cemento: Double, val hormigon: Double, val agua: Double)

    val concretoFalsoPiso = listOf(
        ConcretoSimple("1:8",  5.00, 1.13, 0.17),
        ConcretoSimple("1:9",  4.60, 1.16, 0.16),
        ConcretoSimple("1:10", 4.20, 1.19, 0.14),
        ConcretoSimple("1:12", 3.60, 1.23, 0.12)
    )

    // ── Losas Aligeradas (materiales por m²) ──────────────────────────────
    data class LosaAligerada(
        val altura: Int, // cm
        val cemento: Double, val arena: Double, val piedra: Double, val agua: Double,
        val ladrilloHueco: Int, val aceroKg: Double
    )
    // Valores CAPECO por m² de losa aligerada en una dirección
    val losasAligeradas = listOf(
        LosaAligerada(17, 1.82, 0.12, 0.11, 0.019, 8, 2.50),
        LosaAligerada(20, 1.99, 0.13, 0.12, 0.021, 8, 2.80),
        LosaAligerada(25, 2.30, 0.15, 0.14, 0.024, 7, 3.20),
        LosaAligerada(30, 2.60, 0.17, 0.16, 0.027, 6, 3.80)
    )

    // ── Muros de Ladrillos (und/m²) ───────────────────────────────────────
    data class MuroLadrillo(val tipo: String, val aparejo: String, val ladrillos: Int, val cemento: Double, val arena: Double)

    val murosLadrillos = listOf(
        MuroLadrillo("KK 18H Artesanal (23x12.5x9cm)", "Soga",    45, 0.218, 0.035),
        MuroLadrillo("KK 18H Artesanal (23x12.5x9cm)", "Cabeza",  90, 0.345, 0.053),
        MuroLadrillo("KK 18H Industrial (24x13x9cm)",  "Soga",    39, 0.204, 0.033),
        MuroLadrillo("KK 18H Industrial (24x13x9cm)",  "Cabeza",  78, 0.325, 0.050)
    )

    // ── Acero de refuerzo ─────────────────────────────────────────────────
    /** Peso lineal de barras de acero ASTM A615 (kg/ml) */
    data class BarraAcero(val diam: String, val designation: String, val pesoKgM: Double)

    val barrasAcero = listOf(
        BarraAcero("Ø 6mm  (1/4\")",  "#2",  0.222),
        BarraAcero("Ø 8mm  (5/16\")", "#2.5",0.395),
        BarraAcero("Ø 3/8\" (9.5mm)", "#3",  0.560),
        BarraAcero("Ø 1/2\" (12mm)",  "#4",  0.994),
        BarraAcero("Ø 5/8\" (16mm)",  "#5",  1.552),
        BarraAcero("Ø 3/4\" (19mm)",  "#6",  2.235),
        BarraAcero("Ø 1\"   (25mm)",  "#8",  3.973)
    )

    data class FilaMetrado(val diam: String, val cantidad: Int, val longitud: Double) {
        fun pesoKg(barras: List<BarraAcero>): Double {
            val p = barras.firstOrNull { it.diam == diam }?.pesoKgM ?: 0.0
            return cantidad * longitud * p
        }
    }

    // Cuantías típicas referenciales CAPECO (kg/m³) — solo si no hay metrado real
    const val ACERO_ZAPATAS_KG_M3   = 60.0
    const val ACERO_COLUMNAS_KG_M3  = 180.0
    const val ACERO_VIGAS_KG_M3     = 160.0

    // ── Mano de Obra CAPECO – rendimientos ────────────────────────────────
    // HH (Horas-Hombre) por m³ concreto
    const val MO_CONCRETO_OPERARIO_HH_M3 = 4.44
    const val MO_CONCRETO_OFICIAL_HH_M3  = 4.44
    const val MO_CONCRETO_PEON_HH_M3     = 8.00

    // HH por m² muros
    const val MO_MURO_OPERARIO_HH_M2 = 0.800
    const val MO_MURO_PEON_HH_M2     = 0.534
}
