package io.getstream.video.buildlogic.helloworld

import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

private const val HELLO_MESSAGE = "Hello world from the Stream compiler plugin!"

/**
 * Command line processor that allows Kotlin to discover the compiler plugin via `-Xplugin=<id>`.
 */
@OptIn(ExperimentalCompilerApi::class)
class HelloWorldCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String
        get() = HelloWorldComponentRegistrar.PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = emptyList()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        throw IllegalArgumentException("$pluginId does not support any options")
    }
}

/**
 * Simple component registrar that prints a log message when the compiler plugin is initialized.
 */
@OptIn(ExperimentalCompilerApi::class)
class HelloWorldComponentRegistrar : ComponentRegistrar {
    companion object {
        const val PLUGIN_ID = "io.getstream.video.hello-world"
    }

    override val supportsK2: Boolean = true

    override fun registerProjectComponents(_project: MockProject, configuration: CompilerConfiguration) {
        val collector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        collector?.report(CompilerMessageSeverity.WARNING, HELLO_MESSAGE)
        println(HELLO_MESSAGE)
    }
}
