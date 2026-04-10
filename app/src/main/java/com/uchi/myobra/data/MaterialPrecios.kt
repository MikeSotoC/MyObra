package com.uchi.myobra.data

/**
 * Precios de todos los materiales y mano de obra.
 * El acero tiene precio INDIVIDUAL por diámetro de barra.
 */
data class MaterialPrecios(
    val cementoBolsa: Double    = 26.0,   // S/. / bolsa 42.5 kg
    val arenaM3: Double         = 55.0,   // S/. / m³
    val piedraM3: Double        = 65.0,   // S/. / m³
    val aguaM3: Double          = 5.0,    // S/. / m³
    val hormigonM3: Double      = 45.0,   // S/. / m³
    val piedraGrandM3: Double   = 50.0,   // S/. / m³
    val ladrilloKK: Double      = 1.20,   // S/. / und KK 18H
    val ladrilloHueco: Double   = 1.50,   // S/. / und hueco losa
    val ladrilloCaseton: Double = 3.20,   // S/. / und casetón
    // Mano de Obra (S/. / HH) — tarifas CAPECO vigentes
    val moOperario: Double      = 24.89,
    val moOficial: Double       = 20.16,
    val moPeon: Double          = 18.14,
    // ── Acero: precio por diámetro (S/. / kg) ────────────────────────────
    // Las barras más delgadas suelen costar más por kg por el habilitado
    val acero6mm: Double        = 5.80,   // Ø6mm  (1/4")
    val acero8mm: Double        = 5.50,   // Ø8mm  (5/16")
    val acero3_8: Double        = 4.90,   // Ø3/8" — más común en estribos
    val acero1_2: Double        = 4.60,   // Ø1/2" — longitudinal columnas
    val acero5_8: Double        = 4.50,   // Ø5/8" — vigas principales
    val acero3_4: Double        = 4.40,   // Ø3/4"
    val acero1: Double          = 4.30,   // Ø1"   — columnas grandes
)

/**
 * Detalle de una partida de acero — soporta dos modos:
 *
 * MODO BASTONES (esDistribuido = false):
 *   `NØd`  ej: 8Ø1/2" — N° varillas × longitud c/u
 *
 * MODO DISTRIBUCIÓN @ (esDistribuido = true):
 *   `Ød@s` ej: Ø3/8"@0.20 — diámetro @ espaciado (m)
 *   N calculado = trunc(longitudTramo / espaciado) + 1
 *   longitud = longitud por varilla (del plano)
 */
data class FilaAcero(
    val diametro: String,
    val cantidad: Int         = 1,     // bastones: N° varillas | distribución: ignorado
    val longitud: Double      = 9.0,   // bastones: m/varilla  | distribución: m/varilla (del plano)
    val pesoKgM: Double       = 0.994, // kg/ml ASTM A615
    val esDistribuido: Boolean= false, // true = modo @espaciado
    val espaciado: Double     = 0.20,  // m — solo en modo distribución
    val longitudTramo: Double = 0.0    // m — longitud del tramo (para contar N)
) {
    /** N° de varillas reales */
    val cantidadReal: Int get() = if (esDistribuido && espaciado > 0 && longitudTramo > 0)
        (longitudTramo / espaciado).toInt() + 1
    else cantidad

    /** kg totales de esta partida */
    val totalKg: Double get() = cantidadReal * longitud * pesoKgM

    /** Notación como aparece en el plano estructural */
    val notacion: String get() = if (esDistribuido)
        "${diametro.trim()}@${"%.2f".format(espaciado)}m"
    else
        "${cantidadReal}${diametro.trim()}"

    /** Descripción larga para el presupuesto */
    val descripcionLarga: String get() = if (esDistribuido)
        "${notacion} — ${"%.2f".format(longitudTramo)}m = ${cantidadReal} var. × ${"%.2f".format(longitud)}m"
    else
        "${notacion} — ${cantidadReal} var. × ${"%.2f".format(longitud)}m"
}

/**
 * Resultado completo de un cálculo de sección.
 * El acero viene del metrado real de barras — no estimado.
 */
data class ResultadoCalculo(
    val volumenM3: Double       = 0.0,
    val cemento: Double         = 0.0,   // bolsas
    val arena: Double           = 0.0,   // m³
    val piedra: Double          = 0.0,   // m³
    val agua: Double            = 0.0,   // m³
    val hormigon: Double        = 0.0,   // m³
    val piedraGrande: Double    = 0.0,   // m³
    val filasAcero: List<FilaAcero> = emptyList(), // metrado real
    val aceroTotalKg: Double    = 0.0,   // suma kg de todas las barras
    val ladrillo: Int           = 0,     // und
    val costoCemento: Double    = 0.0,
    val costoArena: Double      = 0.0,
    val costoPiedra: Double     = 0.0,
    val costoAgua: Double       = 0.0,
    val costoHormigon: Double   = 0.0,
    val costoPiedraGrande: Double = 0.0,
    val costoAcero: Double      = 0.0,   // suma de costos por barra (precio × kg)
    val costoLadrillo: Double   = 0.0,
    val costoManoObra: Double   = 0.0,
    val totalMateriales: Double = 0.0,
    val totalObra: Double       = 0.0,
    val isValid: Boolean        = false,
    val descripcion: String     = ""
) {
    /** Compatibilidad con código que usaba .acero */
    val acero: Double get() = aceroTotalKg
}
