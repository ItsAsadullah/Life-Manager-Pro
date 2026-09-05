package com.hisabnikash.app.ui.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.hisabnikash.app.data.local.PersonEntity
import com.hisabnikash.app.data.local.PersonTransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExportUtil {

    fun generateSummaryPdf(context: Context, persons: List<PersonEntity>, personTransactions: List<PersonTransactionEntity>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        canvas.drawText("MyHisab All Persons Summary", pageInfo.pageWidth / 2f, 50f, titlePaint)
        
        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
        canvas.drawText("Date: ${dateFormat.format(Date())}", 50f, 100f, subtitlePaint)

        var yPosition = 150f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Name", 50f, yPosition, paint)
        canvas.drawText("Phone", 250f, yPosition, paint)
        canvas.drawText("Net Balance", 400f, yPosition, paint)

        canvas.drawLine(50f, yPosition + 10f, pageInfo.pageWidth - 50f, yPosition + 10f, paint)
        yPosition += 30f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        var totalReceive = 0.0
        var totalGive = 0.0

        for (person in persons) {
            if (yPosition > 800f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            val txs = personTransactions.filter { it.personId == person.id }
            val receive = txs.filter { it.isReceive }.sumOf { it.amount }
            val give = txs.filter { !it.isReceive }.sumOf { it.amount }
            val net = receive - give

            if (net > 0) totalReceive += net else totalGive += Math.abs(net)

            val name = if (person.name.length > 20) person.name.take(17) + "..." else person.name
            canvas.drawText(name, 50f, yPosition, paint)
            canvas.drawText(person.phone, 250f, yPosition, paint)
            
            if (net > 0) {
                canvas.drawText("$net (Pabo)", 400f, yPosition, paint)
            } else if (net < 0) {
                canvas.drawText("${Math.abs(net)} (Dibo)", 400f, yPosition, paint)
            } else {
                canvas.drawText("0.0", 400f, yPosition, paint)
            }

            yPosition += 25f
        }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawLine(50f, yPosition, pageInfo.pageWidth - 50f, yPosition, paint)
        yPosition += 30f

        canvas.drawText("Total Pabo: $totalReceive", 50f, yPosition, paint)
        yPosition += 25f
        canvas.drawText("Total Dibo: $totalGive", 50f, yPosition, paint)

        pdfDocument.finishPage(page)

        try {
            val file = File(context.cacheDir, "SummaryReport_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            sharePdf(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error generating PDF", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun generateAndSharePdf(context: Context, person: PersonEntity, transactions: List<PersonTransactionEntity>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Title
        canvas.drawText("MyHisab Transaction Report", pageInfo.pageWidth / 2f, 50f, titlePaint)

        // Person Details
        canvas.drawText("Name: ${person.name}", 50f, 100f, subtitlePaint)
        if (person.phone.isNotBlank()) {
            canvas.drawText("Phone: ${person.phone}", 50f, 125f, subtitlePaint)
        }
        if (person.address.isNotBlank()) {
            canvas.drawText("Address: ${person.address}", 50f, 150f, subtitlePaint)
        }

        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
        canvas.drawText("Date: ${dateFormat.format(Date())}", pageInfo.pageWidth - 200f, 100f, subtitlePaint)

        // Table Header
        var yPosition = 200f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Date & Time", 50f, yPosition, paint)
        canvas.drawText("Note", 200f, yPosition, paint)
        canvas.drawText("Amount", 400f, yPosition, paint)
        canvas.drawText("Type", 500f, yPosition, paint)

        // Draw Line
        canvas.drawLine(50f, yPosition + 10f, pageInfo.pageWidth - 50f, yPosition + 10f, paint)
        yPosition += 30f

        // Table Content
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var totalReceive = 0.0
        var totalGive = 0.0

        for (transaction in transactions) {
            if (yPosition > 800f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            canvas.drawText("${transaction.date} ${transaction.time}", 50f, yPosition, paint)
            
            // Limit note length to prevent overlap
            val note = if (transaction.note.length > 20) transaction.note.take(17) + "..." else transaction.note
            canvas.drawText(note, 200f, yPosition, paint)
            
            canvas.drawText(transaction.amount.toString(), 400f, yPosition, paint)
            
            if (transaction.isReceive) {
                canvas.drawText("Received", 500f, yPosition, paint)
                totalReceive += transaction.amount
            } else {
                canvas.drawText("Given", 500f, yPosition, paint)
                totalGive += transaction.amount
            }

            yPosition += 25f
        }

        // Draw Line
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawLine(50f, yPosition, pageInfo.pageWidth - 50f, yPosition, paint)
        yPosition += 30f

        // Summary
        canvas.drawText("Total Received: $totalReceive", 50f, yPosition, paint)
        yPosition += 25f
        canvas.drawText("Total Given: $totalGive", 50f, yPosition, paint)
        yPosition += 25f
        val netBalance = totalReceive - totalGive
        val balanceText = if (netBalance >= 0) "Net Balance: $netBalance (To Receive)" else "Net Balance: ${Math.abs(netBalance)} (To Give)"
        canvas.drawText(balanceText, 50f, yPosition, paint)

        pdfDocument.finishPage(page)

        // Save and Share
        try {
            val file = File(context.cacheDir, "Report_${person.name}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            sharePdf(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error generating PDF", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Open PDF with"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "No PDF viewer installed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
