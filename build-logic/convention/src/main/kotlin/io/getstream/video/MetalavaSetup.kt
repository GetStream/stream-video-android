package io.getstream.video

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Configures the Metalava plugin for API signature generation and compatibility tracking.
 *
 * @param hiddenAnnotations Annotations whose marked APIs are excluded from the signature.
 * @param extraArguments Additional metalava CLI arguments (use `--flag=value` format).
 */
fun Project.configureMetalava(
    hiddenAnnotations: Set<String> = emptySet(),
    extraArguments: Set<String> = emptySet(),
) {
    val metalavaEnabled = providers.gradleProperty("metalava.enabled")
        .getOrElse("true").toBoolean()

    val metalava = extensions.getByName("metalava")

    @Suppress("UNCHECKED_CAST")
    (metalava.javaClass.getMethod("getFilename").invoke(metalava) as Property<String>)
        .set("api/current.txt")

    if (hiddenAnnotations.isNotEmpty()) {
        @Suppress("UNCHECKED_CAST")
        (metalava.javaClass.getMethod("getHiddenAnnotations").invoke(metalava) as SetProperty<String>)
            .set(hiddenAnnotations)
    }

    val defaultArgs = setOf(
        "--hide=ReferencesHidden",
        "--hide=HiddenTypeParameter",
        "--hide=UnavailableSymbol",
        "--hide=IoError",
    )
    @Suppress("UNCHECKED_CAST")
    (metalava.javaClass.getMethod("getArguments").invoke(metalava) as SetProperty<String>)
        .set(defaultArgs + extraArguments)

    afterEvaluate {
        tasks.matching { it.name.startsWith("metalava") }.configureEach {
            setEnabled(metalavaEnabled)
        }
        tasks.named("apiDump") {
            if (metalavaEnabled) {
                finalizedBy("metalavaGenerateSignatureRelease")
            }
        }
    }
}
