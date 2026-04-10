package com.uchi.myobra.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProyectoDao {

    @Query("SELECT * FROM proyectos ORDER BY fechaModificacion DESC")
    fun getAllLive(): LiveData<List<Proyecto>>

    @Query("SELECT * FROM proyectos ORDER BY fechaModificacion DESC")
    suspend fun getAll(): List<Proyecto>

    @Query("SELECT * FROM proyectos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Proyecto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(proyecto: Proyecto): Long

    @Update
    suspend fun update(proyecto: Proyecto)

    @Delete
    suspend fun delete(proyecto: Proyecto)

    @Query("DELETE FROM proyectos WHERE id = :id")
    suspend fun deleteById(id: Long)
}
