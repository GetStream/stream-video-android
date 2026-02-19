import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.TestExtension
import io.getstream.video.configureFlavors
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class DemoFlavorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension>(::configureFlavors)
            }
            pluginManager.withPlugin("com.android.test") {
                extensions.configure<TestExtension>(::configureFlavors)
            }
        }
    }
}
