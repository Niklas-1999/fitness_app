package dev.niklas.ironcore.ui.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.data.FoodEntity
import dev.niklas.ironcore.ocr.LabelOcr
import dev.niklas.ironcore.ui.theme.CyberPalette
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import dev.niklas.ironcore.ui.components.BtnStyle
import dev.niklas.ironcore.ui.components.ChipRow
import dev.niklas.ironcore.ui.components.CyberButton
import dev.niklas.ironcore.ui.components.CyberPanel
import dev.niklas.ironcore.ui.components.FieldLabel
import dev.niklas.ironcore.ui.components.NumberField
import dev.niklas.ironcore.ui.components.OptionCard
import dev.niklas.ironcore.ui.components.PanelHeader
import dev.niklas.ironcore.ui.components.TermInput
import dev.niklas.ironcore.ui.components.Txt
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.T
import kotlin.math.roundToInt

private enum class Mode { PICK, SCANNING, EDIT, PORTION }

/**
 * Log food: pick a saved food (most recent first) → portion in grams → log.
 * NEW FOOD: enter the EU nutrition declaration per 100 g (same order as on the pack).
 * P4 adds a camera scan that pre-fills the editor.
 */
@Composable
fun FoodSheet(foods: List<FoodEntity>, onLog: (FoodEntity, Float, Boolean) -> Unit, onDeleteFood: (Long) -> Unit, onClose: () -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    var mode by remember { mutableStateOf(Mode.PICK) }
    var food by remember { mutableStateOf<FoodEntity?>(null) }
    var save by remember { mutableStateOf(true) }
    var scan by remember { mutableStateOf<LabelOcr.Result?>(null) }
    var scanUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pending by remember { mutableStateOf<android.net.Uri?>(null) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && pending != null) { scanUri = pending; mode = Mode.SCANNING }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { scanUri = uri; mode = Mode.SCANNING }
    }
    androidx.activity.compose.BackHandler { if (mode == Mode.PICK) onClose() else { fx.glitch(); mode = Mode.PICK } }
    Box(
        Modifier.fillMaxSize().background(p.k01.copy(alpha = .985f))
            .clickable(remember { MutableInteractionSource() }, null) {}
            .windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("// FUEL INTAKE", T.mono(10.sp, p.textDim, .25.em), Modifier.weight(1f))
                CyberButton("✕ CLOSE", style = BtnStyle.OUTLINE) { onClose() }
            }
            Spacer(Modifier.height(10.dp))
            when (mode) {
                Mode.PICK -> PickFood(foods, onNew = { food = null; scan = null; mode = Mode.EDIT }, onPick = { food = it; save = true; mode = Mode.PORTION },
                    onEdit = { food = it; scan = null; mode = Mode.EDIT }, onDelete = onDeleteFood,
                    onCamera = { pending = LabelOcr.newPhotoUri(ctx); camera.launch(pending!!) },
                    onGallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                Mode.SCANNING -> ScanProgress(scanUri!!, onDone = { r -> scan = r; food = null; mode = Mode.EDIT }, onFail = { mode = Mode.PICK })
                Mode.EDIT -> FoodEditor(food, scan) { f -> food = f; mode = Mode.PORTION }
                Mode.PORTION -> Portion(food!!, save, { save = it }) { g -> onLog(food!!, g, save) }
            }
        }
    }
}

@Composable
private fun PickFood(
    foods: List<FoodEntity>, onNew: () -> Unit, onPick: (FoodEntity) -> Unit, onEdit: (FoodEntity) -> Unit, onDelete: (Long) -> Unit,
    onCamera: () -> Unit, onGallery: () -> Unit
) {
    val p = Cyber.c
    var q by remember { mutableStateOf("") }
    CyberPanel(Modifier.fillMaxWidth(), padding = 14.dp) {
        PanelHeader("SC", "LABEL_SCANNER", "OFFLINE OCR")
        Txt("Photograph the nutrition table (per 100 g column). Flat, well lit, fill the frame.", T.body(14.sp, p.textDim))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberButton("SCAN // CAMERA", Modifier.weight(1.4f)) { onCamera() }
            CyberButton("GALLERY", Modifier.weight(1f), BtnStyle.OUTLINE) { onGallery() }
        }
    }
    Spacer(Modifier.height(12.dp))
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("DB", "MY_FOODS", "${foods.size} SAVED")
        CyberButton("+ ENTER LABEL MANUALLY", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) { onNew() }
        Spacer(Modifier.height(10.dp))
        if (foods.isNotEmpty()) TermInput(q, { q = it }, placeholder = "SEARCH_", maxLen = 30)
        Spacer(Modifier.height(8.dp))
        val list = foods.filter { q.isBlank() || it.name.contains(q.trim(), ignoreCase = true) }
        if (foods.isEmpty()) Txt("> NO SAVED FOODS YET. ADD ONE FROM ITS NUTRITION LABEL; IT IS SAVED FOR ONE-TAP LOGGING.", T.mono(11.sp, p.textMute, 0.sp).copy(lineHeight = 1.5.em))
        list.forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                OptionCard(f.name, "${f.kcal.roundToInt()} kcal · P ${fmt(f.protein)} · C ${fmt(f.carbs)} · F ${fmt(f.fat)} /100 g", false, Modifier.weight(1f)) { onPick(f) }
                Column(Modifier.padding(start = 6.dp)) {
                    Txt("EDIT", T.mono(10.sp, p.textDim), Modifier.clickable { onEdit(f) }.padding(6.dp))
                    Txt("DEL", T.mono(10.sp, p.textMute), Modifier.clickable { onDelete(f.id) }.padding(6.dp))
                }
            }
        }
    }
}

