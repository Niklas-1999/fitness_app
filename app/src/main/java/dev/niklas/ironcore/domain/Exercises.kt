package dev.niklas.ironcore.domain

/**
 * Exercise library. Bodyweight work progresses through harder variations (a ladder,
 * RESEARCH §3); loaded work progresses by load (ACSM 2–10% rule, RESEARCH §1).
 */
enum class Measure { REPS, HOLD, LOAD }

enum class Pattern { PUSH, PUSH_V, PULL, ROW, SQUAT, LUNGE, HINGE, CORE, CORE_SIDE, BALANCE }

data class Variation(
    val id: String,
    val name: String,
    val measure: Measure,
    val cue: String,
    /** per-variation fixed range overrides the goal range (e.g. negatives, hangs) */
    val fixedRange: IntRange? = null,
    val perSide: Boolean = false
)

data class Ladder(
    val id: String,
    val pattern: Pattern,
    val stat: Stat,
    val requires: Set<Equip>,
    val steps: List<Variation>,
    /** large-muscle lifts get bigger load jumps (ACSM: higher % for large muscle mass) */
    val largeMuscle: Boolean = false
) {
    val isLoad get() = steps.first().measure == Measure.LOAD
}

object Exercises {
    private fun v(id: String, name: String, m: Measure, cue: String, range: IntRange? = null, perSide: Boolean = false) =
        Variation(id, name, m, cue, range, perSide)

    private val R = Measure.REPS
    private val H = Measure.HOLD
    private val L = Measure.LOAD

