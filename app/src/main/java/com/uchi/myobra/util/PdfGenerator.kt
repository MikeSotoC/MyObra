package com.uchi.myobra.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.uchi.myobra.data.ResultadoCalculo
import com.uchi.myobra.data.db.Proyecto
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Genera un presupuesto general en PDF usando PdfDocument (API nativa Android).
 * No requiere ninguna librería externa.
 */
object PdfGenerator {

    private const val PAGE_W     = 595   // A4 en puntos (72 dpi)
    private const val PAGE_H     = 842
    private const val MARGIN     = 40f
    private const val LINE_H     = 22f

    private val fmt = NumberFormat.getNumberInstance(Locale("es", "PE")).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }

    fun Double.fmtS() = "S/. ${fmt.format(this)}"

    // ── Estilos ──────────────────────────────────────────────────────────

    private val pBrandColor  = Paint().apply { color = Color.parseColor("#BF4800") }
    private val pWhite       = Paint().apply { color = Color.WHITE }
    private val pLight       = Paint().apply { color = Color.parseColor("#FFF3EE") }
    private val pDivider     = Paint().apply { color = Color.parseColor("#E0D0C8") }
    private val pRowAlt      = Paint().apply { color = Color.parseColor("#FAF5F3") }

    private fun textPaint(
        size: Float, bold: Boolean = false, color: Int = Color.parseColor("#1C1411")
    ) = Paint().apply {
        textSize   = size
        isFakeBoldText = bold
        this.color = color
        isAntiAlias = true
    }

    // ── Main entry point ─────────────────────────────────────────────────

    /**
     * Genera el PDF y devuelve el File resultante.
     * @param secciones lista de (nombrePartida, resultado) para incluir
     */
    fun generar(
        context: Context,
        proyecto: Proyecto,
        secciones: List<Pair<String, ResultadoCalculo>>
    ): File {
        val doc  = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val c    = page.canvas

        var y = drawHeader(c, proyecto)
        y = drawProjectInfo(c, proyecto, y)
        y = drawTableHeader(c, y)
        y = drawSectionRows(c, secciones, y)
        y = drawTotals(c, secciones, y)
        drawFooter(c)

        doc.finishPage(page)

        val file = getPdfFile(context, proyecto)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun getUri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    // ── Layout sections ──────────────────────────────────────────────────

    private fun drawHeader(c: Canvas, p: Proyecto): Float {
        // Orange header bar
        c.drawRect(0f, 0f, PAGE_W.toFloat(), 70f, pBrandColor)

        // App name
        c.drawText("MyObra", MARGIN, 35f, textPaint(20f, bold = true, color = Color.WHITE))
        c.drawText("Presupuesto General de Materiales y Obra", MARGIN, 55f,
            textPaint(9f, color = Color.parseColor("#FFD8C0")))

        // Date top right
        val date = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(Date())
        val datePaint = textPaint(9f, color = Color.WHITE)
        val dateW = datePaint.measureText(date)
        c.drawText(date, PAGE_W - MARGIN - dateW, 40f, datePaint)

        return 80f
    }

    private fun drawProjectInfo(c: Canvas, p: Proyecto, startY: Float): Float {
        var y = startY
        c.drawRect(MARGIN - 4f, y - 4f, PAGE_W - MARGIN + 4f, y + 68f, pLight)

        c.drawText(p.nombre, MARGIN, y + 18f, textPaint(14f, bold = true))
        if (p.propietario.isNotBlank())
            c.drawText("Propietario: ${p.propietario}", MARGIN, y + 35f, textPaint(9f))
        if (p.ubicacion.isNotBlank())
            c.drawText("Ubicación: ${p.ubicacion}",    MARGIN, y + 50f, textPaint(9f))

        return y + 80f
    }

    private fun drawTableHeader(c: Canvas, startY: Float): Float {
        val y = startY
        c.drawRect(MARGIN - 4f, y, PAGE_W - MARGIN + 4f, y + LINE_H + 4f, pBrandColor)

        val headers = listOf("PARTIDA" to 0.44f, "MATERIALES S/." to 0.19f,
            "MANO DE OBRA S/." to 0.19f, "TOTAL S/." to 0.18f)

        val tableW = PAGE_W - MARGIN * 2
        var x = MARGIN
        headers.forEach { (text, w) ->
            val colW = tableW * w
            c.drawText(text, x + 4f, y + LINE_H - 4f, textPaint(8f, bold = true, color = Color.WHITE))
            x += colW
        }
        return y + LINE_H + 6f
    }

    private fun drawSectionRows(c: Canvas, secciones: List<Pair<String, ResultadoCalculo>>, startY: Float): Float {
        var y   = startY
        val tableW = PAGE_W - MARGIN * 2
        val colWidths = listOf(0.44f, 0.19f, 0.19f, 0.18f)

        secciones.forEachIndexed { i, (nombre, r) ->
            val bg = if (i % 2 == 0) pWhite else pRowAlt
            c.drawRect(MARGIN - 4f, y, PAGE_W - MARGIN + 4f, y + LINE_H, bg)

            val values = listOf(nombre, r.totalMateriales.fmtS(), r.costoManoObra.fmtS(), r.totalObra.fmtS())
            var x = MARGIN
            values.forEachIndexed { vi, text ->
                val colW = tableW * colWidths[vi]
                val paint = if (vi == 0) textPaint(9f) else textPaint(9f, bold = vi == 3, color = if (vi == 3) Color.parseColor("#BF4800") else Color.parseColor("#1C1411"))
                c.drawText(text, x + 4f, y + LINE_H - 5f, paint)
                x += colW
            }
            // Divider
            c.drawRect(MARGIN - 4f, y + LINE_H - 1f, PAGE_W - MARGIN + 4f, y + LINE_H, pDivider)
            y += LINE_H
        }
        return y + 10f
    }

    private fun drawTotals(c: Canvas, secciones: List<Pair<String, ResultadoCalculo>>, startY: Float): Float {
        val totMat = secciones.sumOf { it.second.totalMateriales }
        val totMO  = secciones.sumOf { it.second.costoManoObra }
        val totGen = totMat + totMO
        var y = startY

        // Sub-totals row
        c.drawRect(MARGIN - 4f, y, PAGE_W - MARGIN + 4f, y + LINE_H, pLight)
        val tableW = PAGE_W - MARGIN * 2
        c.drawText("SUBTOTALES", MARGIN + 4f, y + LINE_H - 5f, textPaint(9f, bold = true))
        c.drawText(totMat.fmtS(), MARGIN + tableW * 0.44f + 4f, y + LINE_H - 5f, textPaint(9f, bold = true))
        c.drawText(totMO.fmtS(),  MARGIN + tableW * 0.63f + 4f, y + LINE_H - 5f, textPaint(9f, bold = true))
        y += LINE_H + 6f

        // Grand total
        c.drawRect(MARGIN - 4f, y, PAGE_W - MARGIN + 4f, y + 32f, pBrandColor)
        c.drawText("COSTO TOTAL DE OBRA", MARGIN + 4f, y + 22f, textPaint(11f, bold = true, color = Color.WHITE))
        val totalStr = totGen.fmtS()
        val totalW = textPaint(13f, bold = true, color = Color.WHITE).measureText(totalStr)
        c.drawText(totalStr, PAGE_W - MARGIN - totalW - 4f, y + 22f, textPaint(13f, bold = true, color = Color.WHITE))
        y += 42f

        // Per-section acero detail
        val tieneAcero = secciones.any { it.second.filasAcero.isNotEmpty() }
        if (tieneAcero) {
            y += 10f
            c.drawText("DETALLE DE ACERO POR SECCIÓN", MARGIN, y, textPaint(9f, bold = true, color = Color.parseColor("#B71C1C")))
            y += LINE_H

            secciones.filter { it.second.filasAcero.isNotEmpty() }.forEach { (nombre, r) ->
                c.drawText("• $nombre", MARGIN + 4f, y, textPaint(9f))
                y += LINE_H - 4f
                r.filasAcero.forEach { fila ->
                    val det = "  ${fila.diametro.trim()} · ${fila.cantidad} barras × ${fmt.format(fila.longitud)}m = ${fmt.format(fila.totalKg)} kg"
                    c.drawText(det, MARGIN + 12f, y, textPaint(8f, color = Color.parseColor("#B71C1C")))
                    y += LINE_H - 6f
                }
            }
        }

        return y
    }

    private fun drawFooter(c: Canvas) {
        val fPaint = textPaint(7f, color = Color.parseColor("#9E8880"))
        c.drawLine(MARGIN, PAGE_H - 30f, PAGE_W - MARGIN, PAGE_H - 30f, pDivider)
        c.drawText("Generado con MyObra · Proporciones CAPECO Perú · Los valores son referenciales.",
            MARGIN, PAGE_H - 15f, fPaint)
    }

    // ── File helpers ─────────────────────────────────────────────────────

    private fun getPdfFile(context: Context, proyecto: Proyecto): File {
        val dir = File(context.filesDir, "exports").also { it.mkdirs() }
        val safeName = proyecto.nombre.replace(Regex("[^A-Za-z0-9_\\-]"), "_")
        return File(dir, "Presupuesto_${safeName}.pdf")
    }
}
