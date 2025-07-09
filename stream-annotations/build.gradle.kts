plugins {
    id("java-library")
    alias(libs.plugins.ksp)
    kotlin("jvm")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.google.symbolprocessingapi)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.kotlin.reflect)
}