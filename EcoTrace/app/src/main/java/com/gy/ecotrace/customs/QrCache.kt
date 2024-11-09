package com.gy.ecotrace.customs

import android.content.Context
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


object QrCache {
    private const val SIZE = 512

    fun fileFor(context: Context, content: String): File {
        val dir = File(context.filesDir, "qr").apply { mkdirs() }
        return File(dir, "${content.hashCode()}.png")
    }

    suspend fun ensure(context: Context, content: String, fgColor: Int, bgColor: Int): File =
        withContext(Dispatchers.Default) {
            val file = fileFor(context, content)
            if (!file.exists()) {
                runCatching {
                    val bmp = generate(content, fgColor, bgColor)
                    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bmp.recycle()
                }
            }
            file
        }

    private fun generate(content: String, fg: Int, bg: Int): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, SIZE, SIZE)
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.RGB_565)
        for (x in 0 until SIZE) {
            for (y in 0 until SIZE) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) fg else bg)
            }
        }
        return bmp
    }
}
