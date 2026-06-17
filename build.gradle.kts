buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
    }

    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    // id("com.vanniktech.maven.publish") version "0.19.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.16.3" apply false
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven {
            url = uri("https://maven.pkg.github.com/ddsimoes/auto-swt")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }

    // Help Gradle choose the correct kotlin-logging variant used by ktlint reporters
    configurations.matching { it.name.endsWith("ktlintReporter") }.all {
        attributes.attribute(
            org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute,
            org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm,
        )
    }
}

subprojects {
    // Apply ktlint to all Kotlin subprojects
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // Disable the buggy argument-list-wrapping rule for now
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension>("ktlint") {
            disabledRules.set(
                setOf(
                    "standard:argument-list-wrapping",
                    "standard:no-wildcard-imports",
                    "standard:property-naming",
                    "standard:no-consecutive-comments",
                ),
            )
            // Keep ktlint focused on production code; sample code is excluded
            // and a few generated/ported files are skipped where ktlint has parser issues.
            filter {
                when (project.name) {
                    "sample", "sample-sweet", "sample-jetbrains" -> {
                        exclude("**/*.kt")
                    }
                    "sweet" -> {
                        exclude("**/androidx/compose/foundation/layout/layout.kt")
                        exclude("**/androidx/compose/foundation/layout/PaddingValues.kt")
                        exclude("**/TabClickTest.kt")
                        exclude("**/androidx/compose/foundation/shape/shape.kt")
                        exclude("**/TabInteractiveTest.kt")
                        exclude { it.file.startsWith(project.projectDir.resolve("src/main/kotlin/androidx/compose/animation/core").toString()) }
                    }
                }
            }
        }
    }

    // Apply detekt to all Kotlin subprojects
    apply(plugin = "io.gitlab.arturbosch.detekt")

    plugins.withId("io.gitlab.arturbosch.detekt") {
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            // Per-module baseline so each module manages its own suppressed issues
            baseline = project.file("detekt-baseline.xml")

            source.from(
                files(
                    project.projectDir.resolve("src/main/kotlin"),
                    project.projectDir.resolve("src/test/kotlin"),
                ).filter { it.exists() },
            )
        }
    }
}

/**
 * Repeats the full SWT test suite N times (default 5; `-PrepeatTests=N`) to surface
 * run-to-run flakiness. Each iteration shells out to the gradle wrapper under xvfb:
 *
 *   xvfb-run -a ./gradlew cleanTest test --continue
 *
 * Non-zero exits are counted; if any run fails the task fails with a pointer to
 * `docs/roadmap/test-determinism.md`. Slow — runs the whole suite N times.
 */
tasks.register("repeatTests") {
    group = "verification"
    description =
        "Run the full SWT test suite N times (default 5; -PrepeatTests=N) to surface run-to-run flakiness. Slow."

    doLast {
        val times = (project.findProperty("repeatTests") as? String)?.toIntOrNull() ?: 5
        require(times >= 1) { "repeatTests must be >= 1, got $times" }
        logger.lifecycle("repeatTests: running the full suite $times times under xvfb (see docs/roadmap/test-determinism.md)")

        var failures = 0
        for (i in 1..times) {
            logger.lifecycle("repeatTests: run $i of $times")
            val result = project.exec {
                commandLine("xvfb-run", "-a", "./gradlew", "cleanTest", "test", "--continue")
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) {
                logger.lifecycle("repeatTests: run $i passed")
            } else {
                failures++
                logger.lifecycle("repeatTests: run $i FAILED (exit ${result.exitValue})")
            }
        }

        check(failures == 0) {
            "$failures of $times runs failed — suite is flaky (see docs/roadmap/test-determinism.md)"
        }
        logger.lifecycle("repeatTests: all $times runs passed")
    }
}
