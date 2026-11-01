# Handoff: Build an original, offline fitness RPG Android app

You are starting a fresh session. This file has everything decided so far and is the single source of truth for where the project stands. **Update the "Current status" and "Next steps" sections and push to GitHub after each meaningful chunk of work.**

Repo: https://github.com/Niklas-1999/fitness_app (public; clone over SSH: `git@github.com:Niklas-1999/fitness_app.git`).

## Current status (updated 2026-11-01)

- **P0–P6 are done and tested on the phone. v1.0 release is installed** (package `dev.niklas.ironcore`, next to the `.debug` dev build).
- **P6 contents (2026-11-01):** quest buttons say CLEAR. `data/Backup.kt`: generic JSON export/import of all tables + settings prefs + profile images (base64); imports older schemas (unknown columns skipped). UI: PROFILE → DATA_VAULT, and RESTORE FROM BACKUP on the first setup screen (new phone). `notify/Reminders.kt`: local reminders (directive reminder on training days if nothing is done yet, streak guard if the streak would break). Inexact `AlarmManager`, re-armed on boot/update; POST_NOTIFICATIONS asked when switched on. Release: R8 + `proguard-rules.pro` (Tesseract JNI keep rules), signed. `./release.sh` builds, verifies (no INTERNET, no Google classes), copies `IRONCORE-<version>.apk` to the project root and installs. Verified in the release build: restore from backup, OCR 8/8, notifications, alarm scheduled.
- **P5 contents (2026-11-01):** DB **v4** (`AutoMigration(3→4)`, tested): `achievement`, `cosmetics`, plus a `Counters` query. `domain/Achievements.kt` (catalog + `Cosmetics`, 2 tests incl. "every cosmetic is obtainable"). `ui/profile/`: `CosmeticViews` (IroncoreLogo, Avatar frames, Banner styles, StyledName effects), `ProfileTab` (OperativeCard, editor, achievement grid, settings, AchievementOverlay), `ImageStore` (photo picker → cropped JPEG in `filesDir/profile`). Launcher icon: `res/drawable/ic_launcher_*` + `mipmap-anydpi-v26`.
- **P4 contents (2026-11-01):** `ocr/LabelOcr.kt` (Tesseract4Android 4.9.0 from JitPack, `assets/tessdata/{deu,eng}.traineddata` from tessdata_fast; EXIF rotation, grayscale + contrast, tries PSM 6 and PSM 3 and keeps the better parse), `domain/LabelParser.kt` (7 tests incl. real on-device OCR output). FoodSheet: LABEL_SCANNER (camera via `TakePicture` + FileProvider `cache/scans`, gallery via photo picker) → DECODING_LABEL → pre-filled editor. ABI filter arm64-v8a. Verified on the phone: synthetic rotated, noisy label → 8/8 fields correct in 0.9 s. APK has no INTERNET permission and no Google classes. `tools/make_test_label.py` renders test labels.
- **P3 contents (2026-11-01):** DB **v3** (`AutoMigration(2→3)`, tested on the phone): `food` (EU label values per 100 g), `food_log`, plus water / protein-goal / water-goal / reroll columns on `day_log`. FUEL tab (`ui/fuel/`): gauge, macros, EU reference-intake info, water, intake log, weight tracker with trend chart, metabolic calibration status. `FoodSheet`: my foods (search, edit, delete) → portion → log; new food editor in EU label order (kcal or kJ). `domain/Adaptive.kt`: adaptive TDEE (3 tests). Nutrition rewards (+VIT). **Daily reroll** and **ARSENAL** free training (see SPEC §4).
- **P2 contents:**
  - DB **v2** via Room `AutoMigration(1→2)`: `quest_log`, `day_log`, attribute point columns. Tested: a v1 install upgraded on the phone with its data kept.
  - `domain/Rewards.kt`: XP (base × rating × daily diminishing × streak), attribute points, streak rules, rest times (ACSM). `PlanGenerator.extraSession`: recovery-aware TRAIN ANYWAY. 21 unit tests.
  - `data/Repo.completeQuest`: one transaction that applies progression (no progression in deload weeks), XP, attribute points, streak, directive-clear bonus and level-up snapshot.
  - UI `ui/quest/`: `QuestRunner` (load calibration, set-by-set flow, rest timer, hold timer, "log all sets", effort rating), `RunTimer` (NHS C25K phases, vibration at phase changes, screen stays on), `WalkLogger`, `RewardOverlay`. Then the terminal level-up.
  - HUD: daily directive with EXECUTE/CLEARED, rest day with REST PROTOCOL / TRAIN ANYWAY (bonus ops), weekly active minutes (WHO 150). WEEK tab: OPS_LOG (14 days) + protocol. PROFILE: a debug-only "test run timer" (only in the `.debug` build).
