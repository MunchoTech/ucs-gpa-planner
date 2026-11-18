import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    // Compile exactly the UI sources the Android app uses (no copies).
    sourceSets["main"].kotlin.srcDir("../../../app/src/main/kotlin/ucs/gpa/ui")
}

// Compose Multiplatform 1.6.2 desktop asks for androidx.collection / androidx.annotation, which are
// published only on Google Maven (unreachable here). JetBrains publishes a desktop build of the same
// collection classes on Maven Central; use it, and drop the annotation-only artifact.
configurations.configureEach {
    exclude(group = "androidx.annotation")
    exclude(group = "androidx.collection")
}

dependencies {
    runtimeOnly("org.jetbrains.compose.collection-internal:collection-desktop:1.6.0-beta02") { isTransitive = false }
    implementation(project(":data"))
    implementation(libs.desktop.runtime)
    implementation(libs.desktop.ui)
    implementation(libs.desktop.foundation)
    implementation(libs.desktop.material3)
    implementation(libs.desktop.icons.extended)
    implementation(libs.desktop.jvm.linux)
    implementation(libs.kotlinx.coroutines.swing)

    testImplementation(libs.desktop.ui.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}

tasks.test {
    // Where screenshots are written, and the app resources (fonts, logo) the tests load.
    systemProperty("ucs.screenshots", layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
    systemProperty("ucs.res", rootDir.resolve("../../app/src/main/res").canonicalPath)
    systemProperty("java.awt.headless", "true")
    maxHeapSize = "1536m"
    forkEvery = 1
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}
