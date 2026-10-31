package dev.niklas.ironcore.domain

/** Cosmetic slots that achievements unlock (SPEC §6). */
enum class CosmeticType(val label: String) { TITLE("TITLE"), FRAME("AVATAR FRAME"), BANNER("BANNER STYLE"), NAME_FX("NAME EFFECT") }

data class Cosmetic(val id: String, val type: CosmeticType, val name: String)

object Cosmetics {
    val ALL = listOf(
        // titles
        Cosmetic("t_newblood", CosmeticType.TITLE, "NEW BLOOD"),
        Cosmetic("t_firstblood", CosmeticType.TITLE, "FIRST BLOOD"),
        Cosmetic("t_ironwill", CosmeticType.TITLE, "IRON WILL"),
        Cosmetic("t_unbreakable", CosmeticType.TITLE, "UNBREAKABLE"),
        Cosmetic("t_machine", CosmeticType.TITLE, "THE MACHINE"),
        Cosmetic("t_pushdaemon", CosmeticType.TITLE, "PUSH DAEMON"),
        Cosmetic("t_gravity", CosmeticType.TITLE, "GRAVITY DEFIER"),
        Cosmetic("t_legday", CosmeticType.TITLE, "LEG DAY SURVIVOR"),
        Cosmetic("t_5k", CosmeticType.TITLE, "5K PROTOCOL"),
        Cosmetic("t_dataeater", CosmeticType.TITLE, "DATA EATER"),
        Cosmetic("t_hydro", CosmeticType.TITLE, "HYDRO CORE"),
        Cosmetic("t_patched", CosmeticType.TITLE, "PATCHED"),
        Cosmetic("t_overclocked", CosmeticType.TITLE, "OVERCLOCKED"),
        Cosmetic("t_blackice", CosmeticType.TITLE, "BLACK ICE"),
        Cosmetic("t_legend", CosmeticType.TITLE, "LIVING LEGEND"),
        Cosmetic("t_restmaster", CosmeticType.TITLE, "RECOVERY ARCHITECT"),
        // avatar frames
        Cosmetic("f_hex", CosmeticType.FRAME, "HEX"),
        Cosmetic("f_double", CosmeticType.FRAME, "DOUBLE HEX"),
        Cosmetic("f_circuit", CosmeticType.FRAME, "CIRCUIT"),
        Cosmetic("f_glitch", CosmeticType.FRAME, "GLITCH HEX"),
        Cosmetic("f_apex", CosmeticType.FRAME, "APEX CROWN"),
        // banner styles (when no custom banner image is set)
        Cosmetic("b_grid", CosmeticType.BANNER, "SYNTH GRID"),
        Cosmetic("b_rain", CosmeticType.BANNER, "DATA RAIN"),
        Cosmetic("b_static", CosmeticType.BANNER, "STATIC"),
        Cosmetic("b_sunset", CosmeticType.BANNER, "NEON SUNSET"),
        Cosmetic("b_circuit", CosmeticType.BANNER, "CIRCUIT BOARD"),
        // name effects
        Cosmetic("n_none", CosmeticType.NAME_FX, "NONE"),
        Cosmetic("n_glitch", CosmeticType.NAME_FX, "GLITCH"),
        Cosmetic("n_pulse", CosmeticType.NAME_FX, "NEON PULSE"),
        Cosmetic("n_chroma", CosmeticType.NAME_FX, "CHROMATIC"),
        Cosmetic("n_flicker", CosmeticType.NAME_FX, "BROKEN NEON"),
    )
    private val byId = ALL.associateBy { it.id }
    fun get(id: String) = byId[id]

    /** Always available. */
    val DEFAULTS = setOf("t_newblood", "f_hex", "b_grid", "n_none")
}

/** Everything achievements are evaluated against (snapshot from the database). */
data class Progress(
    val quests: Int = 0,
    val clears: Int = 0,
    val restDays: Int = 0,
    val bestStreak: Int = 0,
    val level: Int = 1,
    val foodLogs: Int = 0,
    val weighIns: Int = 0,
    val proteinDays: Int = 0,
    val waterDays: Int = 0,
    val activeMinutes: Int = 0,
    val c25kRuns: Int = 0,
    /** ladderId -> current variation step */
    val steps: Map<String, Int> = emptyMap(),
    val legQuests: Int = 0
)

enum class AchCategory { TRAINING, CONSISTENCY, PROGRESSION, FUEL, LEVEL }

data class Achievement(
    val id: String,
    val name: String,
    val desc: String,
    val category: AchCategory,
    val reward: String,                       // cosmetic id
    val goal: Int,
    val value: (Progress) -> Int
) {
    fun unlocked(p: Progress) = value(p) >= goal
}

object Achievements {
    private fun stepOf(p: Progress, id: String) = p.steps[id] ?: -1