- The phone currently holds a **test profile ("RUNNER")**. Niklas should RE-INITIALIZE with his real data.
- **App name IRONCORE**, package `dev.niklas.ironcore` (debug: `.debug`). Never change it after the first release install.
- **Custom sounds:** `sounds/<role>.mp3` + `tools/import_sfx.sh` (see `sounds/README.md`).
- **Testing helpers:** `tools/phone.sh` (tap by text, `edit N value`, shot, texts) drives the app over adb.

## Release signing (IMPORTANT)

- Keystore: `~/android-tools/keys/ironcore-release.jks`, alias `ironcore`, RSA 4096, valid 100 years. The SHA-256 cert fingerprint starts with `EF:C5:D8:73`.
- Passwords: in `~/android-tools/keys/keystore.properties`, copied to the project root as `keystore.properties` (git-ignored).
- **Niklas must back up both files** (USB stick + password manager). Without them, updates can't be installed over the existing app, and a reinstall loses the data unless an IRONCORE backup file exists.
- On a new PC: copy both files back, put `keystore.properties` in the project root (fix `storeFile` if the home path differs), then run `./release.sh`.
- Every release: bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`, run `./release.sh`, and `adb install -r` keeps the data.

## Next steps

1. Niklas uses v1.0 day to day (re-initialize with real data; the release app currently holds the restored RUNNER test profile). Collect feedback: target difficulty, XP pacing, real-label OCR misreads (add their raw OCR text to `LabelParserTest`).
2. Possible v1.x ideas: history charts per exercise, editable plan (swap exercises / days), more achievements, a scan crop frame, a home-screen widget. **v2 (decided: later):** opt-in sync, leaderboard and friends against a self-hosted server (SPEC §9).
3. Known small issues: walk quests don't log the user's actual pace; there's no history chart yet; the run timer relies on the screen staying on (no foreground service).

## Dev loop (how Niklas sees changes live)

- `./dev.sh` builds the debug APK, installs it as an update over adb (data is kept) and relaunches the app. About 20 s with a warm Gradle daemon. The first build after a clone takes 1–2 min.
- `./dev.sh mirror` also opens **scrcpy**, which mirrors the phone in a laptop window that he can control with the mouse. He watches there and gives feedback.
- Claude verifies visually itself with `adb exec-out screencap -p > shot.png` (then reads the image) and checks crashes with `adb logcat -b crash`.
- Debug builds have the `.debug` suffix, so they install **next to** the release app with separate data. Real progress lives only in the release app.

## Setting up a new PC

Everything lives in `~/android-tools/` (no sudo). On a new machine, recreate it:
- JDK 21: Temurin tarball → `~/android-tools/jdk21` (`https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse`). The system JDK 25 is too new for Gradle 8.11/AGP 8.7.
- Android SDK: cmdline-tools → `~/android-tools/cmdline-tools`, then `sdkmanager --sdk_root=$HOME/android-tools/sdk "platforms;android-35" "platform-tools"`. Gradle auto-installs build-tools 34.
- scrcpy: the linux-x86_64 release tarball from GitHub (Genymobile/scrcpy) → `~/android-tools/scrcpy/`.
- Create `local.properties` with `sdk.dir=/home/<user>/android-tools/sdk` (git-ignored).
- Copy the release keystore + `keystore.properties` over from the backup (once they exist; they are never in git).
- Versions in use: Gradle 8.11.1 (wrapper), AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01, compileSdk/targetSdk 35, minSdk 26.

## Roles and the deliverable

- **Niklas is the architect and product owner.** He describes ideas, mechanics and visual preferences. He does not want to write code.
- **You are the engineer.** You design the details, write all the code, build it and install it.
- **The deliverable is a fully working, signed, installable APK** on his phone.
- **Project docs:** `SPEC.md` (what we build), `RESEARCH.md` (evidence for every training/nutrition number), `tools/` (sound generator, level curve simulation). A prototype or "here's the code, build it yourself" is not enough. Iterate until it runs.
- Niklas has built and deployed web apps, games and GitHub Pages sites with Claude before. He is technical and trusts you to make engineering decisions.
- **Working style:** concise and direct. Decide sensible defaults and report them, without long back-and-forth. When showing edits, show only the changed parts. Ask only about things that are truly his call: mechanics, look and feel, and priorities.

## What we are building

An **original** gamified workout tracker in the "fitness RPG / leveling system" genre. The app loop is: train in real life → log it → earn XP → level up, raise stats, complete quests, keep streaks.

Hard requirements:

1. **100% offline.** No `INTERNET` permission in the manifest at all. No accounts, servers, sync or cloud.
2. **All data stored locally** on the phone, in an on-device database. Provide export and import to a file, so the data can be backed up and moved to a new phone.
3. **No Google or third-party services.** No Firebase, Play Services, analytics, crash reporting, ads, attribution SDKs or push services. AndroidX and Jetpack libraries are fine: they are plain open-source libraries, not services.
4. **No paywalls, ads or tracking.** It is his own app.
5. **Sideloaded only.** It will not be published to the Play Store.

## Background: why we are not patching "Arise"

The previous session tried to mod the commercial app **"Arise: Workout & Fitness RPG"** (`llc.sololeveling.Arise` v1.4.10) to remove paywalls, ads and trackers and to make it offline. That was abandoned:

- The app is protected by Google Play's license check (`com.pairip`). A re-signed APK will not run without defeating that check, and that is off-limits.
- Removing paywalls means bypassing licensing, which is also off-limits.
- The app is React Native (Expo) with Hermes bytecode and depends on Firebase Auth, Firestore, Cloud Functions and other backends, so an offline version was not realistic either.
- Rebuilding it "1:1" from its code, art and layout was also declined. That would be a copy of their app.

**Rules for this project, which follow from that:**

- **Do not** decompile, read or reuse anything from the Arise APK: no code, assets, layouts, text, XP formulas or quest data. The pulled APKs are still in `/home/niklas/Documents/arise_app_mod/original/` and `backup/`. Do not touch them for this project.
- **Do not** use Solo Leveling names, characters, artwork, logos or phrases as branding. That includes the app name and catchphrases.
- **Allowed:** general genre conventions, such as dark UIs, glowing "system window" panels, popup notifications, ranks, stats and daily quests. Also allowed: anything Niklas describes from his own experience as a user, and his screenshots as a *mood reference*. We design our own layouts, art, wording, formulas and name.

## Environment (already verified)

- **PC:** Ubuntu Linux, x86_64, kernel 7.0. Username `niklas`. No sudo was used. Everything is installed to user folders.
- **Java:** OpenJDK 25 is the system default but too new for this Gradle/AGP. `dev.sh` uses `~/android-tools/jdk21` automatically. For manual Gradle calls, set `JAVA_HOME=~/android-tools/jdk21`.
- **Installed in `~/android-tools/` on Niklas's main PC:** `jdk21/`, `gradle/` (only used to create the wrapper), `scrcpy/`, `cmdline-tools/`, `sdk/` (platform 35, build-tools 34+35, platform-tools; use `sdk/platform-tools/adb`). The leftovers `apktool.jar`, `jadx/`, `APKEditor.jar` and `platform-tools/` (older adb) are from the mod attempt and not needed.
- **Permissions note:** Claude Code auto mode blocked `pip install git+https://github.com/...`. Gradle and Maven dependency downloads may also need Niklas to approve, so tell him what is being downloaded and why.
- **Phone:** Samsung Galaxy A57 (One UI), adb serial `R5GL706QQGN`. USB debugging is on and this PC is authorized.
  - If it shows `unauthorized`: unlock the phone and run `adb kill-server && adb start-server`. That brought the prompt back last time. Auto Blocker must be off.
  - `adb shell pm` prints harmless `SecurityException ... user 150` errors (Samsung Secure Folder). Ignore them.

