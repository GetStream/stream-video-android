/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License; see the repository LICENSE file.
 */
package io.getstream.gradle.audioswitch

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

@CacheableTask
public abstract class ReplaceAudioSwitchClassesTask : DefaultTask() {

    @get:Classpath
    public abstract val allJars: ListProperty<RegularFile>

    @get:Classpath
    public abstract val allDirectories: ListProperty<Directory>

    @get:Input
    public abstract val patchId: Property<String>

    @get:OutputFile
    public abstract val output: RegularFileProperty

    @TaskAction
    public fun patch() {
        val originalNames = readResourceLines("$PATCH_ROOT/original-index.txt").toSet()
        val replacementNames = readResourceLines("$PATCH_ROOT/replacement-index.txt").toSet()
        val originalHashes = readResourceLines("$PATCH_ROOT/original-sha256.txt").toSet()

        validateIndex("original", originalNames)
        validateIndex("replacement", replacementNames)
        if (MAIN_CLASS !in originalNames || MAIN_CLASS !in replacementNames) {
            throw GradleException("AudioSwitch patch indexes must include $MAIN_CLASS")
        }

        val writtenClasses = mutableSetOf<String>()
        val removedClasses = mutableSetOf<String>()
        var originalMainHash: String? = null
        val outputFile = output.get().asFile
        outputFile.parentFile.mkdirs()

        JarOutputStream(BufferedOutputStream(outputFile.outputStream())).use { outputJar ->
            allJars.get().map { it.asFile }.sortedBy { it.absolutePath }.forEach { jar ->
                JarFile(jar).use { inputJar ->
                    inputJar.entries().asSequence()
                        .filterNot { it.isDirectory }
                        .filter { it.name.endsWith(CLASS_SUFFIX) }
                        .filterNot { isModuleDescriptor(it.name) }
                        .sortedBy { it.name }
                        .forEach { entry ->
                            val bytes = inputJar.getInputStream(entry).use { it.readBytes() }
                            originalMainHash = processClass(
                                entry.name,
                                bytes,
                                outputJar,
                                writtenClasses,
                                removedClasses,
                                originalMainHash,
                            )
                        }
                }
            }

            allDirectories.get().map { it.asFile }.sortedBy { it.absolutePath }.forEach { directory ->
                directory.walkTopDown()
                    .filter { it.isFile && it.extension == "class" }
                    .filterNot { isModuleDescriptor(it.name) }
                    .sortedBy { it.relativeTo(directory).invariantSeparatorsPath }
                    .forEach { file ->
                        val name = file.relativeTo(directory).invariantSeparatorsPath
                        originalMainHash = processClass(
                            name,
                            file.readBytes(),
                            outputJar,
                            writtenClasses,
                            removedClasses,
                            originalMainHash,
                        )
                    }
            }

            if (removedClasses != originalNames) {
                val missing = (originalNames - removedClasses).sorted()
                val unexpected = (removedClasses - originalNames).sorted()
                throw GradleException(
                    "Unsupported AudioSwitch classes. Missing: $missing; unexpected: $unexpected. " +
                        "Expected the exact Twilio AudioSwitch 1.2.0 class family.",
                )
            }
            if (originalMainHash !in originalHashes) {
                throw GradleException(
                    "Unsupported AudioSwitch bytecode (SHA-256: $originalMainHash). " +
                        "Expected Twilio AudioSwitch 1.2.0.",
                )
            }

            replacementNames.sorted().forEach { className ->
                writeClass(
                    outputJar,
                    className,
                    readResourceBytes("$PATCH_ROOT/classes/$className"),
                    writtenClasses,
                )
            }
        }
    }

    private fun processClass(
        name: String,
        bytes: ByteArray,
        outputJar: JarOutputStream,
        writtenClasses: MutableSet<String>,
        removedClasses: MutableSet<String>,
        originalMainHash: String?,
    ): String? {
        if (!isAudioSwitchClassFamily(name)) {
            writeClass(outputJar, name, bytes, writtenClasses)
            return originalMainHash
        }
        if (!removedClasses.add(name)) {
            throw GradleException("Duplicate AudioSwitch class encountered: $name")
        }
        return if (name == MAIN_CLASS) sha256(bytes) else originalMainHash
    }

    private fun validateIndex(label: String, names: Set<String>) {
        if (names.isEmpty() || names.any { !isAudioSwitchClassFamily(it) }) {
            throw GradleException("Invalid $label AudioSwitch class index")
        }
    }

    private fun writeClass(
        outputJar: JarOutputStream,
        name: String,
        bytes: ByteArray,
        writtenClasses: MutableSet<String>,
    ) {
        if (!writtenClasses.add(name)) {
            throw GradleException("Duplicate class encountered: $name")
        }
        outputJar.putNextEntry(JarEntry(name).apply { time = 0L })
        outputJar.write(bytes)
        outputJar.closeEntry()
    }

    private fun readResourceLines(path: String): List<String> =
        javaClass.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { reader ->
                reader.lineSequence()
                    .map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toList()
            }
            ?: throw GradleException("Missing plugin resource: $path")

    private fun readResourceBytes(path: String): ByteArray =
        javaClass.getResourceAsStream(path)?.use { it.readBytes() }
            ?: throw GradleException("Missing replacement class: $path")

    private fun isAudioSwitchClassFamily(name: String): Boolean =
        name == MAIN_CLASS ||
            (name.startsWith(AUDIO_SWITCH_PREFIX) && name.endsWith(CLASS_SUFFIX))

    private fun isModuleDescriptor(name: String): Boolean =
        name == "module-info.class" || name.endsWith("/module-info.class")

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {
        private const val PATCH_ROOT = "/patches/1.2.0"
        private const val AUDIO_SWITCH_PREFIX = "com/twilio/audioswitch/AudioSwitch\$"
        private const val MAIN_CLASS = "com/twilio/audioswitch/AudioSwitch.class"
        private const val CLASS_SUFFIX = ".class"
    }
}
