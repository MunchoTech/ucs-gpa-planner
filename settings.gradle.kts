pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ucs-gpa-planner"

// :core  – academic rules and the GPA engine (pure Kotlin, unit tested)
// :data  – storage, backups, demo data and the app store (pure Kotlin, unit tested)
// :app   – the Android application (Jetpack Compose)
include(":core", ":data", ":app")