## Recommended tech stack (default; report it to Niklas, no need to ask)

- **Kotlin + Jetpack Compose** (Material 3, customized heavily for the game look), single-activity app.
- **Room** (SQLite) for local data. Use the DataStore library for settings.
- **Export and import** through the system file picker (Storage Access Framework, so no storage permission is needed), as JSON.
- **Local notifications** for reminders, using AlarmManager or WorkManager. `POST_NOTIFICATIONS` is the only runtime permission we expect to need.
- Build with **Gradle from the command line** (`./gradlew assembleRelease`). No Android Studio needed, and no EAS or other cloud builds.
- Why not React Native/Expo: Kotlin/Compose gives a smaller and fully offline APK without a JS runtime, and avoids Expo modules that pull in Google services.

## Build, sign and install

1. Project directory: `/home/niklas/Documents/fitness_rpg/` on the main PC, git repo above. `.gitignore` excludes the keystore, `keystore.properties`, `local.properties` and build output.
2. Generate a **release keystore once** and keep it outside the repo, for example `~/android-tools/keys/release.jks`. Tell Niklas to back it up. **If the key is lost, updates will no longer install over the existing app, and installing fresh wipes the local data.** Always sign with this same key.
3. Build with `./gradlew assembleRelease`, signed through Gradle's `signingConfig` with credentials taken from a local, git-ignored `keystore.properties`.
4. Install with `~/android-tools/platform-tools/adb install -r app-release.apk`. `-r` keeps the data.
5. Check `adb logcat` for crashes after each install. Test on the real phone. Before handing it over, verify: no `INTERNET` permission (`aapt dump permissions` or `apkanalyzer`) and no Google/Firebase classes.
6. Use **Room migrations** from version 1 onward. Do not use destructive migrations, so updates never wipe his progress.

