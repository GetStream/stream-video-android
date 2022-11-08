plugins {
    `kotlin-dsl`
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