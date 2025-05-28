package io.getstream.video

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor

@Suppress("EnumEntryName")
enum class FlavorDimension {
    contentType
}

// The content for the app can either come from local static data which is useful for demo
// purposes, or from a production backend server which supplies up-to-date, real content.
// These two product flavors reflect this behaviour.
@Suppress("EnumEntryName")
enum class VideoDemoFlavor(val dimension: FlavorDimension, val applicationIdSuffix: String? = null) {
    development(FlavorDimension.contentType, applicationIdSuffix = ".dogfooding"),
    e2etesting(FlavorDimension.contentType, applicationIdSuffix = ".e2etesting"),
    production(FlavorDimension.contentType),
//    overridepush(FlavorDimension.contentType, ".dogfooding")
}

fun configureFlavors(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    flavorConfigurationBlock: ProductFlavor.(flavor: VideoDemoFlavor) -> Unit = {}
) {
    commonExtension.apply {
        flavorDimensions += "environment"
        productFlavors {
            VideoDemoFlavor.values().forEach {
                create(it.name) {
                    dimension = "environment"
                    flavorConfigurationBlock(this, it)
                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        if (it.applicationIdSuffix != null) {
                            applicationIdSuffix = it.applicationIdSuffix
                        }

//                        manifestPlaceholders["enableStreamFcmService"] =
//                            if (it == VideoDemoFlavor.overridepush) "remove" else "merge"

                    }
                    proguardFiles("benchmark-rules.pro")
                }
            }
        }
    }
}