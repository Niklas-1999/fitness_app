package dev.niklas.ironcore.domain

enum class Sex(val label: String) { MALE("MALE"), FEMALE("FEMALE"), UNSPECIFIED("NOT SPECIFIED") }

/** EFSA PAL values (RESEARCH §9). Daily life only, excluding planned workouts. */
enum class Activity(val pal: Float, val label: String, val desc: String) {
    LOW(1.4f, "LOW ACTIVE", "Desk job, little walking, mostly sitting"),
    MODERATE(1.6f, "MODERATELY ACTIVE", "Some walking or standing during the day"),
    ACTIVE(1.8f, "ACTIVE", "On your feet most of the day, or a lot of walking/cycling"),
    VERY_ACTIVE(2.0f, "VERY ACTIVE", "Physically demanding job or very active lifestyle")
}

/** ACSM training status (RESEARCH §1). */
enum class Experience(val label: String, val desc: String, val novice: Boolean) {
    NONE("NONE", "Never trained regularly", true),
    BEGINNER("< 6 MONTHS", "Trained a little, or long ago", true),
    INTERMEDIATE("6–24 MONTHS", "Regular training for a while", false),
    ADVANCED("> 2 YEARS", "Years of consistent training", false)
}

enum class Goal(val label: String, val desc: String) {
    LOSE_FAT("LOSE FAT", "Reduce body fat, keep muscle"),
    BUILD_MUSCLE("BUILD MUSCLE", "Gain muscle size"),
    STRENGTH("GET STRONGER", "Lift more, master harder variations"),
    ENDURANCE("ENDURANCE", "Run 5 km non-stop and build stamina"),
    GENERAL("GENERAL FITNESS", "Feel better, move better, stay healthy")
}

enum class Intensity(val label: String, val desc: String) {
    GENTLE("GENTLE", "Slow and steady, easy to stick with"),
    STANDARD("STANDARD", "Balanced pace, clear progress"),
    HARD("HARD", "Fast pace, demanding sessions")
}

/** Bitmask-able equipment flags. GYM implies everything. */
enum class Equip(val label: String) {
    PULLUP_BAR("PULL-UP BAR"), BANDS("RESISTANCE BANDS"), DUMBBELLS("DUMBBELLS"),
    KETTLEBELL("KETTLEBELL"), BENCH("BENCH"), GYM("FULL GYM");

    val bit get() = 1 shl ordinal

    companion object {
        fun fromMask(mask: Int): Set<Equip> {
            val s = entries.filter { mask and it.bit != 0 }.toMutableSet()
            if (GYM in s) s += entries
            return s
        }
        fun toMask(set: Set<Equip>) = set.fold(0) { m, e -> m or e.bit }
    }
}

/** EASY/SOLID/HARD/FAILED = simplified reps-in-reserve bands (RESEARCH §4). */
enum class Rating { EASY, SOLID, HARD, FAILED }

enum class Stat(val short: String) { STR("STR"), END("END"), AGI("AGI"), VIT("VIT"), WIL("WIL") }
