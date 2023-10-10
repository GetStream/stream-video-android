package io.getstream.video

/**
 * This is shared between :dogfooding and :benchmarks module to provide configurations type safety.
 */
enum class DogfoodingBuildType(val applicationIdSuffix: String? = null) {
    DEBUG(".debug"),
    RELEASE,
    BENCHMARK(".benchmark")
}