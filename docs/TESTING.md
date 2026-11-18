# Testing

80 automated tests, all passing at the time of writing.

| Suite | Where | Tests | Covers |
|---|---|---|---|
| Engine | `core/src/test` | 55 | Grade values, 30/15 credits, full vs half weighting, worked year and four-year examples checked by hand, fails, S, ungraded, temporary grades (both policies), excluded and 0-credit courses, custom and invalid credits, unknown grades, repeats (both policies), duplicates, cumulative across different credit totals, class boundaries (exact and rounding policies), near-boundary display, required average, secured / secured-if-passed / achievable / out-of-reach / met / missed / no data, estimates, custom targets, combinations always reach the target, what-if with partial and complete grades, insights thresholds, input validation |
| Data | `data/src/test` | 12 | Demo record values, first launch, persistence across restarts (real files), demo and personal kept separate, hypothetical grades cleared on grading, demo reset, course list added once, backup export/import/validation, unreadable file kept aside, newer-version data not overwritten, forward-compatible decoding, atomic saves |
| UI flows | `tools/desktop-preview/ui-desktop/src/test/.../UiFlowTest.kt` | 7 | Welcome → demo; all tabs and back navigation; editing a grade updates the year GPA and is saved and reloaded; invalid course input rejected with reasons; planner combination, "A in full courses, B+ in half courses" what-if and target change; creating an own record keeps the demo separate; missing information is flagged |
| Screenshots | `.../ScreenshotTest.kt` | 6 groups, 29 images | Every main screen in light and dark, at 360dp, 393dp and tablet (800dp) widths, plus empty, error and out-of-reach states |

## How the UI is tested

The UI code in `app/src/main/kotlin/ucs/gpa/ui` uses no Android APIs, so the verification build
compiles exactly those files for the JVM desktop (Compose Multiplatform) and drives them with the
Compose UI test API. Screenshots are rendered by Skia, the same engine Android uses, with the app's
real fonts and logo. They are written to `tools/desktop-preview/ui-desktop/build/screenshots`.

## Running

```bash
# Engine, data and APK (needs the Android SDK, e.g. via Android Studio)
./gradlew :core:test :data:test :app:assembleDebug

# UI tests and screenshots (no Android SDK needed)
./gradlew -p tools/desktop-preview :ui-desktop:test
```

The GitHub Actions workflow (`.github/workflows/android.yml`) runs all of these and uploads the APK
and screenshots.

## Not yet tested

Be aware of these before a public release:

- **The Android build itself was not compiled in the environment where this version was written**:
  Google's Android SDK and Maven servers were not reachable there. The Android-specific code is
  limited to `app/src/main/kotlin/ucs/gpa/android` (two files), the manifest and resources. The
  first CI run or Android Studio build is the check; fix anything it reports before distributing.
- No emulator or physical-device run yet: system back gesture, file picker for backups, edge-to-edge
  insets, the keyboard over text fields, and the launcher icon on different launchers.
- TalkBack walkthrough (semantics are set on key elements, but not tested with a screen reader).
- Very small screens below 360dp and font scaling above 130%.
