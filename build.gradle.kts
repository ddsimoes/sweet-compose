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
    id("com.vanniktech.maven.publish") version "0.19.0"
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}