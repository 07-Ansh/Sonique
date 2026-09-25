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
    androidLibrary {
        namespace = "com.sonique.data"
        compileSdk = 36
        minSdk = 26
    }

    room {
        schemaDirectory("$projectDir/schemas")
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
                implementation(projects.listenTogether)

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
                implementation(libs.koin.android)
                implementation(projects.media3)
                implementation(libs.room.ktx)
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
}

tasks.withType<CompileArtProfileTask> {
    enabled = false
}