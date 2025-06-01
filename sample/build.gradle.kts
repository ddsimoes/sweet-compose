import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    application
}

dependencies {
    implementation(project(":sweet"))
}

application {
    mainClass.set("io.github.ddsimoes.sweet.CounterExampleWithDebugKt")
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