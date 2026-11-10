# KeepHub — Android Vocabulary & Spaced Repetition App

KeepHub helps English-proficiency exam takers and lifelong language learners **grow, retain, and master vocabulary**. Add words quickly, explore rich context-aware definitions, listen to phonetics audio, and retain words forever using **scientific spaced repetition (SM-2)**, **daily smart reminders**, and **Google Gemini AI integration (BYOK)**.

The app is **100% offline-first**: all core functionalities, local Room caching, and spaced repetition quizzes run flawlessly with or without an internet connection or AI key.

---

## ✨ Features

### 🧠 Google Gemini AI Integration (Bring Your Own Key)
* **Zero-Cost BYOK Architecture**: Add your free Google AI Studio key (`aistudio.google.com`) directly in Settings.
* **Current Flash Endpoints with Auto-Discovery**: Targets Google's recommended models (`gemini-3.5-flash-lite`, `gemini-3.8-flash`, `gemini-3.6-flash`, `gemini-flash-latest`) with automatic fallback.
* **3-Tier Offline-First Fallback**:
  * **Tier 1**: Google Gemini AI (rich context-aware definitions, natural examples, mnemonics).
  * **Tier 2**: Free Dictionary API (definitions, phonetics audio, synonyms/antonyms).
  * **Tier 3**: Local Room SQLite cache (instant, zero network required).
* **Context-Aware Enrichment**: When adding a word from a reading passage or sentence, Gemini prioritizes the specific sense intended in that context.
* **AI Memory Anchors**: Generates high-retention 1-sentence mnemonics or etymology anchors displayed in Word Details.
* **"Explain My Mistake" Interactive Tutor**: Tap on any incorrect quiz card to get a friendly 2–3 sentence explanation comparing your answer with the target word.
* **Smart Semantic & Typo Grading**: Accepts legitimate synonyms and tolerates minor typos (Levenshtein distance + semantic AI grading) during Type and Cloze quizzes.
* **AI Vocabulary Micro-Stories**: Generates a 120-word cohesive story naturally featuring your reviewed words, with target vocabulary highlighted in bold and primary theme color.

### 📝 Vocabulary Management & Dictionary
* **Fast Word Addition**: Manual entry or 1-tap **Android Share Sheet / Process Text** prefill from any browser or reading app.
* **Duplicate Detection**: Normalizes diacritics, case, and basic inflection to prevent duplicate entries while offering 1-tap notes/tags merging.
* **Word Details**: Parts of speech, IPA phonetic transcriptions, definitions, natural examples, synonyms/antonyms.
* **Pronunciation Audio**: High-fidelity phonetic audio playback using Android `MediaPlayer` and system TTS fallback.
* **Tags & Personal Notes**: Organize words by exam (e.g. *GRE*, *TOEFL*, *IELTS*) or custom categories.

### 🎓 Daily 5 Words Leveled Learning
* **Curated 3-Tier Vocabulary Levels**:
  * **Beginner (A1 - A2)**: Essential everyday foundational words for daily fluency (*achieve, ancient, curious, diligent, generous, sincere...*).
  * **Intermediate (B1 - B2)**: High-frequency academic and professional vocabulary (*abundant, articulate, coherent, deliberate, inevitable, resilient...*).
  * **Advanced (C1 - C2 / GRE)**: High-impact literary, competitive exam, and GRE words (*aberration, capricious, ephemeral, gregarious, meticulous, ubiquitous...*).
* **Interactive Step-by-Step Teaching**:
  * Large headword display with IPA phonetic transcription.
  * 1-tap TTS native pronunciation audio button.
  * Clear definition, real-world context sentence, and high-retention memory anchor (mnemonic).
