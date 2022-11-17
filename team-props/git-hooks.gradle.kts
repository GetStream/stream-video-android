fun shouldInstallHooks(): Boolean {
    val osName = System.getProperty("os.name")
        .toLowerCase()
    return osName.contains("linux") || osName.contains("mac os") || osName.contains("macos")
}

fun hooksEnabled(): Boolean {
    val localProperties = java.util.Properties().apply {
        load(java.io.FileInputStream(File(rootProject.rootDir, "local.properties")))
    }

    return localProperties.getProperty("enableGitHooks") == "true"
}

tasks.create<Copy>("copyGitHooks") {
    description = "Copies the git hooks from team-props/git-hooks to the .git folder."
    from("$rootDir/team-props/git-hooks/") {
        include("**/*.sh")
        rename("(.*).sh", "$1")
    }
    into("$rootDir/.git/hooks")
    onlyIf { shouldInstallHooks() && hooksEnabled() }
}

tasks.create<Exec>("installGitHooks") {
    description = "Installs the pre-commit git hooks from team-props/git-hooks."
    group = "git hooks"
    workingDir(rootDir)
    commandLine("chmod")
    args("-R", "+x", ".git/hooks/")
    dependsOn("copyGitHooks")
    onlyIf { shouldInstallHooks() && hooksEnabled() }
    doLast {
        logger.info("Git hook installed successfully.")
    }
}

tasks.getByName("installGitHooks")
    .dependsOn(getTasksByName("copyGitHooks", true))
tasks.getByPath("app:preBuild")
    .dependsOn(getTasksByName("installGitHooks", true))
