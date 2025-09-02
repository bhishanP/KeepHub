# KeepHub — Android vocab app (Kotlin + Compose)

KeepHub helps English-proficiency exam takers **grow and retain vocabulary**. Add words fast, see clean details (definitions, IPA, examples, audio), and review them with **spaced repetition** + **daily reminders**. Runs offline with cached data and mock services.

---

## ✨ Features

* **Add words fast**

    * Manual entry + Share Intent prefill
    * Duplicate prevention (case/diacritics & basic inflection normalization)
    * Tags & notes
* **Word details**

    * Meanings, POS, **IPA**, examples, synonyms/antonyms
    * **Pronunciation audio** (Free Dictionary API phonetics)
    * Offline caching; **Refresh** button
* **Spaced repetition**

    * **SM-2** scheduler (`easiness`, `intervalDays`, `repetitions`, `dueDate`, `lapses`, `history`)
    * Daily review queue (fills with “new” words if under goal)
    * Quizzes: **MCQ**, **Type**, **Cloze**
    * Results recorded to Room
* **Reminders**

    * WorkManager daily job + local notification
    * Deep-links directly to today’s Review
* **Settings**

    * Daily goal, quiz type toggles, reminder time (24h)
    * Translation language (feature can be disabled via flag)
* **Polish & A11y**

    * Material 3, large touch targets, TalkBack labels
    * Empty/error/loading states; shimmer placeholders
    * Scrollable details view; RTL & dynamic text friendly
* **Manage words**

    * Delete with confirmation (removes all linked data)

---

## 🧱 Tech & Architecture

* **Language/UI:** Kotlin, Jetpack Compose (Material 3)
* **Architecture:** MVVM + Repository; Hilt (DI)
* **Async:** Kotlin Coroutines + Flow
* **Persistence:** Room (offline-first), DataStore (settings)
* **Background:** WorkManager + Notification API
* **Networking:** OkHttp (optional real clients), mock services default

**Android:** minSdk 24, compile/target SDK latest supported by your AGP
**JDK:** 17

---

## 🗂️ Project structure

```
KeepHub/
├─ app/                         # UI, nav, workers, notifications
│  ├─ src/main/java/com/keephub/app/
│  │  ├─ MainActivity.kt
│  │  ├─ KeepHubApp.kt          # Hilt Application + initial scheduling
│  │  ├─ nav/NavGraph.kt
│  │  ├─ notifications/ReviewNotification.kt
│  │  ├─ work/DueReviewWorker.kt
│  │  ├─ ui/components/States.kt
│  │  ├─ ui/screens/
│  │  │  ├─ WordListScreen.kt
│  │  │  ├─ AddWordScreen.kt
│  │  │  ├─ WordDetailScreen.kt  # audio chips + delete confirm
│  │  │  ├─ ReviewScreen.kt      # MCQ / Type / Cloze
│  │  │  └─ ReviewAndSettings.kt # Settings screen
│  │  └─ ui/review/              # Question models + VM
│  └─ src/main/res/              # Adaptive icon, theme, strings
└─ core-data/                    # Room, repos, services, settings
   ├─ db/                        # Entities, DAOs, AppDatabase (+ migrations)
   ├─ repo/                      # WordRepository (+ SRS, enrich, delete)
   ├─ srs/                       # SM-2 engine + tests
   ├─ net/                       # Mock + real services
   ├─ di/                        # Hilt modules (DB/Network/Repo)
   └─ settings/                  # DataStore SettingsStore
```

---

## 🔧 Getting started

### 1) Requirements

* Android Studio (Koala+)
* JDK 17
* Android SDK + emulator or device (Android 7.0+)

### 2) Build

```bash
./gradlew assembleDebug
```

---

## ⚙️ Configuration & build flags

All flags live in **`:core-data/build.gradle.kts`** via `buildConfigField`.

| Flag                           |                           Default | What it does                                                                                                 |
| ------------------------------ | --------------------------------: | ------------------------------------------------------------------------------------------------------------ |
| `USE_MOCK` (boolean)           |                            `true` | Use **mock services** (offline-friendly). Set `false` to hit real endpoints.                                 |
| `ENABLE_TRANSLATION` (boolean) |                           `false` | Toggle translation feature globally. When `false`, repo skips translation calls and UI can hide the section. |
| `DICT_BASE_URL` (String)       | `"https://api.dictionaryapi.dev"` | Free Dictionary API base.                                                                                    |
| `TRANSLATE_BASE_URL` (String)  |                              `""` | LibreTranslate-compatible API base (e.g., `"https://libretranslate.de"`).                                    |
| `TRANSLATE_API_KEY` (String)   |                              `""` | Optional key for your translate server.                                                                      |

> **Recommended dev setup:** keep `USE_MOCK = true` and `ENABLE_TRANSLATION = false`.
> Flip `USE_MOCK = false` and set `TRANSLATE_BASE_URL` only when you want real network.

