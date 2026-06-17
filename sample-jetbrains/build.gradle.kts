plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    application
}

dependencies {
    // JetBrains Compose Desktop runtime (includes org.jetbrains.compose.ui/runtime/foundation, etc.)
    implementation("org.jetbrains.compose.desktop:desktop-jvm:1.8.1")
    // JetBrains fork of Material3; classes are in androidx.compose.material3.*
    implementation("org.jetbrains.compose.material3:material3:1.8.1")

    // Ensure Skiko native runtime is available for the current OS.
    val os = System.getProperty("os.name").toLowerCase()
    val skikoRuntime =
        when {
            os.contains("linux") -> "org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.9.4.2"
            os.contains("windows") -> "org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.9.4.2"
            os.contains("mac") -> "org.jetbrains.skiko:skiko-awt-runtime-macos-x64:0.9.4.2"
            else -> null
        }
    if (skikoRuntime != null) {
        implementation(skikoRuntime)
    }
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("io.github.ddsimoes.sweet.sample.JetbrainsRunnersKt")
}
