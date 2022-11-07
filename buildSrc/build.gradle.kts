plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    // google()
    mavenCentral()
    // maven { setUrl("https://jitpack.io")  }
}
//
// buildscript {
//     repositories {
//         google()
//         mavenCentral()
//         maven { setUrl("https://jitpack.io")  }
//     }
//
//     dependencies {
//         classpath("com.android.tools.build:gradle:7.3.0")
//     }
// }

gradlePlugin {
    plugins {
        create("GenerateRPCServicePlugin") {
            id = "io.getstream.video.generateServices"
            implementationClass = "io.getstream.video.android.commands.rpc.plugin.GenerateRPCServicePlugin"
            version = "1.0.0"
        }
    }
}

// dependencies {
//     implementation("com.android.tools.build:gradle:7.3.0")
// }