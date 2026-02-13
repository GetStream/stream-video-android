import io.getstream.video.configureMetalava
import org.gradle.api.Plugin
import org.gradle.api.Project

class MetalavaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("me.tylerbwong.gradle.metalava")
            configureMetalava()
        }
    }
}
