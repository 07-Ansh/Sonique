plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }

    dependencies {
        implementation(projects.common)
        implementation(projects.domain)
        implementation(projects.mediaJvm)
        // UI
        implementation(libs.compose.ui)
        implementation(libs.compose.material3)

        implementation(libs.coil.compose)
        implementation(libs.coil.network.okhttp)

        implementation(platform(libs.koin.bom))
        implementation(libs.koin.jvm)
        implementation(libs.koin.compose)

        // Gstreamer
        implementation(libs.gst1.java.core)
        implementation(libs.gst1.java.fx)
        implementation(libs.gst1.java.swing)

        implementation(libs.jna)
        implementation(libs.jna.platform)
        implementation(libs.kotlinx.coroutinesSwing)
    }
}