---

## 🗄️ Database & migrations

Room entities (key tables):

* `words` (`WordEntity`): term, normalizedTerm, baseLang, notes, tags, timestamps
* `senses` (`SenseEntity`): pos, definition, ipa, examples\[], synonyms\[], antonyms\[], **audioUrls\[]**
* `translations` (`TranslationEntity`): wordId, languageCode, text
* `srs_state` (`SrsStateEntity`): easiness, intervalDays, repetitions, dueDate, lapses, historyJson
* `quiz_results` (`QuizResultEntity`): mode, correct, timeMs, timestamp

**Migrations**

* **v1 → v2:** add `audioUrls` column to `senses` (TEXT JSON, default `'[]'`).
  Database builder includes `addMigrations(MIGRATION_1_2)`.

---

## 🔔 Notifications & background

* Daily **WorkManager** job computes the due queue and shows a local notification.
* Tap notification → deep-link to **Review**.
* Android 13+ runtime permission: app requests **POST\_NOTIFICATIONS** on first run.
* You can reschedule the worker by changing **Settings → Notify hour** (24h).

---

## 🧭 Key flows

### Add word

1. Tap **Add** (or share text into the app).
2. Type term, tags (comma-sep), optional notes.
3. Duplicate detection runs as you type; open existing if found.
4. Save → go to Word Details.

### Word details

* Shows language, tags, notes, meanings with POS/IPA, examples.
* **Audio chips** play phonetics (if present).
* **Refresh**:

    * Fetches definitions/phonetics; caches to Room.
    * (If translations enabled) fetches headword translation for the selected language.
* **Delete**:

    * Confirmation dialog; deletes word + senses + translations + srs + results.

### Review session

* **Start** → builds today’s queue (due items first, then new words).
* Cycles through MCQ / Type / Cloze (based on Settings).
* Grades mapped to SM-2; stores SRS state + quiz result.
* **Summary** at the end (score + time).

---

## 🧰 Troubleshooting

* **“No translation cached” everywhere**
  By design if `ENABLE_TRANSLATION=false`. To re-enable:

    * Set `ENABLE_TRANSLATION=true`
    * For real API: set `USE_MOCK=false`, `TRANSLATE_BASE_URL="https://libretranslate.de"` (or your server)
    * Tap **Refresh** in Word Details (force replace supported)

* **NetworkOnMainThreadException**
  All clients use `Dispatchers.IO`. If you added custom calls, ensure you wrap blocking `execute()` with `withContext(Dispatchers.IO)`.

* **Got HTML instead of JSON from translate API**
  You’re hitting a website root, not the JSON endpoint. Use a LibreTranslate **API base** (no trailing slash) and POST to `/translate`.

* **Material tokens error in Settings**
  Remove imports from `androidx.compose.material3.tokens.*`; don’t access `.value` on `MaterialTheme` objects.

* **Long details overflow**
  Details screen uses `verticalScroll(rememberScrollState())`.

---

## 🎨 App icon

* Adaptive icon configured in `res/mipmap-anydpi-v26/ic_launcher.xml` & `ic_launcher_round.xml`.
* PNG foreground/background (and optional monochrome) placed in `mipmap-*/`.
* Manifest points to `@mipmap/ic_launcher` (and round variant).
* Notification uses a separate single-color vector (e.g., `ic_stat_keephub`).

---

## 🔐 Permissions

* `INTERNET` (optional—only needed for real network)
* `POST_NOTIFICATIONS` (runtime on Android 13+)

---

## 📦 Build & install

* Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
* Bundle (release): `./gradlew bundleRelease` (configure signing in `gradle.properties` or Android Studio)

---

## 🗺️ Roadmap

* Step 8: **Analytics & Stats** (streaks, accuracy by tag/POS, daily goal progress)
* Step 9: **Import/Export** (JSON/CSV) + cloud backup guidance
* Optional: audio playback improvements, richer distractors, bulk add, pronunciation caching per locale

---

## 🤝 Contributing

PRs welcome! Please:

1. Keep modules clean (`:core-data` for data/DI/services; `:app` for UI/WorkManager).
2. Add/extend tests for scheduler/repo changes.
3. Avoid breaking Room migrations; bump DB `version` and provide migrations.

---

## 📄 License
```
MIT License — © 2025 Bhishan Pangeni
```

---

## 🙌 Credits

* Dictionary data: **Free Dictionary API**
* (Optional) Translations: **LibreTranslate** (self-hosted or public instance)

---

### Quick start TL;DR

```bash
# clone & build
./gradlew assembleDebug

# run unit tests
./gradlew :core-data:test

# (optional) enable real dictionary/translate
# edit :core-data/build.gradle.kts:
# USE_MOCK=false, ENABLE_TRANSLATION=true, TRANSLATE_BASE_URL="https://libretranslate.de"

# install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Happy building!
