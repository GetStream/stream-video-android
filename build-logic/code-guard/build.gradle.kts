plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(kotlin("compiler-embeddable"))
}
kotlin {
    jvmToolchain(17)
}