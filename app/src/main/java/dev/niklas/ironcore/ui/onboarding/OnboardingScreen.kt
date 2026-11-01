package dev.niklas.ironcore.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.niklas.ironcore.audio.LocalFx
import dev.niklas.ironcore.domain.Activity
import dev.niklas.ironcore.domain.Equip
import dev.niklas.ironcore.domain.Exercises
import dev.niklas.ironcore.domain.Experience
import dev.niklas.ironcore.domain.Format
import dev.niklas.ironcore.domain.Goal
import dev.niklas.ironcore.domain.Intensity
import dev.niklas.ironcore.domain.Nutrition
import dev.niklas.ironcore.domain.Plan
import dev.niklas.ironcore.domain.PlanGenerator
import dev.niklas.ironcore.domain.Setup
import dev.niklas.ironcore.domain.Sex
import dev.niklas.ironcore.ui.components.BtnStyle
import dev.niklas.ironcore.ui.components.ChipRow
import dev.niklas.ironcore.ui.components.CyberButton
import dev.niklas.ironcore.ui.components.CyberPanel
import dev.niklas.ironcore.ui.components.FieldLabel
import dev.niklas.ironcore.ui.components.GlitchText
import dev.niklas.ironcore.ui.components.NeonBar
import dev.niklas.ironcore.ui.components.NumberField
import dev.niklas.ironcore.ui.components.OptionCard
import dev.niklas.ironcore.ui.components.PanelHeader
import dev.niklas.ironcore.ui.components.Stopwatch
import dev.niklas.ironcore.ui.components.TermInput
import dev.niklas.ironcore.ui.components.Txt
import dev.niklas.ironcore.ui.components.YesNo
import dev.niklas.ironcore.ui.theme.Cyber
import dev.niklas.ironcore.ui.theme.CyberPalette
import dev.niklas.ironcore.ui.theme.T
import kotlinx.coroutines.delay

