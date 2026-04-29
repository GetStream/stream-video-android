plugins {
    `kotlin-dsl`
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-api:1.23.6")
}

gradlePlugin {
    plugins {
        create("GenerateRPCServicePlugin") {
            id = "io.getstream.video.generateServices"
            implementationClass = "io.getstream.video.android.commands.rpc.plugin.GenerateRPCServicePlugin"
            version = "1.0.0"
        }
    }
}

repositories {
    mavenCentral()
}