* **Frictionless Auto-Enrollment (Default YES)**:
  * Each day's 5 words are enrolled into the user's vocabulary list by default, ensuring continuous streak and vocabulary growth even if the user misses a day or forgets to choose.
  * Fully customizable: users can easily uncheck/skip any word they already know.
  * Daily background sync: `DueReviewWorker` and app launch automatically auto-enroll the day's 5 words.
  * Level selector: switch between Beginner, Intermediate, and Advanced anytime in Settings or directly from the learning screen.

### 🔁 Spaced Repetition System (SRS)
* **SuperMemo SM-2 Algorithm**: Dynamically calculates `easiness`, `intervalDays`, `repetitions`, `lapses`, and `dueDate`.
* **Daily Review Queue**: Automatically queues due cards and fills up to your configured daily goal with new words.
* **4 Interactive Quiz Modes**:
  1. **Multiple Choice (MCQ)**: Smart contextual distractors selected from matching parts of speech.
  2. **Typing**: Spell out the headword from memory with smart letter clues and semantic grading.
  3. **Cloze (Fill-in-the-blank)**: Deduce and type the missing word from a natural example sentence.
  4. **Audio**: Listen to the native pronunciation and match the correct meaning.
* **Session Summary**: Visual review score, duration, accuracy breakdown, and 1-tap AI Story generation.
* **Review History (Recent 10 Days / Limited Sessions)**:
  * Automatically records finished Daily Review and Practice sessions with submitted answers, correct targets, semantic hints, and AI explanations.
  * Prunes history automatically (retains the most recent 10 sessions / 10 days) to prevent database bloat.
  * Interactive Session Inspector: tap any past session to review every question card, compare your answer with the target, and read saved feedback.
  * On-Demand History AI Stories & Tutor Explanations: generate a vocabulary story or request AI mistake explanations for past sessions anytime.

### 🔔 Daily Reminders & Background Work
* **WorkManager Scheduled Worker**: Checks due cards and schedules notifications at your configured reminder hour.
* **Streak Protection**: Reschedules daily reminders safely with `ExistingWorkPolicy.KEEP` so app opens never clobber active alarms.
* **Instant In-App Test**: "Send Test" button in Settings lets you preview notifications and verify Android notification channels instantly.
* **Deep Linking**: Tapping the notification opens today's Review session immediately.

### 🎨 Material Design 3 (M3) UI & Polish
* **Dynamic Theming & Dark Mode**: Full M3 color system support with system-matched dynamic palettes.
* **Tactile Steppers & Quick Presets**: Increment/decrement buttons (`[-]` / `[+]`) and 1-tap chips for Daily Goals (`5`, `10`, `20`, `30`, `50`) and Reminder Times (`Morning 08:00`, `Noon 12:00`, `Evening 18:00`, `Night 21:00`).
* **Adaptive Keyboard Handling**: Comprehensive `Modifier.imePadding()` and `BringIntoViewRequester` integration ensures text editors and action buttons are never obscured by the software keyboard.
* **Native Markdown Formatting**: In-app `MarkdownText` engine strips raw `**` delimiters and renders bold spans with theme accents.

---

## 🧱 Architecture & Tech Stack

```
KeepHub/
├─ app/                         # Presentation Layer & Background Tasks
│  ├─ src/main/java/com/keephub/app/
│  │  ├─ MainActivity.kt        # Edge-to-edge entry point + notification permissions
│  │  ├─ KeepHubApp.kt          # Hilt Application container + initial schedule
│  │  ├─ nav/NavGraph.kt        # Compose Navigation destinations
│  │  ├─ notifications/         # Notification channels & builder
│  │  ├─ work/                  # WorkManager DueReviewWorker
│  │  ├─ ui/
│  │  │  ├─ components/         # States, Shimmer placeholders, MarkdownText
│  │  │  ├─ review/             # Quiz generation, ReviewViewModel, SM-2 bridge
│  │  │  ├─ screens/            # WordList, AddWord, WordDetail, Review, Settings
│  │  │  └─ theme/              # Material 3 Color schemes, Typography, Shapes
│  │  └─ res/                   # Adaptive icons, vectors, notification drawables
│  └─ src/test/                 # Worker tests, Streak tests, Markdown tests
└─ core-data/                   # Domain & Data Layer (Pure Kotlin + Android Library)
   ├─ src/main/java/com/keephub/core/data/
   │  ├─ db/                    # Room Database, TypeConverters, Entities, DAOs
   │  ├─ di/                    # Hilt dependency injection modules
   │  ├─ net/                   # GeminiApiService (REST), FreeDictionaryApiService
   │  ├─ repo/                  # WordRepository (3-tier fallback), DictionaryService
   │  ├─ settings/              # DataStore Preferences (SettingsStore)
   │  ├─ srs/                   # SuperMemo SM-2 calculation engine
   │  └─ util/                  # WordNormalizer (Levenshtein, diacritics, inflections)
   └─ src/test/                 # Unit tests for Gemini API, Normalizer, SM-2 engine
```

