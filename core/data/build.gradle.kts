@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.android.build.gradle.internal.tasks.CompileArtProfileTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
        freeCompilerArgs.add("-Xwhen-guards")
    }
    jvmToolchain(21)
    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.sonique.data"
        compileSdk = 36
        minSdk = 26
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "dataKit"

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = xcfName
            isStatic = true
            // Required when using NativeSQLiteDriver
            linkerOpts.add("-lsqlite3")
        }
    }

    jvm {
    }

    dependencies {
        implementation(platform(libs.koin.bom))
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.common)
                implementation(projects.domain)
                implementation(projects.lyricsService)
                implementation(projects.spotify)
                implementation(projects.kotlinYtmusicScraper)

                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here
                // Kotlinx serialization
                implementation(libs.kotlinx.serialization.json)

                // DataStore
                implementation(libs.datastore.preferences)

                // Room
                implementation(libs.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.androidx.room.migration)

                // Ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)

                // Koin
                implementation(libs.koin.core)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
                implementation(libs.koin.android)
                implementation(projects.media3)
                implementation(libs.room.ktx)
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}

tasks.withType<CompileArtProfileTask> {
    enabled = false
}