package com.uchi.myobra.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uchi.myobra.data.db.Proyecto
import com.uchi.myobra.data.db.ProyectoRepository
import kotlinx.coroutines.launch

/**
 * ViewModel compartido entre todos los fragments de cálculo.
 *
 * - Mantiene el proyecto activo [proyectoActivo]
 * - Persiste cada sección en Room automáticamente cuando cambia
 * - Expone LiveData para que PresupuestoFragment observe cambios
 */
class ObraViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProyectoRepository.getInstance(app)

    // ── Proyecto activo ────────────────────────────────────────────────
    private val _proyectoActivo = MutableLiveData<Proyecto?>()
    val proyectoActivo: LiveData<Proyecto?> = _proyectoActivo

    val proyectoId: Long get() = _proyectoActivo.value?.id ?: 0L

    // ── Secciones de cálculo ───────────────────────────────────────────
    val zapatas       = MutableLiveData<ResultadoCalculo>()
    val cimientos     = MutableLiveData<ResultadoCalculo>()
    val sobrecimiento = MutableLiveData<ResultadoCalculo>()
    val muros         = MutableLiveData<ResultadoCalculo>()
    val falsopiso     = MutableLiveData<ResultadoCalculo>()
    val columnas      = MutableLiveData<ResultadoCalculo>()
    val vigas         = MutableLiveData<ResultadoCalculo>()
    val losas         = MutableLiveData<ResultadoCalculo>()

    // Mapea tipo → LiveData para persistencia genérica
    private val seccionMap by lazy {
        mapOf(
            "zapatas"       to zapatas,
            "cimientos"     to cimientos,
            "sobrecimiento" to sobrecimiento,
            "muros"         to muros,
            "falsopiso"     to falsopiso,
            "columnas"      to columnas,
            "vigas"         to vigas,
            "losas"         to losas
        )
    }

    // ── Abrir un proyecto ──────────────────────────────────────────────

    fun abrirProyecto(proyecto: Proyecto) {
        _proyectoActivo.value = proyecto
        // Limpia los valores anteriores
        seccionMap.values.forEach { it.value = ResultadoCalculo() }
        // Carga las secciones desde Room
        viewModelScope.launch {
            val secciones = repo.cargarSecciones(proyecto.id)
            secciones.forEach { (tipo, resultado) ->
                seccionMap[tipo]?.postValue(resultado)
            }
        }
    }

    fun cerrarProyecto() {
        _proyectoActivo.value = null
        seccionMap.values.forEach { it.value = ResultadoCalculo() }
    }

    // ── Persistir sección ──────────────────────────────────────────────

    /**
     * Guarda una sección en Room y actualiza el LiveData.
     * Llamar desde cada Fragment en lugar de asignar directamente al LiveData.
     */
    fun guardarSeccion(tipo: String, resultado: ResultadoCalculo) {
        seccionMap[tipo]?.value = resultado
        val pid = proyectoId
        if (pid > 0L) {
            viewModelScope.launch {
                repo.guardarSeccion(pid, tipo, resultado)
            }
        }
    }

    // ── Lista de proyectos ─────────────────────────────────────────────
    val todosLosProyectos: LiveData<List<Proyecto>> = repo.getAllLive()

    fun crearProyecto(nombre: String, propietario: String, ubicacion: String, descripcion: String) {
        viewModelScope.launch {
            val id = repo.insert(Proyecto(
                nombre      = nombre,
                propietario = propietario,
                ubicacion   = ubicacion,
                descripcion = descripcion
            ))
            val nuevo = repo.getById(id)
            if (nuevo != null) abrirProyecto(nuevo)
        }
    }

    fun actualizarProyecto(proyecto: Proyecto) {
        viewModelScope.launch {
            repo.update(proyecto.copy(fechaModificacion = System.currentTimeMillis()))
            if (_proyectoActivo.value?.id == proyecto.id) {
                _proyectoActivo.postValue(proyecto)
            }
        }
    }

    fun eliminarProyecto(proyecto: Proyecto) {
        viewModelScope.launch {
            repo.delete(proyecto)
            if (_proyectoActivo.value?.id == proyecto.id) cerrarProyecto()
        }
    }
}