* **UI Framework**: Jetpack Compose (Material 3), Navigation Compose
* **Dependency Injection**: Hilt (Dagger)
* **Database**: Room (SQLite) with schema migrations
* **Preferences**: Jetpack DataStore (Preferences)
* **Background Processing**: WorkManager
* **Networking**: OkHttp, Kotlinx Serialization (`explicitNulls = false`)
* **Target Environment**: minSdk 24, compileSdk 36, JDK 17 / 21

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio** (Koala | Ladybug or newer)
* **JDK 17** or **JDK 21**
* Android device or emulator running **Android 7.0 (API 24)** or higher

### Build & Run
```bash
# Clone the repository
git clone https://github.com/your-username/KeepHub.git
cd KeepHub

# Run all automated unit tests across :app and :core-data
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Install on connected device or emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔑 Setting Up Gemini AI (Optional)

KeepHub works 100% offline out-of-the-box using the Free Dictionary API and Room DB. To unlock AI features:

1. Obtain a free Gemini API key from [Google AI Studio](https://aistudio.google.com).
2. Open KeepHub &rarr; **Settings** &rarr; **Gemini AI (BYOK)**.
3. Paste your API key and tap **Test Key**.
4. KeepHub tests connectivity directly with active Flash endpoints (`gemini-3.5-flash-lite`, `gemini-3.8-flash`), displaying live diagnostics.
5. Tap **Save**. Your key is securely stored in local private DataStore preferences.

---

## ⚙️ Build Configuration Flags

All flags are configured in [`core-data/build.gradle.kts`](core-data/build.gradle.kts) via `buildConfigField`:

| Flag | Default | Description |
| :--- | :---: | :--- |
| `USE_MOCK` | `false` | When `true`, uses mock local dictionary responses for testing without internet. |
| `DICT_BASE_URL` | `"https://api.dictionaryapi.dev"` | Free Dictionary API base URL. |
| `ENABLE_TRANSLATION` | `false` | Toggles LibreTranslate language translation UI and backend service. |
| `TRANSLATE_BASE_URL` | `"https://libretranslate.de"` | Base endpoint for LibreTranslate server. |
| `TRANSLATE_API_KEY` | `""` | Optional authentication key for self-hosted translation servers. |

---

## 🧪 Testing

KeepHub includes over 115 unit tests covering:
* **SM-2 Spaced Repetition**: Intervals, ease-factor decay, lapse handling, and streak calculations.
* **Gemini API Service**: Structured prompt generation, markdown fence stripping, model fallback chains, error diagnostic parsing, and key sanitization.
* **Notification Scheduling**: WorkManager delay math, 2-minute safety window, self-reschedule logic, and notification builder attributes.
* **Word Normalization**: Case, diacritics, plural inflections, and Levenshtein typo tolerances.
* **Markdown Parser**: Stripping asterisks, applying bold and italic spans, and applying primary theme accents.

Run tests using:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

```
MIT License — © 2026 Bhishan Pangeni
```
