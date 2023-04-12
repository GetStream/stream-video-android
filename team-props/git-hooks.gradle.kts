fun isLinuxOrMacOs(): Boolean {
  val osName = System.getProperty("os.name")
      .toLowerCase()
  return osName.contains("linux") || osName.contains("mac os") || osName.contains("macos")
}

tasks.create<Copy>("copyGitHooks") {
  description = "Copies the git hooks from team-props/git-hooks to the .git folder."
  from("$rootDir/team-props/git-hooks/") {
    include("**/*.sh")
    rename("(.*).sh", "$1")
  }
  into("$rootDir/.git/hooks")
  onlyIf { isLinuxOrMacOs() }
}

tasks.create<Exec>("installGitHooks") {
  description = "Installs the pre-commit git hooks from team-props/git-hooks."
  group = "git hooks"
  workingDir(rootDir)
  commandLine("chmod")
  args("-R", "+x", ".git/hooks/")
  dependsOn("copyGitHooks")
  onlyIf { isLinuxOrMacOs() }
  doLast {
    logger.info("Git hook installed successfully.")
  }
}

tasks.getByName("installGitHooks")
    .dependsOn(getTasksByName("copyGitHooks", true))
tasks.getByPath("app:preBuild")
    .dependsOn(getTasksByName("installGitHooks", true))