private fun fmt(v: Float) = if (v >= 10) v.roundToInt().toString() else "%.1f".format(v)

/** EU nutrition declaration editor, per 100 g / 100 ml (Reg. 1169/2011 Annex XV order). */
@Composable
fun FoodEditor(initial: FoodEntity?, scan: LabelOcr.Result? = null, onDone: (FoodEntity) -> Unit) {
    val p = Cyber.c
    val v = scan?.values
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var kcal by remember { mutableStateOf(initial?.kcal ?: v?.kcal) }
    var kj by remember { mutableStateOf<Float?>(null) }
    var fat by remember { mutableStateOf(initial?.fat ?: v?.fat) }
    var sat by remember { mutableStateOf(initial?.satFat ?: v?.satFat) }
    var carbs by remember { mutableStateOf(initial?.carbs ?: v?.carbs) }
    var sugar by remember { mutableStateOf(initial?.sugar ?: v?.sugar) }
    var protein by remember { mutableStateOf(initial?.protein ?: v?.protein) }
    var salt by remember { mutableStateOf(initial?.salt ?: v?.salt) }
    var fibre by remember { mutableStateOf(initial?.fibre ?: v?.fibre) }
    var showRaw by remember { mutableStateOf(false) }
    if (scan != null) {
        CyberPanel(Modifier.fillMaxWidth()) {
            PanelHeader("OCR", "SCAN_RESULT", "${scan.millis / 100 / 10f} S")
            Txt("${scan.values.found}/8 FIELDS DECODED", T.head(16.sp, if (scan.values.found >= 6) CyberPalette.UP else CyberPalette.AMBER))
            Txt(
                if (scan.values.consistent) "> ENERGY MATCHES THE MACROS. LOOKS RIGHT. STILL CHECK EVERY VALUE."
                else "> CHECK ALL VALUES AGAINST THE PACK. MISSING OR SUSPICIOUS FIELDS ARE EMPTY.",
                T.mono(10.sp, if (scan.values.consistent) CyberPalette.UP else CyberPalette.AMBER, 0.sp).copy(lineHeight = 1.5.em),
                Modifier.padding(top = 4.dp)
            )
            Txt(if (showRaw) "▲ HIDE RAW OCR" else "▼ SHOW RAW OCR", T.mono(10.sp, p.textDim), Modifier.clickable { showRaw = !showRaw }.padding(vertical = 6.dp))
            if (showRaw) Txt(scan.text.ifBlank { "(no text recognized)" }, T.mono(10.sp, p.textMute, 0.sp))
        }
        Spacer(Modifier.height(12.dp))
    }
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("LBL", "NUTRITION_LABEL", "PER 100 G / ML")
        Txt(if (scan != null) "Give it a name, check the values, fill gaps." else "Copy the values from the nutrition table on the pack (per 100 g column).", T.body(14.sp, p.textDim))
        FieldLabel("NAME", Modifier.padding(top = 8.dp))
        TermInput(name, { name = it }, placeholder = "E.G. OATS_", maxLen = 32)
        FieldLabel("ENERGY (KCAL) // OR ENTER KJ BELOW")
        NumberField(kcal, { kcal = it }, "KCAL", 1f, 0f, 950f, decimals = 1)
        NumberField(kj, { kj = it; it?.let { v -> kcal = (v / 4.184f * 10).roundToInt() / 10f } }, "KJ", 1f, 0f, 4000f, decimals = 0, modifier = Modifier.padding(top = 6.dp))
        FieldLabel("FAT"); NumberField(fat, { fat = it }, "G", .5f, 0f, 100f, decimals = 1)
        FieldLabel("OF WHICH SATURATES"); NumberField(sat, { sat = it }, "G", .5f, 0f, 100f, decimals = 1)
        FieldLabel("CARBOHYDRATE"); NumberField(carbs, { carbs = it }, "G", .5f, 0f, 100f, decimals = 1)
        FieldLabel("OF WHICH SUGARS"); NumberField(sugar, { sugar = it }, "G", .5f, 0f, 100f, decimals = 1)
        FieldLabel("PROTEIN"); NumberField(protein, { protein = it }, "G", .5f, 0f, 100f, decimals = 1)
        FieldLabel("SALT"); NumberField(salt, { salt = it }, "G", .1f, 0f, 100f, decimals = 2)
        FieldLabel("FIBRE // OPTIONAL"); NumberField(fibre, { fibre = it }, "G", .5f, 0f, 100f, decimals = 1)
        val valid = name.isNotBlank() && kcal != null && fat != null && carbs != null && protein != null
        Spacer(Modifier.height(12.dp))
        CyberButton("CONTINUE >", Modifier.fillMaxWidth(), enabled = valid) {
            onDone((initial ?: FoodEntity(name = "", kcal = 0f, fat = 0f, satFat = 0f, carbs = 0f, sugar = 0f, protein = 0f, salt = 0f, fibre = null)).copy(
                name = name.trim(), kcal = kcal!!, fat = fat!!, satFat = sat ?: 0f, carbs = carbs!!, sugar = sugar ?: 0f,
                protein = protein!!, salt = salt ?: 0f, fibre = fibre
            ))
        }
        Txt("Required: name, energy, fat, carbohydrate, protein.", T.mono(9.sp, p.textMute), Modifier.padding(top = 6.dp))
    }
}

