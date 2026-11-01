package dev.niklas.ironcore.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Full backup as one JSON file (HANDOFF: export/import, no cloud). Generic over all tables, so a
 * backup from an older app version imports into a newer schema: unknown columns are skipped,
 * missing ones take their defaults. Profile images are embedded as base64. Settings
 * (theme, sound, haptics, reminders) are included.
 */
object Backup {
    private const val FORMAT = 1
    private val SKIP = setOf("room_master_table", "sqlite_sequence", "android_metadata")
    private val PREFS = listOf("theme", "fx", "reminders")

    class Result(val tables: Int, val rows: Int)

    private fun tables(db: IronDb): List<String> {
        val out = mutableListOf<String>()
        db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            while (c.moveToNext()) c.getString(0).let { if (it !in SKIP) out += it }
        }
        return out
    }

    private fun columns(db: IronDb, table: String): Set<String> {
        val out = mutableSetOf<String>()
        db.openHelper.readableDatabase.query("PRAGMA table_info(`$table`)").use { c -> while (c.moveToNext()) out += c.getString(1) }
        return out
    }

    suspend fun export(ctx: Context, db: IronDb, uri: Uri): Result = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "IRONCORE").put("format", FORMAT).put("dbVersion", db.openHelper.readableDatabase.version)
            .put("exportedAt", System.currentTimeMillis())
        val tablesJson = JSONObject()
        var rows = 0
        val names = tables(db)
        db.runInTransaction(Runnable {
            for (t in names) {
                val arr = JSONArray()
                db.openHelper.readableDatabase.query("SELECT * FROM `$t`").use { c ->
                    while (c.moveToNext()) {
                        val o = JSONObject()
                        for (i in 0 until c.columnCount) {
                            val v: Any = when (c.getType(i)) {
                                Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                                Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                                Cursor.FIELD_TYPE_BLOB -> Base64.encodeToString(c.getBlob(i), Base64.NO_WRAP)
                                else -> c.getString(i)
                            }
                            o.put(c.getColumnName(i), v)
                        }
                        arr.put(o); rows++
                    }
                }
                tablesJson.put(t, arr)
            }
        })
        root.put("tables", tablesJson)
        // settings
        val prefs = JSONObject()
        for (name in PREFS) {
            val sp = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
            val o = JSONObject()
            sp.all.forEach { (k, v) -> o.put(k, v) }
            prefs.put(name, o)
        }
        root.put("prefs", prefs)
        // images
        val images = JSONObject()
        db.openHelper.readableDatabase.query("SELECT avatarPath, bannerPath FROM cosmetics WHERE id = 0").use { c ->
            if (c.moveToFirst()) {
                for ((i, key) in listOf("avatar", "banner").withIndex()) {
                    c.getString(i)?.let { File(it) }?.takeIf { it.exists() }?.let { images.put(key, Base64.encodeToString(it.readBytes(), Base64.NO_WRAP)) }
                }
            }
        }
        root.put("images", images)
        ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(root.toString().toByteArray()) }
        Result(names.size, rows)
    }

    /** Replaces ALL current data with the backup. Throws with a readable message on invalid files. */
    suspend fun import(ctx: Context, db: IronDb, uri: Uri): Result = withContext(Dispatchers.IO) {
        val text = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        val root = try { JSONObject(text) } catch (e: Exception) { throw IllegalArgumentException("NOT A VALID BACKUP FILE") }
        require(root.optString("app") == "IRONCORE") { "NOT AN IRONCORE BACKUP" }
        require(root.optInt("format") <= FORMAT) { "BACKUP FROM A NEWER APP VERSION. UPDATE IRONCORE FIRST." }
        val tablesJson = root.getJSONObject("tables")
        val existing = tables(db)
        var rows = 0
        db.runInTransaction(Runnable {
            val w = db.openHelper.writableDatabase
            for (t in existing) {
                w.execSQL("DELETE FROM `$t`")
                val arr = tablesJson.optJSONArray(t) ?: continue
                val cols = columns(db, t)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val cv = ContentValues()
                    o.keys().forEach { k ->
                        if (k !in cols) return@forEach
                        when (val v = o.get(k)) {
                            JSONObject.NULL -> cv.putNull(k)
                            is Int -> cv.put(k, v.toLong())
                            is Long -> cv.put(k, v)
                            is Double -> cv.put(k, v)
                            is Boolean -> cv.put(k, if (v) 1 else 0)
                            else -> cv.put(k, v.toString())
                        }
                    }
                    w.insert(t, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                    rows++
                }
            }
            // images: write files and point the cosmetics row at them
            val images = root.optJSONObject("images")
            val dir = File(ctx.filesDir, "profile").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
            for (key in listOf("avatar", "banner")) {
                val col = key + "Path"
                val b64 = images?.optString(key).orEmpty()
                val path = if (b64.isNotEmpty()) File(dir, "${key}_${System.currentTimeMillis()}.jpg").also { it.writeBytes(Base64.decode(b64, Base64.NO_WRAP)) }.absolutePath else null
                w.execSQL("UPDATE cosmetics SET `$col` = ? WHERE id = 0", arrayOf(path))
            }
        })
        // settings
        root.optJSONObject("prefs")?.let { prefs ->
            for (name in PREFS) {
                val o = prefs.optJSONObject(name) ?: continue
                val ed = ctx.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
                o.keys().forEach { k ->
                    when (val v = o.get(k)) {
                        is Boolean -> ed.putBoolean(k, v); is Int -> ed.putInt(k, v); is Long -> ed.putLong(k, v)
                        is Double -> ed.putFloat(k, v.toFloat()); else -> ed.putString(k, v.toString())
                    }
                }
                ed.apply()
            }
        }
        Result(existing.size, rows)
    }
}
