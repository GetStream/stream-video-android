package io.getstream.video.android.commands.rpc.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import io.getstream.video.android.commands.rpc.plugin.GenerateRPCServiceExtension
import org.gradle.api.tasks.Input
import org.gradle.internal.file.FileException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private val CommentRegex = "//.*|/\\*(?s).*?\\*/".toRegex()
private val ProtoPackageRegex = "(?<=package)(.*?)(?=;)".toRegex()
private val ProtoImportsRegex = "((?<=\\\")(.*)(?=\\/))".toRegex()
private val KotlinFilePackageRegex = "(.*?)kotlin/".toRegex()
private val RPCServiceRegex = "service(.*)(?s).*?\\}".toRegex()
private val RPCServiceNameRegex = "(?<=^service)(.*?)(?=\\{)".toRegex()
private val RPCCallRegex = "(?<=rpc)(.*?)(?=;)".toRegex()
private val RPCCallNameRegex = "(?<=^)(.*?)(?=\\()".toRegex()
private val RPCCallModelRegex = "(?<=\\()(.*?)(?=\\))".toRegex()

open class GenerateRPCServiceTask : DefaultTask() {

    @Input
    lateinit var config: GenerateRPCServiceExtension

    @TaskAction
    fun generateServices() {
        val outputFolder = File(config.outputDir)
        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            throw FileException(Throwable("Failed to create output folder: ${outputFolder.path}."))
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
                val serviceFile = getService(
                    serviceName = service.name,
                    filePackage = config.outputDir.replace(KotlinFilePackageRegex, "").replace("/", "."),
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
                            returnType = it.output
                        )
                    }
                )

                try {
                    val outputFile = File("${config.outputDir}/${service.name}Service.kt")
                    if (!outputFile.createNewFile()) throw FileNotFoundException("Failed to create a file for ${service.name}")
                    outputFile.writeText(serviceFile)
                } catch (exception: IOException) {
                    println("Exception for service: ${service.name} inside ${protoFile.name}")
                    exception.printStackTrace()
                }

            }
        }
    }

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

    private fun getCleanProtoFile(lines: List<String>): String {
        return CommentRegex.replace(lines.joinToString("\n"), "")
    }

    private fun getPackageName(proto: String, fileName: String): String {
        return ProtoPackageRegex.find(proto)?.value?.trim()
            ?: throw NullPointerException("Could not find package for file: $fileName")
    }

    private fun getImports(proto: String): List<String> {
        return ProtoImportsRegex.findAll(proto).map { it.value.trim().replace("/", ".") }.toList()
    }

    private fun getRPCServices(proto: String, fileName: String): List<RPCService> {
        return RPCServiceRegex.findAll(proto).map {
            RPCService(
                name = getRPCServiceName(it.value, fileName),
                calls = getRPCServiceCalls(it.value, fileName)
            )
        }.toList()
    }

    private fun getRPCServiceName(service: String, fileName: String): String {
        return RPCServiceNameRegex.find(service)?.value?.trim()
            ?: throw NullPointerException("Cant find name for service inside: $fileName")
    }

    private fun getRPCServiceCalls(service: String, fileName: String): List<RPCCall> {
        return getRPCCalls(service).map {
            val name = getRPCCallName(it, fileName)
            val models = getRPCCallModels(it)

            RPCCall(
                name = name,
                input = models.first,
                output = models.second
            )
        }
    }

    private fun getRPCCalls(service: String): List<String> {
        return RPCCallRegex.findAll(service).map { it.value.trim() }.toList()
    }

    private fun getRPCCallName(call: String, fileName: String): String {
        return RPCCallNameRegex.find(call)?.value
            ?: throw NullPointerException("Could not find service name for call: $call; file: $fileName")
    }

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
    ): String {
        val model = input.split(".").last()
        return """
    @Headers("Content-Type: application/protobuf")
    @POST("/rpc/$packageName.$serviceName/$call")
    public suspend fun ${call.decapitalize()}(
        @Body ${model.decapitalize()}: $model,
        @Query("api_key") apiKey: String
    ): $returnType"""
    }

    private fun getService(
        serviceName: String,
        filePackage: String,
        imports: List<String>,
        functions: List<String>,
    ): String {
        return """package $filePackage
        
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
