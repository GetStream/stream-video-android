import os
import subprocess
import sys
from typing import Optional


def install_file_to_local_maven(
        file_path: str,
        group_id: str,
        artifact_id: str,
        version: str,
        packaging: str,
        sources: Optional[str] = None,
        javadoc: Optional[str] = None,
):
    if not os.path.exists(file_path):
        sys.exit(f"Error: Path '{file_path}' does not exist.")

    # Install Library to the local Maven repository
    mvn_cmd = [
        f"mvn", "install:install-file",
        f"-Dfile={file_path}",
        f"-DgroupId={group_id}",
        f"-DartifactId={artifact_id}",
        f"-Dversion={version}",
        f"-Dpackaging={packaging}",
    ]

    if sources and os.path.exists(sources):
        mvn_cmd.extend([f"-Dsources={sources}"])

    if javadoc and os.path.exists(javadoc):
        mvn_cmd.extend([f"-Djavadoc={javadoc}"])

    subprocess.run(mvn_cmd, check=True)

    # Output for verification
    print(f"...installed library {artifact_id} version {version} to local Maven repository.")


def install_android_lib_module_to_local_maven(module_name: str):
    # Clean module
    subprocess.run(["./gradlew", f":{module_name}:clean"], check=True)

    # Assemble release AAR
    subprocess.run(["./gradlew", f":{module_name}:assembleRelease"], check=True)

    # Publish to local maven
    subprocess.run(["./gradlew", f":{module_name}:publishToMavenLocal"], check=True)
