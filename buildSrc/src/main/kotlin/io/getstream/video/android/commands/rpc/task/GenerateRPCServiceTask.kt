package io.getstream.video.android.commands.rpc.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import io.getstream.video.android.commands.rpc.plugin.GenerateRPCServiceExtension
import org.gradle.api.tasks.Input
import org.gradle.internal.file.FileException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Detects all of the comments. Used to clean the comments so that we don't generate functions that are commented out.
 */
private val CommentRegex = "//.*|/\\*(?s).*?\\*/".toRegex()

/**
 * Detects the proto files package.
 */
private val ProtoPackageRegex = "(?<=package)(.*?)(?=;)".toRegex()

/**
 * Used to get all of the imports inside the proto file for models outside the proto file we are currently parsing.
 */
private val ProtoImportsRegex = "((?<=\\\")(.*)(?=\\/))".toRegex()

/**
 * Used to get all of the services that might be inside a proto file.
 */
private val RPCServiceRegex = "service(.*)(?s).*?\\}".toRegex()

/**
 * Used to get the name of the service we are currently parsing.
 */
private val RPCServiceNameRegex = "(?<=^service)(.*?)(?=\\{)".toRegex()

/**
 * Used to extract all of the rpc calls from a service.
 */
private val RPCCallRegex = "(?<=rpc)(.*?)(?=;)".toRegex()

/**
 * Used to get the rpc call name.
 */
private val RPCCallNameRegex = "(?<=^)(.*?)(?=\\()".toRegex()

/**
 * Used to get the models that are used as inputs and outputs for a rpc call.
 */
private val RPCCallModelRegex = "(?<=\\()(.*?)(?=\\))".toRegex()

open class GenerateRPCServiceTask : DefaultTask() {

    @Input
    lateinit var config: GenerateRPCServiceExtension

    @TaskAction
    fun generateServices() {
        val outputFolder = File(config.outputDir)
        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            throw FileException(Throwable("Failed to create output folder: ${outputFolder.absolutePath}."))
        }

        val file = File(config.srcDir)
        val protoFiles = file.walk().filter {
            it.name.split(".").last() == "proto" && it.isFile
        }

