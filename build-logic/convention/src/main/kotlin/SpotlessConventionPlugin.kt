import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.kotlin.KotlinConstants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.diffplug.spotless")

            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("**/*.kt")
                    targetExclude(
                            "**/build/**/*.kt",                  // Build directory
                            "**/io/getstream/android/video/generated/**/*.kt" // Open Api generated code
                    )
                    ktlint()
                        .editorConfigOverride(
                            mapOf(
                             "ktlint_standard_max-line-length" to "disabled"
                            )
                        )
                    trimTrailingWhitespace()
                    endWithNewline()
                    licenseHeaderFile(rootProject.file("$rootDir/spotless/copyright.kt"))
                }
                format("openapiGenerated") {
                    target("**/*.kt")
                    targetExclude("**/build/**/*.kt", "**/io/getstream/android/video/generated/**/*.kt")
                    trimTrailingWhitespace()
                    endWithNewline()
                    licenseHeaderFile(rootProject.file("$rootDir/spotless/copyright.kt"),
                            KotlinConstants.LICENSE_HEADER_DELIMITER)
                }
                format("kts") {
                    target("**/*.kts")
                    targetExclude("**/build/**/*.kts", "**/io/getstream/android/video/generated/**/*.kt")
                    // Look for the first line that doesn't have a block comment (assumed to be the license)
                    licenseHeaderFile(
                            rootProject.file("spotless/copyright.kts"),
                            "(^(?![\\/ ]\\*).*$)"
                    )
                }
                format("xml") {
                    target("**/*.xml")
                    targetExclude("**/build/**/*.xml")
                    // Look for the first XML tag that isn't a comment (<!--) or the xml declaration (<?xml)
                    licenseHeaderFile(rootProject.file("spotless/copyright.xml"), "(<[^!?])")
                }
            }
        }
    }
}
