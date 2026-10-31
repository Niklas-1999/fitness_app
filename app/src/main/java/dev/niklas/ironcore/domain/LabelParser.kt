package dev.niklas.ironcore.domain

import kotlin.math.abs

/**
 * Parses OCR text of an EU nutrition declaration (Reg. 1169/2011 Annex XV, RESEARCH §12):
 * energy (kJ/kcal), fat, of which saturates, carbohydrate, of which sugars, protein, salt (+ fibre),
 * per 100 g. Labels often have a second "per portion" column: the FIRST value on a row is used.
 * Everything is a suggestion; the user confirms or edits in the food editor.
 */
object LabelParser {
    data class Values(
        val kcal: Float? = null,
        val fat: Float? = null,
        val satFat: Float? = null,
        val carbs: Float? = null,
        val sugar: Float? = null,
        val protein: Float? = null,
        val salt: Float? = null,
        val fibre: Float? = null,
        /** true when energy roughly matches 9·fat + 4·carbs + 4·protein + 2·fibre */
        val consistent: Boolean = false
    ) {
        val found get() = listOf(kcal, fat, satFat, carbs, sugar, protein, salt, fibre).count { it != null }
    }

    private class Field(val keys: List<String>, val exclude: List<String> = emptyList(), val max: Float = 100f)

    private val FAT = Field(listOf("fett", "fat", "lipid", "grassi", "grasas", "matieres grasses"),
        exclude = listOf("saur", "satur", "gesatt", "acidi", "acides", "acidos", "davon", "of which", "dont"))
    private val SAT = Field(listOf("gesatt", "saturat", "satur", "fettsaur"))
    private val CARBS = Field(listOf("kohlenhydrat", "kohlehydrat", "carbohydrat", "glucid", "carboidrat", "hidratos"),
        exclude = listOf("zucker", "sugar"))
    private val SUGAR = Field(listOf("zucker", "sugar", "sucre", "zuccheri", "azucar"))
    private val PROTEIN = Field(listOf("eiweiss", "eiweis", "protein", "proteine", "proteines"))
    private val SALT = Field(listOf("salz", "salt", "sale", "sal"), max = 50f)
    private val FIBRE = Field(listOf("ballaststoff", "ballast", "fibre", "fiber", "fibres", "fibra"))

    /** lowercase, fold umlauts/accents, fix OCR confusions inside numbers. */
    fun normalize(raw: String): String {
        var s = raw.lowercase()
            .replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")
            .replace("é", "e").replace("è", "e").replace("à", "a").replace("ú", "u").replace("ó", "o").replace("í", "i")
        // O/o read instead of 0 next to digits or decimal separators: "O,5" "1O" "0,O"
        s = s.replace(Regex("(?<=[\\d,.])[o](?=[\\d\\s,.g%]|$)"), "0")
            .replace(Regex("(?<![a-z])[o](?=[,.]\\d)"), "0")
            // l / I read instead of 1 between digits
            .replace(Regex("(?<=\\d)[li](?=\\d)"), "1")
        return s
    }

    private val VALUE_TOKEN = Regex("<?\\d+(?:[.,]\\d+)?(?:\\s*(?:g|mg|%))?")

    /**
     * OCR often reads the unit "g" as "9" ("6,9 g" → "6,99", "13 g" → "139") or "q".
     * If a nutrient row has no explicit g unit and every value ends in 9, that 9 was the unit.
     */
    fun repairUnits(line: String): String {
        var l = line.replace(Regex("(\\d)\\s*q(?![a-z])"), "$1 g")
        if (l.contains("kj") || l.contains("kcal")) return l
        val label = l.indexOfFirst { it.isDigit() || it == '<' }.let { if (it < 0) return l else it }
        val tail = l.substring(label)
        val tokens = VALUE_TOKEN.findAll(tail).map { it.value }.toList()
        if (tokens.isEmpty() || tokens.any { it.endsWith("g") || it.endsWith("%") }) return l
        if (tokens.all { it.trimEnd().endsWith("9") && it.trim().trimStart('<').length >= 2 }) {
            l = l.substring(0, label) + tail.replace(Regex("(<?\\d+(?:[.,]\\d+)?)9(?![\\d])")) { it.groupValues[1] + " g" }
        }
        return l
    }

    private val NUM = Regex("(<\\s*)?(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*(kj|kcal|mg|g)?(\\s*%)?")

    /** First plausible number on a line (skips % RI values and kJ/kcal for nutrient rows). */
    private fun firstValue(line: String, max: Float): Float? {
        for (m in NUM.findAll(line)) {
            if (m.groupValues[4].isNotBlank()) continue                 // % reference intake column
            val unit = m.groupValues[3]
            if (unit == "kj" || unit == "kcal") continue
            var v = m.groupValues[2].replace(',', '.').toFloatOrNull() ?: continue
            if (unit == "mg") v /= 1000f
            if (v <= max) return v
        }
        return null
    }

    private fun matches(line: String, f: Field): Boolean {
        val words = line
        if (f.exclude.any { words.contains(it) }) return false
        return f.keys.any { k ->
            if (k.length <= 3) Regex("(^|[^a-z])$k([^a-z]|$)").containsMatchIn(words) else words.contains(k)
        }
    }

    private fun find(lines: List<String>, f: Field): Float? {
        for ((i, l) in lines.withIndex()) {
            if (!matches(l, f)) continue
            firstValue(l, f.max)?.let { return it }
            // value printed on the next line (table read row-wise but wrapped)
            lines.getOrNull(i + 1)?.let { nx -> if (nx.trimStart().firstOrNull()?.isDigit() == true || nx.trimStart().startsWith("<")) firstValue(nx, f.max)?.let { return it } }
        }
        return null
    }

    fun parse(raw: String): Values {
        val text = normalize(raw)
        val lines = text.lines().map { repairUnits(it.trim()) }.filter { it.isNotEmpty() }
        val kcal = Regex("(\\d{1,4}(?:[.,]\\d)?)\\s*kcal").find(text)?.groupValues?.get(1)?.replace(',', '.')?.toFloatOrNull()
            ?: Regex("(\\d{2,4}(?:[.,]\\d)?)\\s*kj").find(text)?.groupValues?.get(1)?.replace(',', '.')?.toFloatOrNull()?.let { it / 4.184f }
        val fat = find(lines, FAT)
        val sat = find(lines, SAT)?.let { if (fat != null && it > fat) null else it }
        val carbs = find(lines, CARBS)
        val sugar = find(lines, SUGAR)?.let { if (carbs != null && it > carbs) null else it }
        val protein = find(lines, PROTEIN)
        val salt = find(lines, SALT)
        val fibre = find(lines, FIBRE)
        val consistent = kcal != null && fat != null && carbs != null && protein != null &&
            abs(kcal - (9 * fat + 4 * carbs + 4 * protein + 2 * (fibre ?: 0f))) <= maxOf(25f, kcal * .15f)
        return Values(kcal?.let { (it * 10).toInt() / 10f }, fat, sat, carbs, sugar, protein, salt, fibre, consistent)
    }
}