    val ALL: List<Ladder> = listOf(
        // ---------- horizontal push ----------
        Ladder("push_bw", Pattern.PUSH, Stat.STR, emptySet(), listOf(
            v("wall_pushup", "WALL PUSH-UP", R, "Hands on wall at shoulder height, body straight, chest to wall."),
            v("incline_pushup", "INCLINE PUSH-UP", R, "Hands on a table or bench edge, body in one line, lower chest to edge."),
            v("knee_pushup", "KNEE PUSH-UP", R, "Knees down, hips in line with shoulders, chest to floor."),
            v("pushup", "PUSH-UP", R, "Body rigid like a plank, elbows ~45°, chest to floor."),
            v("decline_pushup", "DECLINE PUSH-UP", R, "Feet on a chair or bench, body straight, chest to floor."),
            v("archer_pushup", "ARCHER PUSH-UP", R, "Wide hands, shift onto one arm while the other stays straight.", perSide = true)
        )),
        Ladder("push_db", Pattern.PUSH, Stat.STR, setOf(Equip.DUMBBELLS), listOf(
            v("db_floor_press", "DUMBBELL FLOOR PRESS", L, "Lie on the floor, press dumbbells up, upper arms touch the floor each rep.")
        )),
        Ladder("push_db_bench", Pattern.PUSH, Stat.STR, setOf(Equip.DUMBBELLS, Equip.BENCH), listOf(
            v("db_bench_press", "DUMBBELL BENCH PRESS", L, "Feet planted, shoulder blades pulled back, lower to chest level.")
        )),
        Ladder("push_gym", Pattern.PUSH, Stat.STR, setOf(Equip.GYM), listOf(
            v("bench_press", "BENCH PRESS", L, "Shoulder blades back, bar to lower chest, use a spotter or safety pins.")
        ), largeMuscle = true),

        // ---------- vertical push ----------
        Ladder("pushv_bw", Pattern.PUSH_V, Stat.STR, emptySet(), listOf(
            v("prone_y_raise", "PRONE Y-RAISE", R, "Lie face down, thumbs up, lift arms in a Y shape and squeeze shoulder blades."),
            v("pike_pushup", "PIKE PUSH-UP", R, "Hips high in an inverted V, lower the top of your head toward the floor."),
            v("elevated_pike_pushup", "ELEVATED PIKE PUSH-UP", R, "Feet on a chair, hips high, head toward the floor.")
        )),
        Ladder("pushv_db", Pattern.PUSH_V, Stat.STR, setOf(Equip.DUMBBELLS), listOf(
            v("db_overhead_press", "DUMBBELL OVERHEAD PRESS", L, "Brace your core, press straight up, don't arch your lower back.")
        )),
        Ladder("pushv_gym", Pattern.PUSH_V, Stat.STR, setOf(Equip.GYM), listOf(
            v("overhead_press", "OVERHEAD PRESS", L, "Glutes and core tight, bar path straight over mid-foot.")
        )),

        // ---------- vertical pull ----------
        Ladder("pull_bar", Pattern.PULL, Stat.STR, setOf(Equip.PULLUP_BAR), listOf(
            v("dead_hang", "DEAD HANG", H, "Hang from the bar with straight arms, shoulders active.", 15..45),
            v("negative_pullup", "NEGATIVE PULL-UP", R, "Jump or step to the top, lower yourself as slowly as possible (3–5 s).", 3..6),
            v("pullup", "PULL-UP", R, "Full hang to chin over bar, no swinging."),
        )),
        Ladder("pull_gym", Pattern.PULL, Stat.STR, setOf(Equip.GYM), listOf(
            v("lat_pulldown", "LAT PULLDOWN", L, "Pull the bar to your upper chest, elbows down and back.")
        ), largeMuscle = true),

        // ---------- horizontal pull / row ----------
        Ladder("row_bw", Pattern.ROW, Stat.STR, emptySet(), listOf(
            v("prone_w_raise", "PRONE W-RAISE", R, "Lie face down, elbows bent, pull them back into a W and squeeze."),
            v("superman_hold", "SUPERMAN HOLD", H, "Face down, lift arms, chest and legs slightly, hold.", 15..45),
            v("table_row", "TABLE ROW", R, "Lie under a STURDY table, grip the edge, pull chest to table. Test stability first!")
        )),
        Ladder("row_band", Pattern.ROW, Stat.STR, setOf(Equip.BANDS), listOf(
            v("band_row", "BAND ROW", R, "Anchor the band at chest height, pull elbows back, squeeze shoulder blades.")
        )),
        Ladder("row_db", Pattern.ROW, Stat.STR, setOf(Equip.DUMBBELLS), listOf(
            v("db_row", "ONE-ARM DUMBBELL ROW", L, "Hand and knee on a support, back flat, pull dumbbell to hip.", perSide = true)
        )),
        Ladder("row_gym", Pattern.ROW, Stat.STR, setOf(Equip.GYM), listOf(
            v("cable_row", "SEATED CABLE ROW", L, "Chest up, pull handle to belly, don't lean back.")
        ), largeMuscle = true),

        // ---------- squat ----------
        Ladder("squat_bw", Pattern.SQUAT, Stat.STR, emptySet(), listOf(
            v("sit_to_stand", "SIT-TO-STAND", R, "Sit down to a chair and stand back up without using your hands."),
            v("bw_squat", "BODYWEIGHT SQUAT", R, "Feet shoulder-width, knees track over toes, hips below knees if you can."),
            v("split_squat", "SPLIT SQUAT", R, "Long stance, lower the back knee toward the floor, front heel down.", perSide = true),
            v("bulgarian_split_squat", "BULGARIAN SPLIT SQUAT", R, "Back foot on a chair, lower until the front thigh is about parallel.", perSide = true)
        )),
        Ladder("squat_db", Pattern.SQUAT, Stat.STR, setOf(Equip.DUMBBELLS), listOf(
            v("goblet_squat", "GOBLET SQUAT", L, "Hold one dumbbell at the chest, sit between your heels, chest up.")
        ), largeMuscle = true),
        Ladder("squat_kb", Pattern.SQUAT, Stat.STR, setOf(Equip.KETTLEBELL), listOf(
            v("kb_goblet_squat", "KETTLEBELL GOBLET SQUAT", L, "Hold the bell by the horns at the chest, sit between your heels.")
        ), largeMuscle = true),
        Ladder("squat_gym", Pattern.SQUAT, Stat.STR, setOf(Equip.GYM), listOf(
            v("back_squat", "BACK SQUAT", L, "Bar on upper back, brace, squat to depth you control. Use safety pins.")
        ), largeMuscle = true),

        // ---------- lunge ----------
        Ladder("lunge_bw", Pattern.LUNGE, Stat.STR, emptySet(), listOf(
            v("reverse_lunge", "REVERSE LUNGE", R, "Step back, lower the back knee toward the floor, push through the front heel.", perSide = true),
            v("walking_lunge", "WALKING LUNGE", R, "Lunge forward step after step, torso upright.", perSide = true)
        )),
        Ladder("lunge_db", Pattern.LUNGE, Stat.STR, setOf(Equip.DUMBBELLS), listOf(
            v("db_reverse_lunge", "DUMBBELL REVERSE LUNGE", L, "Dumbbells at your sides, step back, front knee over mid-foot.", perSide = true)
        ), largeMuscle = true),

        // ---------- hinge ----------
        Ladder("hinge_bw", Pattern.HINGE, Stat.STR, emptySet(), listOf(
            v("glute_bridge", "GLUTE BRIDGE", R, "On your back, feet flat, drive hips up and squeeze glutes at the top."),
            v("sl_glute_bridge", "SINGLE-LEG GLUTE BRIDGE", R, "One foot down, other leg up, drive hips up level.", perSide = true),
            v("sl_hip_thrust", "SINGLE-LEG HIP THRUST", R, "Shoulders on a sofa or bench, one foot down, hips up to level.", perSide = true)
        )),
        Ladder("hinge_db", Pattern.HINGE, Stat.STR, setOf(Equip.DUMBBELLS), listOf(
            v("db_rdl", "DUMBBELL ROMANIAN DEADLIFT", L, "Soft knees, push hips back, dumbbells slide along thighs, flat back.")
        ), largeMuscle = true),
        Ladder("hinge_kb", Pattern.HINGE, Stat.STR, setOf(Equip.KETTLEBELL), listOf(
            v("kb_rdl", "KETTLEBELL ROMANIAN DEADLIFT", L, "Soft knees, hips back, bell close to legs, flat back.")
        ), largeMuscle = true),
        Ladder("hinge_gym", Pattern.HINGE, Stat.STR, setOf(Equip.GYM), listOf(
            v("rdl", "ROMANIAN DEADLIFT", L, "Bar close to legs, hips back, flat back, stop when hamstrings are stretched.")
        ), largeMuscle = true),

        // ---------- core ----------
        Ladder("core_front", Pattern.CORE, Stat.AGI, emptySet(), listOf(
            v("dead_bug", "DEAD BUG", R, "On your back, lower back pressed down, extend opposite arm and leg slowly.", perSide = true),
            v("plank", "PLANK", H, "Forearms down, body straight from head to heels, squeeze glutes."),
            v("hollow_hold", "HOLLOW HOLD", H, "On your back, lower back pressed down, arms and legs extended off the floor.")
        )),
        Ladder("core_side", Pattern.CORE_SIDE, Stat.AGI, emptySet(), listOf(
            v("side_plank_knee", "KNEE SIDE PLANK", H, "On your side, knees bent, hips lifted in line with shoulders.", perSide = true),
            v("side_plank", "SIDE PLANK", H, "Feet stacked, body straight, hips high.", perSide = true)
        )),

        // ---------- balance (WHO: older adults, RESEARCH §6) ----------
        Ladder("balance", Pattern.BALANCE, Stat.AGI, emptySet(), listOf(
            v("tandem_stance", "TANDEM STANCE", H, "Stand heel-to-toe near a wall or chair for safety.", 15..45),
            v("single_leg_stand", "SINGLE-LEG STAND", H, "Stand on one leg near a support, eyes forward.", 15..45, perSide = true)
        ))
    )

    private val byId = ALL.associateBy { it.id }
    fun ladder(id: String) = byId.getValue(id)

    /** Best ladder for a movement pattern given equipment; load ladders preferred when available. */
    fun pick(pattern: Pattern, equip: Set<Equip>, preferBodyweight: Boolean): Ladder? {
        val candidates = ALL.filter { it.pattern == pattern && equip.containsAll(it.requires) }
        if (candidates.isEmpty()) return null
        if (preferBodyweight) candidates.firstOrNull { !it.isLoad }?.let { return it }
        // most specific (most requirements) loaded option first, e.g. gym > db+bench > db
        return candidates.filter { it.isLoad }
            .maxWithOrNull(compareBy<Ladder>({ Equip.GYM in it.requires }, { it.requires.size }))
            ?: candidates.first()
    }
}
