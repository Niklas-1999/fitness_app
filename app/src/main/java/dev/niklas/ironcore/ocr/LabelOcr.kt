package dev.niklas.ironcore.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import dev.niklas.ironcore.domain.LabelParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Offline OCR of nutrition labels with Tesseract (deu+eng, tessdata_fast). Nothing leaves the device. */
object LabelOcr {
    data class Result(val values: LabelParser.Values, val text: String, val millis: Long)

    private const val MAX_SIDE = 2200

    /** Copies the bundled traineddata to internal storage once (Tesseract needs real files). */
    private fun ensureData(ctx: Context): File {
        val dir = File(ctx.filesDir, "tessdata").apply { mkdirs() }
        for (lang in listOf("deu", "eng")) {
            val out = File(dir, "$lang.traineddata")
            val size = ctx.assets.openFd("tessdata/$lang.traineddata").use { it.length }
            if (!out.exists() || out.length() != size) {
                ctx.assets.open("tessdata/$lang.traineddata").use { input -> out.outputStream().use { input.copyTo(it) } }
            }
        }
        return ctx.filesDir
    }

    /** Decode downsampled, apply EXIF rotation, convert to grayscale with a contrast boost. */
    private fun load(ctx: Context, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val raw = ctx.contentResolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })!!
        }
        val rot = ctx.contentResolver.openInputStream(uri)!!.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
        val scale = minOf(1f, MAX_SIDE / maxOf(raw.width, raw.height).toFloat())
        val m = Matrix().apply { postRotate(rot); postScale(scale, scale) }
        val oriented = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
        val gray = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
        val contrast = 1.4f
        val cm = ColorMatrix().apply { setSaturation(0f) }
        cm.postConcat(ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, -128f * (contrast - 1),
            0f, contrast, 0f, 0f, -128f * (contrast - 1),
            0f, 0f, contrast, 0f, -128f * (contrast - 1),
            0f, 0f, 0f, 1f, 0f
        )))
        Canvas(gray).drawBitmap(oriented, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        if (oriented !== raw) raw.recycle()
        oriented.recycle()
        return gray
    }

    suspend fun scan(ctx: Context, uri: Uri): Result = withContext(Dispatchers.Default) {
        val t0 = System.currentTimeMillis()
        val dataPath = ensureData(ctx).absolutePath
        val bmp = load(ctx, uri)
        val api = TessBaseAPI()
        try {
            check(api.init(dataPath, "deu+eng")) { "OCR engine failed to start" }
            api.setVariable("preserve_interword_spaces", "1")
            // try "uniform block" (keeps table rows together) and "automatic" layout; keep the better parse
            val results = listOf(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK, TessBaseAPI.PageSegMode.PSM_AUTO).map { psm ->
                api.pageSegMode = psm
                api.setImage(bmp)
                val text = api.utF8Text ?: ""
                text to LabelParser.parse(text)
            }
            val (text, values) = results.maxWith(compareBy({ it.second.found }, { it.second.consistent }))
            Result(values, text, System.currentTimeMillis() - t0)
        } finally {
            api.recycle()
            bmp.recycle()
        }
    }

    /** A fresh file URI in the cache for the camera app to write into. */
    fun newPhotoUri(ctx: Context): Uri {
        val dir = File(ctx.cacheDir, "scans").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }   // keep only the latest scan
        val f = File(dir, "label_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
    }
}
