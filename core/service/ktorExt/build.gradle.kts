import com.android.build.gradle.internal.tasks.CompileArtProfileTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    androidLibrary {
        namespace = "com.sonique.ktorext"
        compileSdk = 36
        minSdk = 26
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here
                api(libs.ktor.client.core)
                api(libs.ktor.client.cio)
                implementation(libs.ktor.client.encoding)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.brotli.dec)
            }
        }
    }
}

tasks.withType<CompileArtProfileTask> {
    enabled = false
}