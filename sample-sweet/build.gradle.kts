plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
}

dependencies {
    implementation(project(":sweet"))

    // AutoSWT testing framework
    testImplementation("io.github.ddsimoes:autoswt:0.1.2")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")

    // Kotlin test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Coroutines test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

kotlin {
    jvmToolchain(11)
}

tasks.test {
    useJUnitPlatform()
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