    val ALL = listOf(
        // training volume
        Achievement("a_first", "FIRST BLOOD", "Complete your first quest", AchCategory.TRAINING, "t_firstblood", 1) { it.quests },
        Achievement("a_q50", "OPERATIVE", "Complete 50 quests", AchCategory.TRAINING, "f_circuit", 50) { it.quests },
        Achievement("a_q250", "WAR MACHINE", "Complete 250 quests", AchCategory.TRAINING, "t_machine", 250) { it.quests },
        Achievement("a_clear1", "DIRECTIVE EXECUTED", "Clear a full daily directive", AchCategory.TRAINING, "b_rain", 1) { it.clears },
        Achievement("a_clear25", "PROTOCOL ENFORCER", "Clear 25 daily directives", AchCategory.TRAINING, "b_circuit", 25) { it.clears },
        Achievement("a_legs", "LEG DAY SURVIVOR", "Complete 30 leg quests", AchCategory.TRAINING, "t_legday", 30) { it.legQuests },
        Achievement("a_active", "ALWAYS MOVING", "Log 600 active minutes", AchCategory.TRAINING, "b_sunset", 600) { it.activeMinutes },
        // consistency
        Achievement("a_s7", "IRON WILL", "Reach a 7-day streak", AchCategory.CONSISTENCY, "t_ironwill", 7) { it.bestStreak },
        Achievement("a_s14", "SIGNAL STRONG", "Reach a 14-day streak", AchCategory.CONSISTENCY, "n_pulse", 14) { it.bestStreak },
        Achievement("a_s30", "UNBREAKABLE", "Reach a 30-day streak", AchCategory.CONSISTENCY, "f_glitch", 30) { it.bestStreak },
        Achievement("a_s100", "THE MACHINE NEVER SLEEPS", "Reach a 100-day streak", AchCategory.CONSISTENCY, "t_unbreakable", 100) { it.bestStreak },
        Achievement("a_rest", "RECOVERY ARCHITECT", "Log 20 rest days", AchCategory.CONSISTENCY, "t_restmaster", 20) { it.restDays },
        // progression milestones (RESEARCH §3: variation ladders)
        Achievement("a_pushup", "PUSH DAEMON", "Unlock the full push-up", AchCategory.PROGRESSION, "t_pushdaemon", 1) { if (stepOf(it, "push_bw") >= 3) 1 else 0 },
        Achievement("a_pullup", "GRAVITY DEFIER", "Unlock the strict pull-up", AchCategory.PROGRESSION, "t_gravity", 1) { if (stepOf(it, "pull_bar") >= 2) 1 else 0 },
        Achievement("a_bss", "SINGLE-LEG SYSTEM", "Unlock the Bulgarian split squat", AchCategory.PROGRESSION, "n_chroma", 1) { if (stepOf(it, "squat_bw") >= 3) 1 else 0 },
        Achievement("a_c25k", "5K PROTOCOL", "Finish Couch to 5K (27 runs)", AchCategory.PROGRESSION, "t_5k", C25K.RUNS.size) { it.c25kRuns },
        // fuel
        Achievement("a_food", "DATA EATER", "Log 50 food entries", AchCategory.FUEL, "t_dataeater", 50) { it.foodLogs },
        Achievement("a_protein", "PROTEIN SYNTHESIS", "Hit your protein target on 10 days", AchCategory.FUEL, "b_static", 10) { it.proteinDays },
        Achievement("a_water", "HYDRO CORE", "Hit your water target on 7 days", AchCategory.FUEL, "t_hydro", 7) { it.waterDays },
        Achievement("a_weigh", "MASS SPECTROMETER", "Log 14 weigh-ins", AchCategory.FUEL, "n_flicker", 14) { it.weighIns },
        // level / rank
        Achievement("a_l5", "FIRMWARE UPDATE", "Reach level 5", AchCategory.LEVEL, "n_glitch", 5) { it.level },
        Achievement("a_l10", "PATCHED", "Reach level 10 (rank RUNNER)", AchCategory.LEVEL, "t_patched", 10) { it.level },
        Achievement("a_l15", "DUAL CORE", "Reach level 15", AchCategory.LEVEL, "f_double", 15) { it.level },
        Achievement("a_l22", "OVERCLOCKED", "Reach level 22 (rank OPERATIVE)", AchCategory.LEVEL, "t_overclocked", 22) { it.level },
        Achievement("a_l40", "BLACK ICE", "Reach level 40 (rank VANGUARD)", AchCategory.LEVEL, "t_blackice", 40) { it.level },
        Achievement("a_l46", "APEX PREDATOR", "Reach level 46 (rank APEX)", AchCategory.LEVEL, "f_apex", 46) { it.level },
        Achievement("a_l53", "LIVING LEGEND", "Reach level 53 (rank LEGEND)", AchCategory.LEVEL, "t_legend", 53) { it.level },
    )

    fun byId(id: String) = ALL.first { it.id == id }

    /** Achievements newly satisfied by [p] that are not in [unlocked] yet. */
    fun newlyUnlocked(p: Progress, unlocked: Set<String>) = ALL.filter { it.id !in unlocked && it.unlocked(p) }

    /** Cosmetics available to equip. */
    fun unlockedCosmetics(unlocked: Set<String>) = Cosmetics.DEFAULTS + ALL.filter { it.id in unlocked }.map { it.reward }
}
