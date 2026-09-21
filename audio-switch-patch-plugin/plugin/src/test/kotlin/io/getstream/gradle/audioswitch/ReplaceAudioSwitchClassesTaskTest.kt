package io.getstream.gradle.audioswitch

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipFile

class ReplaceAudioSwitchClassesTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `replaces the complete pinned AudioSwitch class family`() {
        val input = extractOriginalClassesJar()
        val output = temporaryFolder.newFile("patched.jar")

        createTask("patchAudioSwitch", input, output).patch()

        JarFile(input).use { originalJar ->
            JarFile(output).use { patchedJar ->
                val replacementNames = resourceLines("/patches/1.2.0/replacement-index.txt").toSet()
                val actualNames = patchedJar.entries().asSequence()
                    .map { it.name }
                    .filter(::isAudioSwitchClass)
                    .toSet()

                assertEquals(replacementNames, actualNames)
                assertArrayEquals(
                    resourceBytes("/patches/1.2.0/classes/$MAIN_CLASS"),
                    patchedJar.bytes(MAIN_CLASS),
                )
                assertNotEquals(
                    sha256(originalJar.bytes(MAIN_CLASS)),
                    sha256(patchedJar.bytes(MAIN_CLASS)),
                )
                assertArrayEquals(
                    originalJar.bytes(AUDIO_DEVICE_CLASS),
                    patchedJar.bytes(AUDIO_DEVICE_CLASS),
                )
            }
        }
    }

    @Test
    fun `rejects AudioSwitch bytecode that is not allowlisted`() {
        val input = extractOriginalClassesJar()
        val modifiedInput = temporaryFolder.newFile("modified-classes.jar")
        rewriteJar(input, modifiedInput) { name, bytes ->
            if (name == MAIN_CLASS) bytes.copyOf().apply { this[lastIndex] = last().inc() } else bytes
        }

        val exception = assertThrows(GradleException::class.java) {
            createTask(
                "rejectUnknownAudioSwitch",
                modifiedInput,
                temporaryFolder.newFile("rejected.jar"),
            ).patch()
        }

        assertEquals(true, exception.message?.startsWith("Unsupported AudioSwitch bytecode"))
    }

    private fun createTask(
        name: String,
        input: File,
        output: File,
    ): ReplaceAudioSwitchClassesTask {
        val project = ProjectBuilder.builder().withProjectDir(temporaryFolder.root).build()
        return project.tasks.create(name, ReplaceAudioSwitchClassesTask::class.java).apply {
            allJars.set(listOf(project.layout.file(project.provider { input }).get()))
            allDirectories.empty()
            patchId.set("audioswitch-1.2.0-getstream-1")
            this.output.set(output)
        }
    }

    private fun extractOriginalClassesJar(): File {
        val aar = File(requireNotNull(System.getProperty("audioswitch.original.aar")))
        val output = temporaryFolder.newFile("original-classes-${System.nanoTime()}.jar")
        ZipFile(aar).use { zip ->
            zip.getInputStream(requireNotNull(zip.getEntry("classes.jar"))).use { input ->
                output.outputStream().use(input::copyTo)
            }
        }
        return output
    }

    private fun rewriteJar(
        input: File,
        output: File,
        transform: (String, ByteArray) -> ByteArray,
    ) {
        JarFile(input).use { inputJar ->
            JarOutputStream(output.outputStream()).use { outputJar ->
                inputJar.entries().asSequence().filterNot { it.isDirectory }.forEach { entry ->
                    val bytes = inputJar.getInputStream(entry).use { it.readBytes() }
                    outputJar.putNextEntry(JarEntry(entry.name))
                    outputJar.write(transform(entry.name, bytes))
                    outputJar.closeEntry()
                }
            }
        }
    }

    private fun JarFile.bytes(name: String): ByteArray =
        getInputStream(requireNotNull(getJarEntry(name))).use { it.readBytes() }

    private fun resourceLines(path: String): List<String> =
        requireNotNull(javaClass.getResourceAsStream(path)).bufferedReader().use { reader ->
            reader.lineSequence().filter(String::isNotBlank).toList()
        }

    private fun resourceBytes(path: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream(path)).use { it.readBytes() }

    private fun isAudioSwitchClass(name: String): Boolean =
        name == MAIN_CLASS || name.startsWith("com/twilio/audioswitch/AudioSwitch\$")

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        private const val MAIN_CLASS = "com/twilio/audioswitch/AudioSwitch.class"
        private const val AUDIO_DEVICE_CLASS = "com/twilio/audioswitch/AudioDevice.class"
    }
}
