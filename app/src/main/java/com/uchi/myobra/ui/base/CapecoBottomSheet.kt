package com.uchi.myobra.ui.base

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uchi.myobra.R
import com.uchi.myobra.data.CapecoDatos
import com.uchi.myobra.data.PreciosRepository

/**
 * BottomSheet que muestra tablas de referencia CAPECO.
 *
 * Modo = "normal"     → Concreto normal (zapatas, columnas, vigas)
 * Modo = "ciclopeo"   → Concreto ciclópeo (cimientos, sobrecimientos)
 * Modo = "falsopiso"  → Concreto simple falso piso
 * Modo = "muros"      → Ladrillos y morteros
 * Modo = "acero"      → Barras ASTM A615 pesos + precios
 * Modo = "losas"      → Losas aligeradas por m²
 */
class CapecoBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val ARG_MODO = "modo"
        fun newInstance(modo: String) = CapecoBottomSheet().apply {
            arguments = Bundle().also { it.putString(ARG_MODO, modo) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.bottom_sheet_capeco, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val modo      = arguments?.getString(ARG_MODO) ?: "normal"
        val container = view.findViewById<LinearLayout>(R.id.containerTablaCapeco)
        val tvTitulo  = view.findViewById<TextView>(R.id.tvCapTitulo)
        val tvSub     = view.findViewById<TextView>(R.id.tvCapSubtitulo)
        val secAcero  = view.findViewById<LinearLayout>(R.id.sectionAcero)
        val repo      = PreciosRepository.getInstance(requireContext())

        when (modo) {
            "normal" -> {
                tvTitulo.text = "Concreto Normal — CAPECO"
                tvSub.text    = "Materiales por m³ de concreto (fc en kg/cm²)"
                buildTablaConcreto(container)
            }
            "ciclopeo" -> {
                tvTitulo.text = "Concreto Ciclópeo — CAPECO"
                tvSub.text    = "Materiales por m³ (cimientos y sobrecimientos)"
                buildTablaCiclopeo(container)
            }
            "falsopiso" -> {
                tvTitulo.text = "Concreto Simple — CAPECO"
                tvSub.text    = "Materiales por m³ (falso piso)"
                buildTablaFalsoPiso(container)
            }
            "muros" -> {
                tvTitulo.text = "Muros de Ladrillo — CAPECO"
                tvSub.text    = "Materiales por m² según aparejo (Norma E.070)"
                buildTablaMuros(container)
            }
            "losas" -> {
                tvTitulo.text = "Losas Aligeradas — CAPECO"
                tvSub.text    = "Materiales por m² según altura de losa"
                buildTablaLosas(container)
            }
            "acero" -> {
                tvTitulo.text = "Acero de Refuerzo — ASTM A615"
                tvSub.text    = "Pesos lineales y precios actuales por diámetro"
                secAcero.visibility = View.VISIBLE
                val cont2 = view.findViewById<LinearLayout>(R.id.containerTablaAcero)
                buildTablaAcero(cont2, repo)
                // Keep main container empty
            }
        }
    }

    // ── Builders ──────────────────────────────────────────────────────────

    private fun buildTablaConcreto(parent: LinearLayout) {
        val headers = listOf("f'c (kg/cm²)", "Proporción c:a:p", "Cem. (bls)", "Arena (m³)", "Piedra (m³)", "Agua (m³)")
        addHeader(parent, headers)
        CapecoDatos.concretoNormal.forEachIndexed { i, c ->
            val cols = listOf("${c.fc}", c.prop, "${c.cemento}", "${c.arena}", "${c.piedra}", "${c.agua}")
            addRow(parent, cols, i % 2 == 0)
        }
    }

    private fun buildTablaCiclopeo(parent: LinearLayout) {
        val headers = listOf("Proporción", "Cem. (bls)", "Hormigón (m³)", "Piedra Gde (m³)", "Agua (m³)")
        addHeader(parent, headers)
        (CapecoDatos.concretoCiclopeo + CapecoDatos.ciclopeoSobrecimiento)
            .distinctBy { it.proporcion }
            .forEachIndexed { i, c ->
                addRow(parent, listOf(c.proporcion, "${c.cemento}", "${c.hormigon}", "${c.piedraGrande}", "${c.agua}"), i % 2 == 0)
            }
    }

    private fun buildTablaFalsoPiso(parent: LinearLayout) {
        val headers = listOf("Proporción c:h", "Cem. (bls)", "Hormigón (m³)", "Agua (m³)")
        addHeader(parent, headers)
        CapecoDatos.concretoFalsoPiso.forEachIndexed { i, c ->
            addRow(parent, listOf(c.proporcion, "${c.cemento}", "${c.hormigon}", "${c.agua}"), i % 2 == 0)
        }
    }

    private fun buildTablaMuros(parent: LinearLayout) {
        val headers = listOf("Tipo", "Aparejo", "Ladrillos/m²", "Cem. (bls/m²)", "Arena (m³/m²)")
        addHeader(parent, headers)
        CapecoDatos.murosLadrillos.forEachIndexed { i, m ->
            val tipoCorto = m.tipo.substringBefore("(").trim()
            addRow(parent, listOf(tipoCorto, m.aparejo, "${m.ladrillos}", "${m.cemento}", "${m.arena}"), i % 2 == 0)
        }
    }

    private fun buildTablaLosas(parent: LinearLayout) {
        val headers = listOf("Altura (cm)", "Cem. (bls)", "Arena (m³)", "Piedra (m³)", "Ladr./m²", "Acero (kg/m²)")
        addHeader(parent, headers)
        CapecoDatos.losasAligeradas.forEachIndexed { i, l ->
            addRow(parent, listOf("${l.altura}", "${l.cemento}", "${l.arena}", "${l.piedra}", "${l.ladrilloHueco}", "${l.aceroKg}"), i % 2 == 0)
        }
    }

    private fun buildTablaAcero(parent: LinearLayout, repo: PreciosRepository) {
        val precios = repo.getPrecios()
        val headers = listOf("Diámetro", "Designación", "kg/ml", "S/./kg")
        addHeader(parent, headers, accentColor = Color.parseColor("#B71C1C"))
        CapecoDatos.barrasAcero.forEachIndexed { i, b ->
            val precio = repo.getPrecioAcero(b.diam, precios)
            addRow(parent, listOf(b.diam, b.designation, "${b.pesoKgM}", "%.2f".format(precio)), i % 2 == 0)
        }
    }

    // ── Helpers de tabla ──────────────────────────────────────────────────

    private fun addHeader(parent: LinearLayout, cols: List<String>, accentColor: Int? = null) {
        val color = accentColor ?: ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary)
        val row = row()
        row.setBackgroundColor(color)
        cols.forEach { text ->
            row.addView(cell(text, bold = true, textColor = Color.WHITE, weight = 1f))
        }
        parent.addView(row)
    }

    private fun addRow(parent: LinearLayout, cols: List<String>, even: Boolean) {
        val row = row()
        row.setBackgroundColor(if (even) Color.parseColor("#F5F5F5") else Color.WHITE)
        cols.forEachIndexed { i, text ->
            row.addView(cell(text, bold = i == 0, weight = 1f))
        }
        parent.addView(row)
    }

    private fun row() = LinearLayout(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dpToPx(6), 0, dpToPx(6))
    }

    private fun cell(text: String, bold: Boolean = false, textColor: Int = Color.BLACK, weight: Float = 1f) =
        TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            this.text = text
            this.gravity = Gravity.CENTER
            this.setTextColor(textColor)
            textSize = 12f
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
        }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
