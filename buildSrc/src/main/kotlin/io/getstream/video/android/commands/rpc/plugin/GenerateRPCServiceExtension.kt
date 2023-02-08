package io.getstream.video.android.commands.rpc.plugin

/**
 * Extension that is used inside build.gradle to setup the plugin.
 */
open class GenerateRPCServiceExtension {
    /**
     * Source directory from which to extract proto files.
     */
    var srcDir = "stream-video-android-core/src/main/proto"

    /**
     * Output folder for the files. If the output folder is in build don't forget to add it to sources in the modules
     * build.gradle.
     */
    var outputDir = ""
}