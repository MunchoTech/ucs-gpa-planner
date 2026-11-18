# Architecture

UCS GPA Planner is a native Android app written in Kotlin with Jetpack Compose. It works fully
offline, has no backend and requests no permissions.

## Modules

```
core/   Pure Kotlin. Academic rules, GPA calculator, classifier, target planner,
        grade combinations, what-if projections, insights, input validation, formatting.
        No Android, no UI, no I/O.

data/   Pure Kotlin. Stored state, JSON codec, atomic file store, backup format,
        fictional demo student, Computer Science course list, and AppStore
        (the single source of truth for the UI).

app/    Android application.
        ucs/gpa/android/  Activity, ViewModel, file storage location, backup
                          export/import through the system file picker.
        ucs/gpa/ui/       All screens and components (Compose). Uses no Android
                          APIs, so the same code also compiles for the JVM desktop.

tools/desktop-preview/
        A separate Gradle build that compiles core, data and app/src/main/kotlin/ucs/gpa/ui
        for the JVM desktop, renders every screen to PNG and runs UI tests. It needs no
        Android SDK.
```

Dependencies point one way: `app → data → core`. The engine can be reused by a future web or iOS
version, or by a UCS server, without change.

## Data flow

```
User action ──► AppStore action (e.g. saveCourse)
                   │
                   ├─► new StoredState (immutable)
                   ├─► Derived.of(record)   ← core: calculator, planner, insights
                   ├─► state: StateFlow<AppState>  ──► Compose screens recompose
                   └─► save queue (conflated) ──► RecordStore.save (background)
```

- **State.** `AppStore` holds an immutable `StoredState` and exposes `StateFlow<AppState>`.
  Every change recomputes `Derived` (the record's results, target analysis, combinations,
  projection and insights). The calculations are cheap, so there is no caching layer to go stale.
- **Screens** read `AppState.Ready` and call store actions. They hold only local form state.
- **Navigation** is a small typed back stack (`ui/nav/Navigator.kt`): four tabs (Home, Calculator,
  Planner, Insights) and pushed screens (Settings, Profile, Methodology, Setup, course editor,
  calculation breakdown). The Android activity forwards the system back gesture. Keeping it in plain
  Kotlin state lets the same navigation run in the JVM UI tests.

## Persistence and privacy

- One JSON file in the app's private storage (`filesDir/records/state.json`). Other apps and other
  users of the phone cannot read it.
- Writes are atomic (temp file, then rename). A file that cannot be read is kept aside, never
  deleted, and the student is told. Data written by a newer app version is not overwritten.
- The demo record and the student's own record are stored separately; the demo can be reset
  without touching real results.
- `android:allowBackup="false"`: records are not copied to cloud backups. Students move or keep
  their data through explicit backup files (Settings → Export a backup), which use the system file
  picker and need no storage permission.
- Backups are validated on import (format, version, sizes, year range) and get fresh ids.

## Extending

- **Cloud sync or accounts.** Implement `RecordStore` (load/save `StoredState`) against a server
  and pass it to `AppStore`. Records already have stable ids and a schema version. Nothing in the
  engine or UI changes.
- **Other programmes.** Add course lists to `data/Catalogue.kt`, or load them from a file.
- **Rule changes.** Edit `UnzaRules.kt`; the Methodology screen lists the rules from there.
- **Translations.** UI strings are in the Compose code; moving them to a strings table is the next
  step if UCS wants other languages.

## Design system

Tokens are in `ui/theme/Theme.kt`: a warm-neutral canvas, a charcoal-navy hero surface, UCS teal
(from the logo) for brand and primary actions, and UCS orange used only for "you are here"
markers and hypothetical grades. Light and dark palettes are defined side by side. Type is Geist
(SIL Open Font License, bundled) with Geist Mono for codes and figures. Cards use 12dp corners and
hairline borders, no shadows. Screen titles stay at 24sp.

## Versions

Android: AGP 8.10.1, Kotlin 2.1.21, Compose BOM 2025.05.00, min SDK 26, target SDK 35.
The desktop harness pins Compose Multiplatform 1.6.2 because later desktop releases depend on
AndroidX artifacts published only on Google Maven, which the build environment used to produce this
version could not reach. The UI code compiles against both.