@Composable
fun OnboardingFlow(vm: OnboardingVM, onRestore: (android.net.Uri) -> Unit = {}, restoreMessage: String? = null, onFinish: (Setup, Plan) -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    val flow = vm.flow
    val step = vm.step
    androidx.activity.compose.BackHandler(enabled = vm.index > 0 && step != Step.COMPILE) {
        fx.glitch(); if (step == Step.REPORT) vm.step = Step.SCAN else vm.back()
    }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // header: step counter + progress
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row {
                Txt("IRONCORE // INIT", T.mono(10.sp, p.textDim, .2.em))
                Spacer(Modifier.weight(1f))
                Txt("STEP ${(vm.index + 1).toString().padStart(2, '0')}/${flow.size}", T.mono(10.sp, p.textDim, .2.em))
            }
            Spacer(Modifier.height(6.dp))
            NeonBar((vm.index + 1) / flow.size.toFloat(), height = 4.dp, segments = flow.size, durationMs = 500)
        }
        AnimatedContent(
            step,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(tween(300)) { it / 6 * dir } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 6 * dir } + fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f),
            label = "step"
        ) { s ->
            when (s) {
                Step.COMPILE -> CompileStep(vm) { vm.next() }
                Step.REPORT -> {
                    val setup = remember { vm.toSetup() }
                    val plan = remember(setup) { PlanGenerator.generate(setup) }
                    ReportScreen(setup, plan, onBack = { vm.step = Step.SCAN }) { onFinish(setup, plan) }
                }
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    CyberPanel(Modifier.fillMaxWidth()) {
                        PanelHeader(s.tag, s.title)
                        StepContent(s, vm)
                    }
                    if (s == Step.INTRO) {
                        val restore = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
                            if (uri != null) onRestore(uri)
                        }
                        Spacer(Modifier.height(14.dp))
                        CyberPanel(Modifier.fillMaxWidth()) {
                            PanelHeader("DB", "EXISTING_OPERATIVE", "NEW PHONE?")
                            Txt("Restore your profile, progress and settings from an IRONCORE backup file.", T.body(14.sp, Cyber.c.textDim))
                            Spacer(Modifier.height(10.dp))
                            CyberButton("RESTORE FROM BACKUP", Modifier.fillMaxWidth(), BtnStyle.OUTLINE) {
                                restore.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                            }
                            restoreMessage?.let { Txt("> $it", T.mono(11.sp, if (it.contains("FAILED")) CyberPalette.DOWN else CyberPalette.UP), Modifier.padding(top = 8.dp)) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        if (step != Step.COMPILE && step != Step.REPORT) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(p.k04.copy(alpha = .9f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (vm.index > 0) CyberButton("< BACK", Modifier.weight(1f), BtnStyle.OUTLINE) { fx.glitch(); vm.back() }
                CyberButton(
                    if (step == Step.INTRO) "INITIALIZE >" else if (step == Step.SCAN) "COMPILE PROTOCOL >" else "NEXT >",
                    Modifier.weight(2f),
                    enabled = vm.valid(step)
                ) { vm.next() }
            }
        }
    }
}

@Composable
private fun Prompt(text: String, sub: String? = null) {
    val p = Cyber.c
    Txt(text, T.body(20.sp, Color.White, FontWeight.Bold))
    if (sub != null) Txt(sub, T.body(14.sp, p.textDim, FontWeight.Medium), Modifier.padding(top = 2.dp))
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Note(text: String, color: Color = Cyber.c.textDim) {
    Txt("> $text", T.mono(11.sp, color, 0.sp).copy(lineHeight = 1.5.em), Modifier.padding(top = 10.dp))
}

@Composable
private fun ColumnScope.StepContent(s: Step, vm: OnboardingVM) {
    val p = Cyber.c
    when (s) {
        Step.INTRO -> {
            GlitchText("IRONCORE", T.head(36.sp, p.accent, glow = 24f, weight = 900, spacing = .1.em))
            Txt("NEURAL TRAINING TERMINAL", T.mono(10.sp, p.textDim, .3.em))
            Spacer(Modifier.height(14.dp))
            Txt(
                "> NEW OPERATIVE DETECTED\n> NO BIOMETRIC PROFILE ON FILE\n> CALIBRATION REQUIRED\n\n" +
                    "The system will scan your body, history and objectives, then compile a training protocol built on sports-science guidelines (ACSM, WHO, EFSA).\n\n" +
                    "Takes ~5 minutes. Have space for a short fitness test.",
                T.mono(12.sp, p.text, 0.sp).copy(lineHeight = 1.55.em)
            )
            Note("ALL DATA STAYS ON THIS DEVICE. NO NETWORK ACCESS.", CyberPalette.UP)
        }
        Step.HANDLE -> {
            Prompt("Choose your operative handle.", "Shown on your profile.")
            TermInput(vm.handle, { vm.handle = it.uppercase() }, placeholder = "HANDLE_", maxLen = 16)
        }
        Step.BIOMETRICS -> {
            Prompt("Biometric scan.", "Used to calculate your energy needs and starting point.")
            FieldLabel("AGE")
            NumberField(vm.age, { vm.age = it }, "YEARS", 1f, 18f, 100f)
            vm.ageError()?.let { Note(it, CyberPalette.DOWN) }
            FieldLabel("SEX // FOR THE ENERGY FORMULA")
            ChipRow(Sex.entries.map { if (it == Sex.UNSPECIFIED) "OTHER" else it.label }, { Sex.entries[it] == vm.sex }, { vm.sex = Sex.entries[it] })
            FieldLabel("HEIGHT")
            NumberField(vm.heightCm, { vm.heightCm = it }, "CM", 1f, 120f, 230f)
            FieldLabel("WEIGHT")
            NumberField(vm.weightKg, { vm.weightKg = it }, "KG", .5f, 35f, 300f, decimals = 1)
            if (vm.sex == Sex.UNSPECIFIED) Note("OTHER uses the midpoint of the male/female formula constants.")
        }
        Step.ACTIVITY -> {
            Prompt("How active is your normal day?", "Not counting planned workouts. The app re-calibrates from your logs later.")
            Activity.entries.forEach { a ->
                OptionCard(a.label, a.desc, vm.activity == a, Modifier.padding(bottom = 8.dp), tag = "PAL ${a.pal}") { vm.activity = a }
            }
        }
        Step.EXPERIENCE -> {
            Prompt("Training history.", "How long have you trained regularly (strength or similar)?")
            Experience.entries.forEach { e -> OptionCard(e.label, e.desc, vm.experience == e, Modifier.padding(bottom = 8.dp)) { vm.experience = e } }
        }
        Step.HEALTH -> {
            Prompt("Health check.", "Based on the PAR-Q+ screening. Answer honestly. This only changes how carefully the plan starts.")
            HEALTH_QUESTIONS.forEachIndexed { i, q -> YesNo(q, vm.health[i]) { vm.health[i] = it } }
            if (vm.askPregnancy) YesNo("Are you currently pregnant?", vm.pregnant) { vm.pregnant = it }
            if (vm.health.any { it == true } || vm.pregnant == true) {
                Note("YES ON ANY QUESTION: talk to a doctor before increasing your activity. IRONCORE will start in CONSERVATIVE MODE (fewer sets, easier targets).", CyberPalette.AMBER)
            }
        }
        Step.GOAL -> {
            Prompt("Primary objective.")
            Goal.entries.forEach { g ->
                OptionCard(g.label, g.desc, vm.goal == g, Modifier.padding(bottom = 8.dp)) { vm.goal = g; if (vm.goal2 == g) vm.goal2 = null }
            }
            FieldLabel("SECONDARY OBJECTIVE // OPTIONAL", Modifier.padding(top = 8.dp))
            val others = Goal.entries.filter { it != vm.goal }
            others.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    row.forEach { g -> OptionCard(g.label, null, vm.goal2 == g, Modifier.weight(1f)) { vm.goal2 = if (vm.goal2 == g) null else g } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Step.TARGET -> {
            LaunchedEffect(Unit) { if (vm.targetKg == null) vm.targetKg = vm.weightKg }
            Prompt(if (vm.goal == Goal.LOSE_FAT) "Target weight." else "Target weight after gaining.", "Current: ${vm.weightKg} kg")
            NumberField(vm.targetKg, { vm.targetKg = it }, "KG", .5f, 35f, 300f, decimals = 1)
            vm.targetError()?.let { Note(it, CyberPalette.DOWN) }
            Note("Rates by intensity (next step): " + Intensity.entries.joinToString(" · ") { "${it.label} ${vm.ratePreview(it)}" })
            Note(if (vm.goal == Goal.LOSE_FAT) "0.5–1 % of body weight per week preserves muscle best (Helms 2014)."
                else "0.25–0.5 % of body weight per week limits fat gain (Iraki 2019).")
        }
        Step.INTENSITY -> {
            Prompt("Training intensity.", "Sets the pace of progression" + (if (vm.needsTarget) " and your weekly weight-change rate." else "."))
            Intensity.entries.forEach { i ->
                OptionCard(i.label, i.desc, vm.intensity == i, Modifier.padding(bottom = 8.dp), tag = vm.ratePreview(i)) { vm.intensity = i }
            }
        }
        Step.SCHEDULE -> {
            Prompt("Training days.", "Pick the weekdays you will train. Other days are rest days.")
            ChipRow(Format.DAY_SHORT, { (it + 1) in vm.days }, { d -> if ((d + 1) in vm.days) vm.days.remove(d + 1) else vm.days.add(d + 1) })
            Note("${vm.days.size} DAY(S) / WEEK" + if (vm.experience?.novice == true && vm.days.size > 3)
                " // NOVICE: max 3 full-body strength days (ACSM). Extra days become conditioning." else "")
            FieldLabel("SESSION LENGTH", Modifier.padding(top = 10.dp))
            val lens = listOf(20, 40, 60)
            ChipRow(lens.map { "~$it MIN" }, { lens[it] == vm.sessionMin }, { vm.sessionMin = lens[it] })
        }
        Step.EQUIPMENT -> {
            Prompt("Available equipment.", "Select everything you can use. None selected = bodyweight only.")
            Equip.entries.forEach { e ->
                val on = e in vm.equipment
                OptionCard(e.label, if (e == Equip.GYM) "All gym machines, barbells and dumbbells" else null, on, Modifier.padding(bottom = 8.dp)) {
                    if (on) vm.equipment.remove(e) else vm.equipment.add(e)
                }
            }
            if (vm.hasDumbbells) {
                FieldLabel("HEAVIEST DUMBBELL (PER HAND)")
                NumberField(vm.dumbbellMax, { vm.dumbbellMax = it }, "KG", 1f, 1f, 80f, decimals = 1)
            }
        }
        Step.SCAN -> ScanStep(vm)
        else -> {}
    }
}

@Composable
private fun ScanStep(vm: OnboardingVM) {
    val p = Cyber.c
    Prompt("System scan.", "Do each test now with good form. Rest 2–3 minutes between tests. Stop when form breaks, not at total exhaustion.")
    Note("You can also enter an honest estimate.")
    Spacer(Modifier.height(8.dp))
    Txt("01 // PUSH TEST", T.head(12.sp))
    FieldLabel("HARDEST PUSH-UP VARIATION YOU CAN DO WITH GOOD FORM")
    val ladder = Exercises.ladder("push_bw")
    ladder.steps.take(5).forEachIndexed { i, v ->
        OptionCard(v.name, if (vm.pushStep == i) v.cue else null, vm.pushStep == i, Modifier.padding(bottom = 6.dp)) { vm.pushStep = i }
    }
    FieldLabel("MAX REPS IN A ROW")
    NumberField(vm.pushReps, { vm.pushReps = it }, "REPS", 1f, 0f, 150f)
    Spacer(Modifier.height(14.dp))
    Txt("02 // SQUAT TEST", T.head(12.sp))
    FieldLabel("BODYWEIGHT SQUATS IN A ROW (STOP AT 50)")
    NumberField(vm.squatReps, { vm.squatReps = it }, "REPS", 1f, 0f, 50f)
    Spacer(Modifier.height(14.dp))
    Txt("03 // CORE TEST", T.head(12.sp))
    FieldLabel("FOREARM PLANK HOLD")
    Stopwatch { vm.plankSec = it.toFloat() }
    Spacer(Modifier.height(6.dp))
    NumberField(vm.plankSec, { vm.plankSec = it }, "SEC", 5f, 0f, 600f)
    if (vm.hasBar) {
        Spacer(Modifier.height(14.dp))
        Txt("04 // PULL TEST", T.head(12.sp))
        FieldLabel("STRICT PULL-UPS (0 IS FINE)")
        NumberField(vm.pullReps, { vm.pullReps = it }, "REPS", 1f, 0f, 60f)
    }
    Note("Results set your starting variations and targets. The system adjusts after every session.", p.textDim)
}

/** Terminal log while the protocol is generated; real numbers, then auto-advance. */
@Composable
private fun CompileStep(vm: OnboardingVM, onDone: () -> Unit) {
    val p = Cyber.c
    val fx = LocalFx.current
    val setup = remember { vm.toSetup() }
    val plan = remember { PlanGenerator.generate(setup) }
    val nut = remember {
        Nutrition.targets(setup.sex, setup.age, setup.weightKg, setup.heightCm, setup.activity, setup.goal, setup.intensity, setup.targetWeightKg, setup.pregnant)
    }
    val types = plan.sessions.values.map { it.type.label }.distinct()
    val lines = listOf(
        "PARSING BIOMETRICS" to "OK",
        "BMR (MIFFLIN-ST JEOR)" to "${nut.bmr} KCAL",
        "ENERGY EXPENDITURE" to "${nut.tdee} KCAL",
        "HEALTH SCREEN" to if (setup.conservative) "CONSERVATIVE" else "CLEAR",
        "SELECTING SPLIT" to "${plan.sessions.size}D/WK",
        "SESSION TYPES" to types.size.toString(),
        "MAPPING EXERCISE LADDERS" to "${plan.states.size} LOADED",
        "CALIBRATING TARGETS" to "OK",
        "FUEL MATRIX" to "${nut.kcal} KCAL",
        "PROJECTING FORECAST" to "OK",
        "COMPILING PROTOCOL" to "DONE"
    )
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (shown < lines.size) { delay(190); shown++; fx.boot() }
        delay(500); fx.complete(); delay(300); onDone()
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        CyberPanel(Modifier.fillMaxWidth()) {
            PanelHeader("CMP", "PROTOCOL_COMPILER", "RUNNING", live = true)
            val text = buildAnnotatedString {
                lines.take(shown).forEach { (l, r) ->
                    append("> $l ")
                    append(".".repeat((30 - l.length - r.length).coerceAtLeast(2)))
                    append(" ")
                    withStyle(SpanStyle(color = CyberPalette.UP)) { append(r) }
                    append("\n")
                }
                if (shown < lines.size) append("> _")
            }
            Txt(text, T.mono(12.sp, p.accent, 0.sp, glow = p.acc(.5f)).copy(lineHeight = 1.6.em))
            Spacer(Modifier.height(10.dp))
            NeonBar(shown / lines.size.toFloat(), height = 5.dp, durationMs = 180)
        }
    }
}
