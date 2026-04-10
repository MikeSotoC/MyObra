package com.uchi.myobra.ui.base

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.uchi.myobra.R
import com.uchi.myobra.data.ObraViewModel
import com.uchi.myobra.data.PreciosRepository
import com.uchi.myobra.data.ResultadoCalculo
import java.text.NumberFormat
import java.util.Locale

abstract class BaseSeccionFragment : Fragment() {

    protected val obraViewModel: ObraViewModel by activityViewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            requireActivity().application
        )
    }
    protected lateinit var repo: PreciosRepository

    protected val fmt = NumberFormat.getNumberInstance(Locale("es", "PE")).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 2
    }
    protected val fmtInt = NumberFormat.getIntegerInstance(Locale("es", "PE"))

    protected fun Double.fmtS()   = "S/. ${fmt.format(this)}"
    protected fun Double.fmtM3()  = "${fmt.format(this)} m³"
    protected fun Double.fmtBls() = "${fmt.format(this)} bls"
    protected fun Double.fmtKg()  = "${fmt.format(this)} kg"
    protected fun Int.fmtUnd()    = "${fmtInt.format(this)} und"

    // ────────────────────────────────────────────────────────────────────────
    // B. SECCIÓN "CÁLCULO DE MATERIALES"
    // ────────────────────────────────────────────────────────────────────────

    protected fun mostrarResultadosCard(root: View, r: ResultadoCalculo) {
        val sinDatos = root.findViewById<View>(R.id.layoutSinDatos)
        val content  = root.findViewById<View>(R.id.layoutResultados)
        sinDatos?.visibility = if (r.isValid) View.GONE else View.VISIBLE
        content?.visibility  = if (r.isValid) View.VISIBLE else View.GONE
        if (!r.isValid) return

        // Volume badge
        root.findViewById<TextView>(R.id.tvVolumen)?.apply {
            text = if (r.volumenM3 > 0) r.volumenM3.fmtM3() else "—"
            visibility = View.VISIBLE
        }
        root.findViewById<TextView>(R.id.tvVolLabel)?.text =
            if (r.volumenM3 > 0) r.volumenM3.fmtM3() else "—"

        // Cemento
        root.findViewById<TextView>(R.id.tvCemento)?.text = fmt.format(r.cemento)

        // Arena / Hormigón (segunda columna)
        val arVal = if (r.arena > 0) r.arena else r.hormigon
        root.findViewById<TextView>(R.id.tvAreLabel)?.text = if (r.arena > 0) "ARENA" else "HORMIGÓN"
        root.findViewById<TextView>(R.id.tvArena)?.text    = fmt.format(arVal)

        // Piedra
        val pieVal = r.piedra
        val rowPA  = root.findViewById<View>(R.id.rowPiedraAgua)
        rowPA?.visibility = if (pieVal > 0) View.VISIBLE else View.GONE
        root.findViewById<TextView>(R.id.tvPieLabel)?.text = "PIEDRA"
        root.findViewById<TextView>(R.id.tvPiedra)?.text   = fmt.format(pieVal)

        // Agua
        root.findViewById<TextView>(R.id.tvAgua)?.text = fmt.format(r.agua)

        // Piedra Grande
        val rowPG = root.findViewById<View>(R.id.rowPiedraGrande)
        rowPG?.visibility = if (r.piedraGrande > 0) View.VISIBLE else View.GONE
        root.findViewById<TextView>(R.id.tvPiedraGrande)?.text = r.piedraGrande.fmtM3()

        // ── Acero: una línea por varilla con notación del plano ──────────
        val rowAcero = root.findViewById<View>(R.id.rowAcero)
        val containerAceroB = root.findViewById<LinearLayout>(R.id.containerAceroBCalculo)

        if (r.filasAcero.isNotEmpty()) {
            rowAcero?.visibility = View.VISIBLE
            containerAceroB?.removeAllViews()
            containerAceroB?.visibility = View.VISIBLE

            r.filasAcero.forEach { fila ->
                // Row: accent + notacion + N varillas + kg
                val row = LinearLayout(context ?: return@forEach).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(32)
                    )
                    orientation = LinearLayout.HORIZONTAL
                    gravity     = Gravity.CENTER_VERTICAL
                }
                // Accent dot
                View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(3), dpToPx(16)).also { it.marginEnd = dpToPx(10) }
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.mat_acero))
                }.also { row.addView(it) }
                // Notation: "8Ø1/2"" or "Ø3/8"@0.20m"
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text     = fila.descripcionLarga   // "8Ø1/2" — 8 var. × 9.00m"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.mat_acero))
                }.also { row.addView(it) }
                // N varillas
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(50), LinearLayout.LayoutParams.WRAP_CONTENT)
                    text     = "${fila.cantidadReal} var."
                    textSize = 11f
                    gravity  = Gravity.END
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.mat_acero))
                }.also { row.addView(it) }
                // kg
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(54), LinearLayout.LayoutParams.WRAP_CONTENT).also { it.marginEnd = dpToPx(4) }
                    text     = fila.totalKg.fmtKg()
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    gravity  = Gravity.END
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.mat_acero))
                }.also { row.addView(it) }

                containerAceroB?.addView(row)
            }

            // Total row
            val totalKg = r.filasAcero.sumOf { it.totalKg }
            val totalRow = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(30)).also { it.topMargin = dpToPx(2) }
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
            }
            View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(3), dpToPx(0)).also { it.marginEnd = dpToPx(10) }
            }.also { totalRow.addView(it) }
            TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text     = "Total acero habilitado"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.mat_acero))
            }.also { totalRow.addView(it) }
            TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(104), LinearLayout.LayoutParams.WRAP_CONTENT).also { it.marginEnd = dpToPx(4) }
                text     = totalKg.fmtKg()
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                gravity  = Gravity.END
                setTextColor(ContextCompat.getColor(requireContext(), R.color.mat_acero))
            }.also { totalRow.addView(it) }
            containerAceroB?.addView(totalRow)

        } else {
            rowAcero?.visibility = View.GONE
            containerAceroB?.removeAllViews()
            containerAceroB?.visibility = View.GONE
        }

        // Ladrillo
        val rowLad = root.findViewById<View>(R.id.rowLadrillo)
        rowLad?.visibility = if (r.ladrillo > 0) View.VISIBLE else View.GONE
        root.findViewById<TextView>(R.id.tvLadrillo)?.text = r.ladrillo.fmtUnd()
    }

    // ────────────────────────────────────────────────────────────────────────
    // C. SECCIÓN "PRESUPUESTO DE MATERIALES"
    // ────────────────────────────────────────────────────────────────────────

    protected fun mostrarPresupuestoCard(root: View, r: ResultadoCalculo) {
        val sinDatos = root.findViewById<View>(R.id.layoutSinDatosP)
        val content  = root.findViewById<View>(R.id.layoutPresupuesto)
        sinDatos?.visibility = if (r.isValid) View.GONE else View.VISIBLE
        content?.visibility  = if (r.isValid) View.VISIBLE else View.GONE
        if (!r.isValid) return

        val matCemento = ContextCompat.getColor(requireContext(), R.color.mat_cemento)
        val matArena   = ContextCompat.getColor(requireContext(), R.color.mat_arena)
        val matPiedra  = ContextCompat.getColor(requireContext(), R.color.mat_piedra)
        val matPG      = ContextCompat.getColor(requireContext(), R.color.mat_piedra_grande)
        val matAgua    = ContextCompat.getColor(requireContext(), R.color.mat_agua)
        val matLad     = ContextCompat.getColor(requireContext(), R.color.mat_ladrillo)
        val matAcero   = ContextCompat.getColor(requireContext(), R.color.mat_acero)

        fun setRow(rowId: Int, label: String, cant: String, sub: Double, accentColor: Int? = null) {
            val row = root.findViewById<View>(rowId) ?: return
            row.visibility = View.VISIBLE
            row.findViewById<TextView>(R.id.tvPCemPartida)?.text  = label
            row.findViewById<TextView>(R.id.tvPCemCantidad)?.text = cant
            row.findViewById<TextView>(R.id.tvPCemSubtotal)?.text = sub.fmtS()
            accentColor?.let { row.findViewById<View>(R.id.viewAccent)?.setBackgroundColor(it) }
        }

        setRow(R.id.pRowCemento, "Cemento 42.5kg", r.cemento.fmtBls(), r.costoCemento, matCemento)

        if (r.arena > 0)
            setRow(R.id.pRowArena, "Arena gruesa", r.arena.fmtM3(), r.costoArena, matArena)
        else
            setRow(R.id.pRowArena, "Hormigón", r.hormigon.fmtM3(), r.costoArena, matArena)

        val rowPiedra = root.findViewById<View>(R.id.pRowPiedra)
        rowPiedra?.visibility = if (r.piedra > 0) View.VISIBLE else View.GONE
        if (r.piedra > 0) setRow(R.id.pRowPiedra, "Piedra chancada", r.piedra.fmtM3(), r.costoPiedra, matPiedra)

        val rowPG = root.findViewById<View>(R.id.pRowPiedraGrande)
        rowPG?.visibility = if (r.piedraGrande > 0) View.VISIBLE else View.GONE
        if (r.piedraGrande > 0) setRow(R.id.pRowPiedraGrande, "Piedra grande (PG)", r.piedraGrande.fmtM3(), r.costoPiedraGrande, matPG)

        setRow(R.id.pRowAgua, "Agua", r.agua.fmtM3(), r.costoAgua, matAgua)

        val rowLad = root.findViewById<View>(R.id.pRowLadrillo)
        rowLad?.visibility = if (r.ladrillo > 0) View.VISIBLE else View.GONE
        if (r.ladrillo > 0) setRow(R.id.pRowLadrillo, "Ladrillo", r.ladrillo.fmtUnd(), r.costoLadrillo, matLad)

        // ── Acero: una línea de costo por varilla ───────────────────────
        val rowAceroStatic  = root.findViewById<View>(R.id.pRowAcero)
        val containerAceroC = root.findViewById<LinearLayout>(R.id.containerAceroRows)

        rowAceroStatic?.visibility = View.GONE

        if (r.filasAcero.isNotEmpty()) {
            containerAceroC?.removeAllViews()
            containerAceroC?.visibility = View.VISIBLE

            r.filasAcero.forEach { fila ->
                val prec  = repo.getPrecioAcero(fila.diametro)
                val costo = fila.totalKg * prec

                val row = LinearLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(36))
                    orientation  = LinearLayout.HORIZONTAL
                    gravity      = Gravity.CENTER_VERTICAL
                }
                // Accent bar
                View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(3), dpToPx(22)).also { it.marginEnd = dpToPx(10) }
                    setBackgroundColor(matAcero)
                }.also { row.addView(it) }
                // Label with plan notation: "8Ø1/2" — 8×9.00m = 71.57 kg"
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text     = fila.descripcionLarga
                    textSize = 12f
                    setTextColor(matAcero)
                }.also { row.addView(it) }
                // Cost
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(84), LinearLayout.LayoutParams.WRAP_CONTENT)
                    text     = costo.fmtS()
                    textSize = 13f
                    gravity  = Gravity.END
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(matAcero)
                }.also { row.addView(it) }

                containerAceroC?.addView(row)
            }
        } else {
            containerAceroC?.removeAllViews()
            containerAceroC?.visibility = View.GONE
        }

        // Mano de Obra
        root.findViewById<TextView>(R.id.tvPMOCantidad)?.text = "${fmt.format(r.volumenM3)} m³"
        root.findViewById<TextView>(R.id.tvPMOSubtotal)?.text = r.costoManoObra.fmtS()

        // Total
        root.findViewById<TextView>(R.id.tvTotalObra)?.text = r.totalObra.fmtS()
    }

    protected fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
