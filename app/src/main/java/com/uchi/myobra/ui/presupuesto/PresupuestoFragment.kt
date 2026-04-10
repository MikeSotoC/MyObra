package com.uchi.myobra.ui.presupuesto

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.uchi.myobra.R
import com.uchi.myobra.data.ResultadoCalculo
import com.uchi.myobra.databinding.FragmentPresupuestoBinding
import com.uchi.myobra.ui.base.BaseSeccionFragment
import com.uchi.myobra.util.PdfGenerator
import java.io.File

class PresupuestoFragment : BaseSeccionFragment() {

    private var _b: FragmentPresupuestoBinding? = null
    private val b get() = _b!!

    // Current section list — updated on every observe
    private var seccionesActivas: List<Pair<String, ResultadoCalculo>> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentPresupuestoBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = com.uchi.myobra.data.PreciosRepository.getInstance(requireContext())

        // Observe all 8 sections
        obraViewModel.zapatas.observe(viewLifecycleOwner)      { rebuild() }
        obraViewModel.cimientos.observe(viewLifecycleOwner)    { rebuild() }
        obraViewModel.sobrecimiento.observe(viewLifecycleOwner){ rebuild() }
        obraViewModel.muros.observe(viewLifecycleOwner)        { rebuild() }
        obraViewModel.falsopiso.observe(viewLifecycleOwner)    { rebuild() }
        obraViewModel.columnas.observe(viewLifecycleOwner)     { rebuild() }
        obraViewModel.vigas.observe(viewLifecycleOwner)        { rebuild() }
        obraViewModel.losas.observe(viewLifecycleOwner)        { rebuild() }

        b.btnExportarPdf.setOnClickListener { exportarPdf() }
    }

    // ── Table construction ────────────────────────────────────────────────

    private fun rebuild() {
        seccionesActivas = listOf(
            "01 — Zapatas y Solados"   to obraViewModel.zapatas.value,
            "02 — Cimientos Corridos"  to obraViewModel.cimientos.value,
            "03 — Sobrecimientos"      to obraViewModel.sobrecimiento.value,
            "04 — Muros de Ladrillos"  to obraViewModel.muros.value,
            "05 — Falso Piso"          to obraViewModel.falsopiso.value,
            "06 — Columnas"            to obraViewModel.columnas.value,
            "07 — Vigas"               to obraViewModel.vigas.value,
            "08 — Losas Aligeradas"    to obraViewModel.losas.value
        ).filter { (_, r) -> r != null && r.isValid }
            .mapNotNull { (n, r) -> r?.let { n to it } }

        if (seccionesActivas.isEmpty()) {
            b.tvSinDatos.visibility           = View.VISIBLE
            b.cardPresupuestoGeneral.visibility = View.GONE
            b.btnExportarPdf.isEnabled        = false
            b.btnExportarPdf.alpha            = 0.5f
            return
        }

        b.tvSinDatos.visibility           = View.GONE
        b.cardPresupuestoGeneral.visibility = View.VISIBLE
        b.btnExportarPdf.isEnabled        = true
        b.btnExportarPdf.alpha            = 1f

        b.containerPartidas.removeAllViews()

        var totalMat = 0.0
        var totalMO  = 0.0

        seccionesActivas.forEachIndexed { idx, (nombre, r) ->
            addPartidaRow(nombre, r.totalMateriales, r.costoManoObra, r.totalObra, idx % 2 == 0)
            totalMat += r.totalMateriales
            totalMO  += r.costoManoObra
        }

        b.tvTotalMateriales.text = totalMat.fmtS()
        b.tvTotalManoObra.text   = totalMO.fmtS()
        b.tvGranTotal.text       = (totalMat + totalMO).fmtS()

        // Show project name in card
        val p = obraViewModel.proyectoActivo.value
        b.tvNombreProyectoPresup.text = p?.nombre ?: "Proyecto sin nombre"
        b.tvPropietarioPresup.text    = p?.propietario?.takeIf { it.isNotBlank() }?.let { "Propietario: $it" } ?: ""
        b.tvUbicacionPresup.text      = p?.ubicacion?.takeIf { it.isNotBlank() }?.let { "📍 $it" } ?: ""
    }

    private fun addPartidaRow(nombre: String, mat: Double, mo: Double, total: Double, alt: Boolean) {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(40)
            ).also { it.bottomMargin = dpToPx(1) }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
            if (alt) setBackgroundColor(ContextCompat.getColor(ctx, R.color.surface_page))
        }

        fun cell(text: String, weight: Float = 0f, w: Int = 0, bold: Boolean = false, color: Int? = null): TextView {
            return TextView(ctx).apply {
                this.text = text
                layoutParams = LinearLayout.LayoutParams(if (weight > 0f) 0 else w, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                textSize = 13f
                gravity = if (weight > 0f) Gravity.START else Gravity.END
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                if (color != null) setTextColor(color) else setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            }
        }

        row.addView(cell(nombre, weight = 1f))
        row.addView(cell(mat.fmtS(), w = dpToPx(100)))
        row.addView(cell(mo.fmtS(), w = dpToPx(90)))
        row.addView(cell(total.fmtS(), w = dpToPx(95), bold = true,
            color = ContextCompat.getColor(ctx, R.color.md_theme_light_primary)))

        b.containerPartidas.addView(row)
    }

    // ── PDF Export ────────────────────────────────────────────────────────

    private fun exportarPdf() {
        val proyecto = obraViewModel.proyectoActivo.value
        if (proyecto == null) {
            Snackbar.make(requireView(), "Abre un proyecto antes de exportar", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (seccionesActivas.isEmpty()) {
            Snackbar.make(requireView(), "No hay datos para exportar", Snackbar.LENGTH_SHORT).show()
            return
        }

        try {
            val file: File = PdfGenerator.generar(requireContext(), proyecto, seccionesActivas)
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Presupuesto — ${proyecto.nombre}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir presupuesto PDF"))
        } catch (e: Exception) {
            Snackbar.make(requireView(), "Error al generar PDF: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun Double.fmtS() = "S/. ${fmt.format(this)}"
    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
