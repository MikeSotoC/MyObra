package com.uchi.myobra.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa un proyecto de obra.
 * Cada proyecto tiene sus propios cálculos por sección.
 */
@Entity(tableName = "proyectos")
data class Proyecto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val nombre: String,
    val propietario: String   = "",
    val ubicacion: String     = "",
    val descripcion: String   = "",
    val fechaCreacion: Long   = System.currentTimeMillis(),
    val fechaModificacion: Long = System.currentTimeMillis()
)
