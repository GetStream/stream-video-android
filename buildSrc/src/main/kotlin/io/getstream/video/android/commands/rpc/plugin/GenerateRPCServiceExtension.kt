package io.getstream.video.android.commands.rpc.plugin

open class GenerateRPCServiceExtension {
    var srcDir = ""
        set(value) { field = if (field.isBlank()) value else "$field/$value" }
    internal var outputDir = ""
}