// Verification build. Runs everything that does not need the Android SDK:
// the core engine and data tests, and the shared Compose UI compiled for the JVM desktop
// so it can be rendered headlessly and driven by UI tests.
// The Android build lives in the repository root (see ../../settings.gradle.kts).
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    // Some Kotlin plugin markers are only on the Gradle Plugin Portal; point them at the
    // implementation artifacts on Maven Central so this build works with Maven Central alone.
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.plugin.serialization" ->
                    useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
                "org.jetbrains.kotlin.plugin.compose" ->
                    useModule("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") { from(files("../../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "ucs-gpa-verify"

include(":core", ":data")
project(":core").projectDir = file("../../core")
project(":data").projectDir = file("../../data")

// The shared Compose UI from ../../app/src/main/kotlin/ucs/gpa/ui, compiled for the JVM desktop.
include(":ui-desktop")
