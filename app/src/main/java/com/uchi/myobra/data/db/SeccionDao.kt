package com.uchi.myobra.data.db

import androidx.room.*

@Dao
interface SeccionDao {

    @Query("SELECT * FROM secciones WHERE proyectoId = :proyectoId")
    suspend fun getByProyecto(proyectoId: Long): List<SeccionCalculo>

    @Query("SELECT * FROM secciones WHERE proyectoId = :proyectoId AND tipo = :tipo LIMIT 1")
    suspend fun get(proyectoId: Long, tipo: String): SeccionCalculo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(seccion: SeccionCalculo): Long

    @Query("DELETE FROM secciones WHERE proyectoId = :proyectoId AND tipo = :tipo")
    suspend fun delete(proyectoId: Long, tipo: String)

    @Query("DELETE FROM secciones WHERE proyectoId = :proyectoId")
    suspend fun deleteAll(proyectoId: Long)
}
