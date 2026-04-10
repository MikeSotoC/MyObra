package com.uchi.myobra.ui.base

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import com.google.android.material.chip.Chip
import com.uchi.myobra.R
import com.uchi.myobra.data.CapecoDatos
import com.uchi.myobra.data.FilaAcero
import com.uchi.myobra.data.PreciosRepository

/**
 * Gestiona filas de metrado de acero con dos modos:
 *
 * BASTONES   → NØd  ej: 8Ø1/2" — N° varillas × diámetro × longitud/varilla (default 9 m)
 * DISTRIBUCIÓN @ → Ød@s ej: Ø3/8"@0.20 — diámetro + @espaciado(m) + L tramo + L/varilla
 *
 * Los resultados (kg, S/.) van en las secciones B y C del fragment, no aquí.
 */
class AceroMetradoHelper(
    private val context: Context,
    private val container: LinearLayout,
    private val repo: PreciosRepository,
    private val onChanged: (filas: List<FilaAcero>, totalKg: Double, totalCosto: Double) -> Unit
) {
    private val barras   = CapecoDatos.barrasAcero
    private val diamOpts = barras.map { it.diam }

    /** Agrega una fila. [distribuido] = false → bastones, true → @spacing */
    fun addFila(
        diam: String  = barras[3].diam,  // default Ø1/2"
        distribuido: Boolean = false
    ) {
        val row = LayoutInflater.from(context)
            .inflate(R.layout.item_acero_fila, container, false)

        val dropDiam       = row.findViewById<AutoCompleteTextView>(R.id.dropDiam)
        val layoutBastones = row.findViewById<View>(R.id.layoutBastones)
        val layoutDistrib  = row.findViewById<View>(R.id.layoutDistribucion)
        val chipBastones   = row.findViewById<Chip>(R.id.chipBastones)
        val chipDistrib    = row.findViewById<Chip>(R.id.chipDistribucion)
        val etCantidad     = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCantidad)
        val etLongitud     = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etLongitud)
        val etEspaciado    = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEspaciado)
        val etTramo        = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTramo)
        val etLongDistrib  = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etLongDistrib)
        val btnDel         = row.findViewById<View>(R.id.btnEliminar)

        // Setup diameter dropdown
        dropDiam.setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, diamOpts))
        dropDiam.setText(diam, false)

        // Initial mode
        fun setMode(isDistrib: Boolean) {
            layoutBastones.visibility = if (isDistrib) View.GONE  else View.VISIBLE
            layoutDistrib.visibility  = if (isDistrib) View.VISIBLE else View.GONE
            notificar()
        }
        if (distribuido) {
            chipDistrib.isChecked = true
            chipBastones.isChecked = false
            setMode(true)
        }

        chipBastones.setOnCheckedChangeListener { _, checked -> if (checked) setMode(false) }
        chipDistrib.setOnCheckedChangeListener  { _, checked -> if (checked) setMode(true)  }

        // TextWatcher
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { notificar() }
        }
        dropDiam.setOnItemClickListener { _, _, _, _ -> notificar() }
        listOf(etCantidad, etLongitud, etEspaciado, etTramo, etLongDistrib)
            .forEach { it.addTextChangedListener(watcher) }

        btnDel.setOnClickListener { container.removeView(row); notificar() }

        container.addView(row)
        notificar()
    }

    fun notificar() {
        val filas      = getFilas()
        val totalKg    = filas.sumOf { it.totalKg }
        val totalCosto = filas.sumOf { it.totalKg * repo.getPrecioAcero(it.diametro) }
        onChanged(filas, totalKg, totalCosto)
    }

    fun getFilas(): List<FilaAcero> {
        val result = mutableListOf<FilaAcero>()
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i) ?: continue

            val diam      = row.findViewById<AutoCompleteTextView>(R.id.dropDiam)?.text.toString()
            val pesoKgM   = barras.firstOrNull { it.diam == diam }?.pesoKgM ?: 0.0
            val isDistrib = row.findViewById<Chip>(R.id.chipDistribucion)?.isChecked == true

            if (isDistrib) {
                // User enters spacing in cm (e.g. "20" means 0.20 m)
                val espaciadoRaw = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEspaciado)
                    ?.text.toString().toDoubleOrNull() ?: 0.0
                val tramo   = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTramo)
                    ?.text.toString().toDoubleOrNull() ?: 0.0
                val longVar = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etLongDistrib)
                    ?.text.toString().toDoubleOrNull() ?: 0.0

                // Normalize: user types "20" (cm) → 0.20 m; if already ≤1.0 assume already in meters
                val espaciadoM = if (espaciadoRaw > 1.0) espaciadoRaw / 100.0 else espaciadoRaw

                if (diam.isNotEmpty() && espaciadoM > 0 && tramo > 0 && longVar > 0) {
                    result.add(FilaAcero(
                        diametro      = diam,
                        cantidad      = 0,
                        longitud      = longVar,
                        pesoKgM       = pesoKgM,
                        esDistribuido = true,
                        espaciado     = espaciadoM,
                        longitudTramo = tramo
                    ))
                }
            } else {
                val cant = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCantidad)
                    ?.text.toString().toIntOrNull() ?: 0
                val long = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etLongitud)
                    ?.text.toString().toDoubleOrNull() ?: 0.0
                if (diam.isNotEmpty() && cant > 0 && long > 0)
                    result.add(FilaAcero(diam, cant, long, pesoKgM))
            }
        }
        return result
    }

    fun getTotalKg()    = getFilas().sumOf { it.totalKg }
    fun getTotalCosto() = getFilas().sumOf { it.totalKg * repo.getPrecioAcero(it.diametro) }
    fun clear()         { container.removeAllViews(); notificar() }
    fun isEmpty()       = container.childCount == 0
}
