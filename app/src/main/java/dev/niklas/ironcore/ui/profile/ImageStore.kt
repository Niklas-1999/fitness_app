package dev.niklas.ironcore.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Avatar / banner images: center-cropped, downscaled, stored privately in the app (never shared). */
object ImageStore {
    suspend fun save(ctx: Context, uri: Uri, banner: Boolean): String = withContext(Dispatchers.IO) {
        val (tw, th) = if (banner) 1200 to 400 else 512 to 512
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= tw && bounds.outHeight / (sample * 2) >= th) sample *= 2
        val raw = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }!!
        val rot = ctx.contentResolver.openInputStream(uri)!!.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f
            }
        }
        val src = if (rot != 0f) Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rot) }, true) else raw
        // center crop to target aspect
        val aspect = tw / th.toFloat()
        val (cw, ch) = if (src.width / src.height.toFloat() > aspect) ((src.height * aspect).toInt() to src.height) else (src.width to (src.width / aspect).toInt())
        val cropped = Bitmap.createBitmap(src, (src.width - cw) / 2, (src.height - ch) / 2, cw, ch)
        val out = Bitmap.createScaledBitmap(cropped, tw, th, true)
        val dir = File(ctx.filesDir, "profile").apply { mkdirs() }
        val prefix = if (banner) "banner_" else "avatar_"
        dir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
        val f = File(dir, "$prefix${System.currentTimeMillis()}.jpg")
        f.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        f.absolutePath
    }
}
