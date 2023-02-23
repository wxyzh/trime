/**
 * The buildscript block is where you configure the repositories and
 * dependencies for Gradle itself--meaning, you should not include dependencies
 * for your modules here. For example, this block includes the Android plugin for
 * Gradle as a dependency because it provides the additional instructions Gradle
 * needs to build Android app modules.
 */

plugins {
    id("com.android.application") version "7.4.0" apply false
    id("com.android.library") version "7.4.0" apply false
    kotlin("android") version "${Extra.kotlinVersion}" apply false
    id("com.diffplug.spotless") version "6.3.0"
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        target("app/src/main/java/com/osfans/trime/**/*.java")
        googleJavaFormat("1.15.0")
    }
    kotlin {
        target("app/src/main/java/com/osfans/trime/**/*.kt")
        ktlint("0.44.0")
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
}
