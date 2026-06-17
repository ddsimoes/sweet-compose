plugins {
    id("kotlin")
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

// Public-ABI guard (WS-3): locks Sweet's OWN public API (`io.github.ddsimoes.sweet.*`) so
// accidental binary-breaking changes fail the build. The `androidx.compose.*` shim packages are
// intentionally divergent mirrors of upstream Compose, so they are excluded from the dump.
apiValidation {
    ignoredPackages.add("androidx.compose")
}

dependencies {
    api("org.jetbrains.compose.runtime:runtime:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    api("org.eclipse.platform:org.eclipse.swt.gtk.linux.x86_64:3.108.0")
    implementation(project(":kotlinx-coroutines-swt"))
    implementation("androidx.annotation:annotation-jvm:1.9.1")

    // Note: AutoSWT testing framework moved to separate auto-swt repository
    testImplementation("io.github.ddsimoes:autoswt:0.1.2")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")

    // Kotlin test
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Coroutines test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    useJUnitPlatform()

    jvmArgs("-Xmx1g")

    testLogging {
        // events("passed", "skipped", "failed")
        // exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        outputs.upToDateWhen { false }
        showStandardStreams = true
    }
}

allprojects {
    group = "io.github.ddsimoes.sweet"
    version = "0.1.0"
}

kotlin {
    jvmToolchain(11)
}
configurations.all {
    resolutionStrategy {
        dependencySubstitution {
            // The maven property ${osgi.platform} is not handled by Gradle
            // so we replace the dependency, using the osgi platform from the project settings
            val os = System.getProperty("os.name").toLowerCase()
            when {
                os.contains("windows") -> {
                    substitute(module("org.eclipse.platform:org.eclipse.swt.\${osgi.platform}"))
                        .using(module("org.eclipse.platform:org.eclipse.swt.win32.win32.x86_64:3.108.0"))
                }
                os.contains("linux") -> {
                    substitute(module("org.eclipse.platform:org.eclipse.swt.\${osgi.platform}"))
                        .using(module("org.eclipse.platform:org.eclipse.swt.gtk.linux.x86_64:3.108.0"))
                }
                os.contains("mac") -> {
                    substitute(module("org.eclipse.platform:org.eclipse.swt.\${osgi.platform}"))
                        .using(module("org.eclipse.platform:org.eclipse.swt.cocoa.macosx.x86_64:3.108.0"))
                }
            }
        }
    }
}

/**
 * Verifies the vendored upstream is checked out at the pinned revision.
 *
 * **This is NOT an API-shape check and NOT a parity check.** It only confirms the
 * reproducibility prerequisites for a future BCV-based signature diff (WS-3 A2):
 * the vendored `3rdparty/compose-multiplatform-core` exists and matches the SHA in
 * `3rdparty/COMPOSE_REVISION.txt`, and the mirror-scope allowlist is present.
 * Behavioral parity is enforced by `SharedSampleParityTest` in `:sample-sweet`.
 *
 * Run locally:
 *   ./gradlew :sweet:verifyUpstreamPin
 */
tasks.register("verifyUpstreamPin") {
    group = "verification"
    description = "Verify vendored upstream pin (NOT a shape/parity check; BCV diff planned)"

    doLast {
        val upstreamDir = rootProject.file("3rdparty/compose-multiplatform-core")
        val revFile = rootProject.file("3rdparty/COMPOSE_REVISION.txt")
        val allowlistFile = rootProject.file("config/api-shape-allowlist.txt")

        check(upstreamDir.isDirectory) {
            "Vendored upstream not found at ${upstreamDir.relativeTo(rootProject.rootDir)}. " +
            "Run: bash 3rdparty/fetch-upstream.sh"
        }
        check(revFile.isFile) {
            "Pinned revision file not found at ${revFile.relativeTo(rootProject.rootDir)}"
        }
        check(allowlistFile.isFile) {
            "Allowlist not found at ${allowlistFile.relativeTo(rootProject.rootDir)}"
        }

        val pinnedRev = revFile.readLines()
            .filter { !it.startsWith("#") }
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: error("No pinned revision in ${revFile.relativeTo(rootProject.rootDir)}")

        val actualHead = ProcessBuilder("git", "rev-parse", "HEAD")
            .directory(upstreamDir)
            .redirectErrorStream(true)
            .start()
            .let { p -> p.inputStream.bufferedReader().readText().trim().also { p.waitFor() } }

        check(actualHead.startsWith(pinnedRev)) {
            "Vendored upstream is at $actualHead, expected $pinnedRev. " +
            "Run: bash 3rdparty/fetch-upstream.sh"
        }

        val packages = allowlistFile.readLines()
            .filter { !it.startsWith("#") && it.isNotBlank() }
            .map { it.trim() }

        logger.lifecycle("=== Upstream pin verification ===")
        logger.lifecycle("Pinned at: $pinnedRev (verified)")
        logger.lifecycle("Mirror-scope allowlist: ${packages.size} packages (for future BCV diff)")
        logger.lifecycle("")
        logger.lifecycle("NOTE: This task verifies the pin only. It is NOT a signature/shape check.")
        logger.lifecycle("For BEHAVIORAL parity, run:")
        logger.lifecycle("  xvfb-run -a ./gradlew :sample-sweet:test --tests \"SharedSampleParityTest\"")
    }
}