## Process

1. **Discovery (first thing in the new session).** Ask Niklas these questions in one go, short and grouped:
   - What does he do on a normal day in a fitness RPG app? Which 2–3 mechanics motivate him most?
   - His training: bodyweight or gym, which exercises, and does he log sets, reps, weight or time?
   - Progression: levels, stats (which ones?), ranks, titles, quests (daily, weekly, custom?), streaks, penalties for missed days (yes or no)?
   - Visual style: colors, mood, references or screenshots, sound and haptics?
   - App name, and any must-have extras (charts, rest timer, body-weight tracking, reminders)?
2. **Spec.** Write a short `SPEC.md` in the project with screens, data model, XP and level formulas, and the quest system. Have Niklas approve it.
3. **MVP.** Build the core loop: log a workout → XP → level up → daily quests, with local storage. Install it on the phone early and iterate on real feedback.
4. **Polish.** Add the visual style, animations, level-up and quest-complete popups, stats screen, history and charts, reminders, and export/import.
5. **Each round:** build, install on the phone, then give a short report of what changed, what to test, and any known issues.

## Housekeeping

- `/home/niklas/Documents/arise_app_mod/` (old folder) contains the old mod attempt (`original/`, `backup/`, `work/`). It is not needed for the new app. Niklas may delete it.