/** Runs offline OCR on the photo with a terminal-style progress log. */
@Composable
private fun ScanProgress(uri: android.net.Uri, onDone: (LabelOcr.Result) -> Unit, onFail: () -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val lines = listOf("LOADING OPTICAL MATRIX", "NORMALIZING IMAGE", "RUNNING NEURAL OCR // DEU+ENG", "PARSING EU NUTRITION DECLARATION", "CROSS-CHECKING ENERGY")
    var shown by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(uri) {
        val job = kotlinx.coroutines.coroutineScope {
            val anim = launch { while (shown < lines.size - 1) { kotlinx.coroutines.delay(450); shown++; fx.boot() } }
            val r = runCatching { LabelOcr.scan(ctx, uri) }
            anim.cancel()
            r
        }
        job.onSuccess { shown = lines.size; fx.complete(); kotlinx.coroutines.delay(300); onDone(it) }
            .onFailure { error = it.message ?: "SCAN FAILED"; fx.error() }
    }
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("OCR", "DECODING_LABEL", if (error == null) "RUNNING" else "ERROR", live = error == null)
        lines.take(shown + 1).forEachIndexed { i, l ->
            Txt("> $l " + if (i < shown) "... OK" else "_", T.mono(12.sp, p.accent, 0.sp, glow = p.acc(.5f)), Modifier.padding(vertical = 2.dp))
        }
        Spacer(Modifier.height(10.dp))
        dev.niklas.ironcore.ui.components.NeonBar((shown + 1) / lines.size.toFloat(), height = 5.dp, durationMs = 400)
        error?.let {
            Txt("> $it", T.mono(12.sp, dev.niklas.ironcore.ui.theme.CyberPalette.DOWN), Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(10.dp))
            CyberButton("< BACK", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) { onFail() }
        }
    }
}

@Composable
private fun Portion(food: FoodEntity, save: Boolean, onSave: (Boolean) -> Unit, onLog: (Float) -> Unit) {
    val p = Cyber.c
    var grams by remember { mutableStateOf<Float?>(food.portionG) }
    val g = grams ?: 0f
    val k = g / 100f
    CyberPanel(Modifier.fillMaxWidth()) {
        PanelHeader("POR", "PORTION", food.name.take(18))
        Txt(food.name, T.head(16.sp, Color.White, glow = 10f))
        FieldLabel("AMOUNT EATEN", Modifier.padding(top = 10.dp))
        NumberField(grams, { grams = it }, "G", 10f, 0f, 3000f)
        Spacer(Modifier.height(6.dp))
        val quick = listOf(50f, 100f, 150f, 250f)
        ChipRow(quick.map { "${it.toInt()}G" }, { quick[it] == grams }, { grams = quick[it] })
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Stat("KCAL", food.kcal * k); Stat("PROTEIN", food.protein * k); Stat("CARBS", food.carbs * k); Stat("FAT", food.fat * k)
        }
        Spacer(Modifier.height(10.dp))
        OptionCard("SAVE TO MY FOODS", "One-tap logging next time, remembers this portion", save) { onSave(!save) }
        Spacer(Modifier.height(10.dp))
        CyberButton("LOG ${g.roundToInt()} G >", Modifier.fillMaxWidth(), enabled = g > 0f) { onLog(g) }
    }
}

@Composable
private fun Stat(label: String, v: Float) {
    val p = Cyber.c
    Column {
        Txt(label, T.mono(9.sp, p.textMute))
        Txt(if (label == "KCAL") v.roundToInt().toString() else "%.1f".format(v), T.mono(15.sp, Color.White))
    }
}
