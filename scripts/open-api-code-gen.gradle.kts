tasks.register<Exec>("generateOpenApiClient") {
    group = "Custom"
    description = "Generates open api client"

    val repoUrl = project.findProperty("repoUrl") as? String ?: "git@github.com:GetStream/chat.git"
    val refType = project.findProperty("refType") as? String ?: "branch"
    val refValue = project.findProperty("refValue") as? String ?: "feature/rahullohra/kotlin_open_api_generator"

    val modelPackageName = project.findProperty("modelPackage") as? String ?: "org.openapitools.client.models"
    val modelsDir = project.findProperty("modelsDir") as? String ?: "models"

    val apiServicePackageName = project.findProperty("apiServicePackage") as? String ?: "org.openapitools.client.apis"
    val apiServiceClassName = project.findProperty("apiServiceClassName") as? String ?: "ProductvideoApi"
    val apiServiceDir = project.findProperty("apiServiceDir") as? String ?: "apis"

    val moshiAdaptersDir = project.findProperty("moshiAdaptersDir") as? String ?: "infrastructure"
    val moshiAdapterPackage = project.findProperty("moshiAdapterPackageName") as? String ?: "org.openapitools.client.infrastructure"

    val classesToSkip = project.findProperty("classesToSkip") as? String ?: "PrivacySettingsResponse,PrivacySettings,StopRTMPBroadcastsRequest"

//    val outputSpec = project.findProperty("outputSpec") as? String ?: "./releases/video-openapi-clientside"
    val outputClient = project.findProperty("outputClient") as? String ?: "stream-video-android-core/src/main/kotlin/org/openapitools/client/v3"
    val keepClasses = project.findProperty("keepClasses") as? String ?: "WSAuthMessageRequest.kt"
    val buildDir = project.layout.buildDirectory.asFile.get()

    println("outputClient = $outputClient")
    println("path = $path")
    println("buildDir = $buildDir")
    println("workingDir = $workingDir")
    println("projectDir = ${projectDir}")
    println("project root dir = ${project.rootDir}")
    println("Keep Classes = ${keepClasses}")

//    return@register

    workingDir = project.rootDir
    val scriptFile = file("${project.rootDir}/generate_openapi_v2.sh")

    // Make sure the script exists
    if (!scriptFile.exists()) {
        throw GradleException("❌ ERROR: Script not found at ${scriptFile.absolutePath}")
    }

    // Ensure the script has execute permission
    scriptFile.setExecutable(true)

    commandLine("sh", scriptFile.absolutePath,
        "--repo-url=$repoUrl",
        "--ref-type=$refType",
        "--ref-value=$refValue",
        "--model-package-name=$modelPackageName",
        "--model-dir=$modelsDir",
        "--api-service-package-name=$apiServicePackageName",
        "--api-service-class-name=$apiServiceClassName",
        "--api-service-dir=$apiServiceDir",
        "--moshi-adapters-dir=$moshiAdaptersDir",
        "--moshi-adapters-package-name=$moshiAdapterPackage",
        "--classes-to-skip=$classesToSkip",
        "--keep-classes=$keepClasses",
        "--output-client=$outputClient",
    )

    doLast {
        println("✅ OpenAPI Kotlin client generation completed via Gradle!")
    }
}
