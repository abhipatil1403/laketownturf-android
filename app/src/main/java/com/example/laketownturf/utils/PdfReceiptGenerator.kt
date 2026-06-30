package com.example.laketownturf.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.laketownturf.data.model.Booking
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PdfReceiptGenerator {
    fun generateAndGetUri(context: Context, booking: Booking): android.net.Uri? {
        val document = PdfDocument()
        // A4 Size roughly 595 x 842 at 72 PPI
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        var currentY = 80f
        val leftMargin = 50f
        val rightMargin = 545f
        
        // 1. Header (Centered)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("LAKE TOWN TURF", pageInfo.pageWidth / 2f, currentY, paint)
        currentY += 25f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        canvas.drawText("Block A, Lake Town Society, Pune", pageInfo.pageWidth / 2f, currentY, paint)
        currentY += 15f
        canvas.drawText("GSTIN: 27AABCU9603R1ZM", pageInfo.pageWidth / 2f, currentY, paint)
        currentY += 40f

        // 2. Receipt Title
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("DIGITAL RECEIPT", pageInfo.pageWidth / 2f, currentY, paint)
        currentY += 50f

        // 3. Booking Details (Left Aligned)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val parsedDate = try { LocalDate.parse(booking.date).format(formatter) } catch (e: Exception) { booking.date }

        canvas.drawText("Booking ID: ${booking.bookingId}", leftMargin, currentY, paint)
        currentY += 20f
        canvas.drawText("Slot Date: $parsedDate", leftMargin, currentY, paint)
        currentY += 20f
        canvas.drawText("Time: ${TimeUtils.formatTime12hr(booking.startTime)} - ${TimeUtils.formatTime12hr(booking.endTime)}", leftMargin, currentY, paint)
        currentY += 30f

        // 4. Line Separator
        paint.color = Color.DKGRAY
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
        currentY += 30f

        // 5. Roster Section
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("Resident Players", leftMargin, currentY, paint)
        currentY += 20f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        if (booking.players.isEmpty()) {
            canvas.drawText("No resident players.", leftMargin, currentY, paint)
            currentY += 20f
        } else {
            booking.players.forEach { p ->
                val flatDisplay = if (p.blockNo.isNotBlank()) "${p.blockNo}-${p.flatNo}" else "Flat ${p.flatNo}"
                canvas.drawText("- ${p.name} ($flatDisplay)", leftMargin, currentY, paint)
                currentY += 20f
            }
        }
        currentY += 10f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("Guest Passes", leftMargin, currentY, paint)
        currentY += 20f
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        if (booking.guests.isEmpty()) {
            canvas.drawText("No guests.", leftMargin, currentY, paint)
            currentY += 20f
        } else {
            booking.guests.forEach { g ->
                canvas.drawText("- ${g.name} (₹100)", leftMargin, currentY, paint)
                currentY += 20f
            }
        }
        currentY += 20f

        // 6. Line Separator
        paint.color = Color.DKGRAY
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
        currentY += 30f

        // 7. Payment Summary
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val guestTotal = booking.guests.size * 100
        val basePrice = booking.amount.toInt() - guestTotal

        canvas.drawText("Base Slot Price:", leftMargin, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹$basePrice", rightMargin, currentY, paint)
        
        if (guestTotal > 0) {
            currentY += 25f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Guest Passes (${booking.guests.size} x ₹100):", leftMargin, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("₹$guestTotal", rightMargin, currentY, paint)
        }
        currentY += 30f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("TOTAL PAID:", leftMargin, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${booking.amount.toInt()}", rightMargin, currentY, paint)

        currentY += 60f

        // Footer
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.GRAY
        canvas.drawText("Thank you for booking with Lake Town Turf!", pageInfo.pageWidth / 2f, currentY, paint)

        // Finish page
        document.finishPage(page)

        // Write to file
        return try {
            val receiptsDir = File(context.cacheDir, "receipts")
            if (!receiptsDir.exists()) receiptsDir.mkdirs()
            
            val file = File(receiptsDir, "Receipt_${booking.bookingId}.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }
}
