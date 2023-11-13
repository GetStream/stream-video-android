/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.ResValue
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import io.getstream.video.android.Configuration
import java.io.FileInputStream
import java.util.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("io.getstream.android.application.compose")
    id("io.getstream.spotless")
    id("com.google.gms.google-services")
    id(libs.plugins.firebase.crashlytics.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
    id(libs.plugins.hilt.get().pluginId)
    id(libs.plugins.ksp.get().pluginId)
    id(libs.plugins.play.publisher.get().pluginId)
    id(libs.plugins.baseline.profile.get().pluginId)
}

android {
    namespace = "io.getstream.video.android"
    compileSdk = Configuration.compileSdk

    defaultConfig {
        applicationId = "io.getstream.video.android"
        minSdk = Configuration.minSdk
        targetSdk = Configuration.targetSdk
        versionCode = 1
        versionName = Configuration.streamVideoCallGooglePlayVersion
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val signFile: File = rootProject.file(".sign/keystore.properties")
    if (signFile.exists()) {
        val properties = Properties()
        properties.load(FileInputStream(signFile))

        signingConfigs {
            create("release") {
                keyAlias = properties["keyAlias"] as? String
                keyPassword = properties["keyPassword"] as? String
                storeFile = rootProject.file(properties["keystore"] as String)
                storePassword = properties["storePassword"] as? String
            }
        }
    } else {
        signingConfigs {
            create("release") {
                keyAlias = "androiddebugkey"
                keyPassword = "android"
                storeFile = rootProject.file(".sign/debug.keystore.jks")
                storePassword = "android"
            }
        }
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = rootProject.file(".sign/debug.keystore.jks")
            storePassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            versionNameSuffix = "-DEBUG"
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("Boolean", "BENCHMARK", "false")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("Boolean", "BENCHMARK", "false")
        }
        create("benchmark") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            proguardFiles("benchmark-rules.pro")
            buildConfigField("Boolean", "BENCHMARK", "true")
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("development") {
            dimension = "environment"
            applicationIdSuffix = ".dogfooding"
        }
        create("production") {
            dimension = "environment"
        }
    }

    buildFeatures {
        resValues = true
        buildConfig = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    baselineProfile {
        mergeIntoMain = true
    }

    playConfigs {
        val serviceAccountCredentialsFile: File = rootProject.file(".sign/service-account-credentials.json")
        if (serviceAccountCredentialsFile.exists()) {
            register("productionRelease") {
                enabled.set(true)
                serviceAccountCredentials.set(serviceAccountCredentialsFile)
                track.set("internal")
                defaultToAppBundles.set(true)
                resolutionStrategy.set(ResolutionStrategy.AUTO)
            }
        }
    }
}

play {
    enabled.set(false)
}

androidComponents {
    val envProps: File = rootProject.file(".env.properties")
    if (envProps.exists()) {
        val properties = Properties()
        properties.load(FileInputStream(envProps))

        onVariants { applicationVariant ->
            applicationVariant.flavorName?.let { flavor ->
                val keyPrefix = if (flavor == "dogfooding") "DOGFOODING" else "PRODUCTION"
                val buildConfigKeyPrefix = "${keyPrefix}_BUILD_CONFIG_"
                val resConfigKeyPrefix = "${keyPrefix}_RES_CONFIG_"
                val appProperties = properties.filterKeys { "$it".startsWith(keyPrefix) }
                appProperties
                    .filterKeys { "$it".startsWith(buildConfigKeyPrefix) }
                    .forEach {
                        val key: String = it.key.toString().replace(buildConfigKeyPrefix, "")
                        applicationVariant.buildConfigFields.put(
                            key,
                            BuildConfigField("String", "\"${it.value}\"", null),
                        )
                    }
                appProperties
                    .filterKeys { "$it".startsWith(resConfigKeyPrefix) }
                    .forEach {
                        val key: String = it.key.toString()
                            .replace(resConfigKeyPrefix, "")
                            .toLowerCase()
                        applicationVariant.resValues.put(
                            applicationVariant.makeResValueKey("string", key),
                            ResValue("${it.value}"),
                        )
                    }
            }

            applicationVariant.outputs.forEach {
                it.versionName.set(
                    it.versionCode.map { playVersionCode ->
                        "${Configuration.streamVideoCallGooglePlayVersion} ($playVersionCode)"
                    }
                )
            }
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.firebaseui:firebase-ui-auth:7.2.0")
    implementation(project(":stream-video-android-ui-compose"))
    implementation(project(":stream-video-android-ui-xml"))

    // Stream Video SDK
    compileOnly(project(":stream-video-android-previewdata"))

    // Stream Chat SDK
    implementation(libs.stream.chat.compose)
    implementation(libs.stream.chat.offline)
    implementation(libs.stream.chat.state)
    implementation(libs.stream.chat.ui.utils)

    implementation(libs.stream.push.firebase)
    implementation(libs.stream.log.android)

    implementation(libs.androidx.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)

    implementation(libs.retrofit)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.converter)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.landscapist.coil)
    implementation(libs.accompanist.permission)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)

    // QR code scanning
    implementation(libs.androidx.camera.core)
    implementation(libs.play.services.mlkit.barcode.scanning)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.camera2)

    // hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // firebase
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // moshi
    implementation(libs.moshi.kotlin)

    // Play Install Referrer library - used to extract the meeting link from demo flow after install
    implementation(libs.play.install.referrer)

    // Only used for launching a QR code scanner in demo app
    implementation(libs.play.code.scanner)

    // memory detection
    debugImplementation(libs.leakCanary)

    baselineProfile(project(":benchmark"))
}