        for (protoFile in protoFiles) {
            val proto = getCleanProtoFile(protoFile.readLines())
            if (!proto.contains("service")) continue

            val packageName = getPackageName(proto, protoFile.name)
            val imports = getImports(proto)
            val services = getRPCServices(proto, protoFile.name)

            services.forEach { service ->
                // TODO move to rpc only when BE sorts it out
                val isRPC = service.name.contains("RPC")

                val serviceFile = getService(
                    serviceName = service.name,
                    imports = getModelImports(
                        packageName = packageName,
                        imports = imports,
                        models = service.calls.map { it.input }.union(service.calls.map { it.output }).toList()
                    ),
                    functions = service.calls.map {
                        getFunction(
                            packageName = packageName,
                            serviceName = service.name,
                            call = it.name,
                            input = it.input,
                            returnType = it.output,
                            isRPC = isRPC
                        )
                    }
                )

                try {
                    val outputFile = File("${config.outputDir}/${service.name}Service.kt")
                    if (!outputFile.exists() && !outputFile.createNewFile()) throw FileNotFoundException("Failed to create a file for ${service.name}")
                    outputFile.writeText(serviceFile)
                } catch (exception: IOException) {
                    println("Exception for service: ${service.name} inside ${protoFile.name}")
                    exception.printStackTrace()
                }
            }
        }
    }

    /**
     * Gets the import for each model that is inside a service.
     *
     * @param packageName The package of the proto file.
     * @param imports Imports inside the proto file.
     * @param models The models that are inside the service.
     *
     * @return A list of imports for the models.
     */
    private fun getModelImports(packageName: String, imports: List<String>, models: List<String>): List<String> {
        return models.map { model ->
            if (model.contains(".")) {
                val packageRoot = packageName.split(".").first()
                val splitModel = model.split(".")
                val modelName = splitModel.last()
                val import = imports.first { it.split(".").last() == splitModel.first() }

                "$packageRoot.$import.${modelName}"
            } else {
                "$packageName.$model"
            }
        }
    }

    /**
     * Removes all of the comments from the proto file.
     *
     * @param lines All of the lines from proto that have been read using readLines().
     *
     * @return Returns a string that contains the proto file without any comments.
     */
    private fun getCleanProtoFile(lines: List<String>): String {
        return CommentRegex.replace(lines.joinToString("\n"), "")
    }

    /**
     * Gets the package name form the proto file.
     *
     * @param proto The body of the proto file without comments.
     * @param fileName The name of the proto file.
     *
     * @return Package of the proto file.
     */
    private fun getPackageName(proto: String, fileName: String): String {
        return ProtoPackageRegex.find(proto)?.value?.trim()
            ?: throw NullPointerException("Could not find package for file: $fileName")
    }

    /**
     * Gets all of the imports from the proto file.
     *
     * @param proto The body of the proto file without comments.
     *
     * @return A list of imports from the proto file.
     */
    private fun getImports(proto: String): List<String> {
        return ProtoImportsRegex.findAll(proto).map { it.value.trim().replace("/", ".") }.toList()
    }

    /**
     * Extracts and returns all of the services that are inside proto files.
     *
     * @param proto The body of the proto file without comments.
     * @param fileName The name of the proto file.
     *
     * @return The list of the RPC services with the proto file.
     */
    private fun getRPCServices(proto: String, fileName: String): List<RPCService> {
        return RPCServiceRegex.findAll(proto).map {
            RPCService(
                name = getRPCServiceName(it.value, fileName),
                calls = getRPCServiceCalls(it.value, fileName)
            )
        }.toList()
    }

    /**
     * Returns a name for a single service.
     *
     * @param service The service body in the proto file.
     * @param fileName The name of the proto file.
     *
     * @return The name of the service.
     * */
    private fun getRPCServiceName(service: String, fileName: String): String {
        return RPCServiceNameRegex.find(service)?.value?.trim()
            ?: throw NullPointerException("Cant find name for service inside: $fileName")
    }

    /**
     * Returns all of the call that are inside a single service.
     *
     * @param service The service body in the proto file.
     * @param fileName The name of the proto file.
     *
     * @return The list of RPC calls inside a service.
     */
    private fun getRPCServiceCalls(service: String, fileName: String): List<RPCCall> {
        return RPCCallRegex.findAll(service).map { it.value.trim() }.toList().map {
            val name = getRPCCallName(it, fileName)
            val models = getRPCCallModels(it)

            RPCCall(
                name = name,
                input = models.first,
                output = models.second
            )
        }
    }

    /**
     * Returns the name of the RPC call,
     *
     * @param call A single call from a service.
     * @param fileName The name of the proto file.
     *
     * @return The name of the RPC call.
     */
    private fun getRPCCallName(call: String, fileName: String): String {
        return RPCCallNameRegex.find(call)?.value
            ?: throw NullPointerException("Could not find service name for call: $call; file: $fileName")
    }

    /**
     * Returns the models of the RPC call,
     *
     * @param call A single call from a service.
     *
     * @return A pair of models used in the call, Pair<Input, Output>.
     */
    private fun getRPCCallModels(call: String): Pair<String, String> {
        val models = RPCCallModelRegex.findAll(call).map { it.value }
        return models.first() to models.last()
    }

    private fun getFunction(
        packageName: String,
        serviceName: String,
        call: String,
        input: String,
        returnType: String,
        isRPC: Boolean
    ): String {
        val model = input.split(".").last()
        return """
    @Headers("Content-Type: application/protobuf")
    @POST("/${if (isRPC) "rpc" else "twirp"}/$packageName.$serviceName/$call")
    public suspend fun ${call.decapitalize()}(@Body ${model.decapitalize()}: $model): $returnType"""
    }

    private fun getService(
        serviceName: String,
        imports: List<String>,
        functions: List<String>,
    ): String {
        return """package io.getstream.video.android.api
        
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
${imports.joinToString("\n") { "import $it" }}
        
public interface ${serviceName}Service {
            ${functions.joinToString("\n")}
            
}

"""
    }
}

data class RPCService(
    val name: String,
    val calls: List<RPCCall>,
)

data class RPCCall(
    val name: String,
    val input: String,
    val output: String,
)
