package com.uchi.myobra.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Resultado de cálculo de una sección vinculado a un proyecto.
 * El par (proyectoId, tipo) es único: hay una sola sección de cada tipo por proyecto.
 * filasAceroJson guarda List<FilaAcero> como JSON para preservar el metrado de barras.
 */
@Entity(
    tableName   = "secciones",
    foreignKeys = [ForeignKey(
        entity        = Proyecto::class,
        parentColumns = ["id"],
        childColumns  = ["proyectoId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["proyectoId", "tipo"], unique = true)]
)
data class SeccionCalculo(
    @PrimaryKey(autoGenerate = true)
    val id: Long            = 0L,
    val proyectoId: Long,
    val tipo: String,           // "zapatas" | "cimientos" | "sobrecimiento" | ...
    val descripcion: String     = "",
    // ── Cantidades ──────────────────────────────────────────────────────
    val volumenM3: Double       = 0.0,
    val cemento: Double         = 0.0,
    val arena: Double           = 0.0,
    val piedra: Double          = 0.0,
    val agua: Double            = 0.0,
    val hormigon: Double        = 0.0,
    val piedraGrande: Double    = 0.0,
    val aceroTotalKg: Double    = 0.0,
    val ladrillo: Int           = 0,
    val filasAceroJson: String  = "[]",  // JSON de List<FilaAcero>
    // ── Costos ──────────────────────────────────────────────────────────
    val costoCemento: Double    = 0.0,
    val costoArena: Double      = 0.0,
    val costoPiedra: Double     = 0.0,
    val costoAgua: Double       = 0.0,
    val costoHormigon: Double   = 0.0,
    val costoPiedraGrande: Double = 0.0,
    val costoAcero: Double      = 0.0,
    val costoLadrillo: Double   = 0.0,
    val costoManoObra: Double   = 0.0,
    val totalMateriales: Double = 0.0,
    val totalObra: Double       = 0.0
)
