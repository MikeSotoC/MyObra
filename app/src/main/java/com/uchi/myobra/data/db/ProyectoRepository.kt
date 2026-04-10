package com.uchi.myobra.data.db

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.uchi.myobra.data.FilaAcero
import com.uchi.myobra.data.ResultadoCalculo

/**
 * Capa de acceso a datos para proyectos y secciones.
 * Convierte entre ResultadoCalculo (dominio) ↔ SeccionCalculo (Room).
 */
class ProyectoRepository(context: Context) {

    private val db       = AppDatabase.getInstance(context)
    private val proyDao  = db.proyectoDao()
    private val secDao   = db.seccionDao()
    private val gson     = Gson()

    // ── Proyectos ──────────────────────────────────────────────────────

    fun getAllLive() = proyDao.getAllLive()

    suspend fun getAll()             = proyDao.getAll()
    suspend fun getById(id: Long)    = proyDao.getById(id)
    suspend fun insert(p: Proyecto)  = proyDao.insert(p)
    suspend fun update(p: Proyecto)  = proyDao.update(p)
    suspend fun delete(p: Proyecto)  = proyDao.delete(p)
    suspend fun deleteById(id: Long) = proyDao.deleteById(id)

    // ── Secciones ──────────────────────────────────────────────────────

    /**
     * Carga todas las secciones de un proyecto como ResultadoCalculo.
     * Retorna un mapa tipo → ResultadoCalculo.
     */
    suspend fun cargarSecciones(proyectoId: Long): Map<String, ResultadoCalculo> {
        return secDao.getByProyecto(proyectoId).associate { s ->
            s.tipo to s.toResultado()
        }
    }

    /** Guarda/actualiza una sección en la DB. */
    suspend fun guardarSeccion(proyectoId: Long, tipo: String, resultado: ResultadoCalculo) {
        if (!resultado.isValid) {
            secDao.delete(proyectoId, tipo)
            return
        }
        secDao.upsert(resultado.toSeccion(proyectoId, tipo))
        // Actualizar fechaModificacion del proyecto
        proyDao.getById(proyectoId)?.let { p ->
            proyDao.update(p.copy(fechaModificacion = System.currentTimeMillis()))
        }
    }

    /** Elimina todas las secciones de un proyecto. */
    suspend fun borrarSecciones(proyectoId: Long) = secDao.deleteAll(proyectoId)

    // ── Conversión dominio ↔ Room ───────────────────────────────────────

    private fun SeccionCalculo.toResultado(): ResultadoCalculo {
        val filas: List<FilaAcero> = try {
            val type = object : TypeToken<List<FilaAcero>>() {}.type
            gson.fromJson(filasAceroJson, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }

        return ResultadoCalculo(
            volumenM3       = volumenM3,
            cemento         = cemento,
            arena           = arena,
            piedra          = piedra,
            agua            = agua,
            hormigon        = hormigon,
            piedraGrande    = piedraGrande,
            filasAcero      = filas,
            aceroTotalKg    = aceroTotalKg,
            ladrillo        = ladrillo,
            costoCemento    = costoCemento,
            costoArena      = costoArena,
            costoPiedra     = costoPiedra,
            costoAgua       = costoAgua,
            costoHormigon   = costoHormigon,
            costoPiedraGrande = costoPiedraGrande,
            costoAcero      = costoAcero,
            costoLadrillo   = costoLadrillo,
            costoManoObra   = costoManoObra,
            totalMateriales = totalMateriales,
            totalObra       = totalObra,
            isValid         = true,
            descripcion     = descripcion
        )
    }

    private fun ResultadoCalculo.toSeccion(proyectoId: Long, tipo: String) = SeccionCalculo(
        proyectoId      = proyectoId,
        tipo            = tipo,
        descripcion     = descripcion,
        volumenM3       = volumenM3,
        cemento         = cemento,
        arena           = arena,
        piedra          = piedra,
        agua            = agua,
        hormigon        = hormigon,
        piedraGrande    = piedraGrande,
        aceroTotalKg    = aceroTotalKg,
        ladrillo        = ladrillo,
        filasAceroJson  = gson.toJson(filasAcero),
        costoCemento    = costoCemento,
        costoArena      = costoArena,
        costoPiedra     = costoPiedra,
        costoAgua       = costoAgua,
        costoHormigon   = costoHormigon,
        costoPiedraGrande = costoPiedraGrande,
        costoAcero      = costoAcero,
        costoLadrillo   = costoLadrillo,
        costoManoObra   = costoManoObra,
        totalMateriales = totalMateriales,
        totalObra       = totalObra
    )

    companion object {
        @Volatile private var INSTANCE: ProyectoRepository? = null
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: ProyectoRepository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
