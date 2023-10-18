import glob
import os
import subprocess

from scripts.repackage.utils.maven import install_file_to_local_maven


def install_android_lib_module_to_local_maven(
        module_name: str,
        artifact_id: str,
        group_id: str,
        version: str,
):
    # Navigate to module
    os.chdir(module_name)

    # Clean module
    subprocess.run(["./gradlew", "clean"], check=True)

    # Assemble release AAR
    subprocess.run(["./gradlew", "assembleRelease"], check=True)

    aar_files = glob.glob(os.path.join(module_name, "build/outputs/aar/*release.aar"))
    sources_files = glob.glob(os.path.join(module_name, "build/libs/*sources.jar"))
    javadoc_files = glob.glob(os.path.join(module_name, "build/libs/*javadoc.jar"))

    # Get the first matched AAR file, if any
    aar_file = aar_files[0] if aar_files else None
    sources_file = sources_files[0] if sources_files else None
    javadoc_file = javadoc_files[0] if javadoc_files else None

    if not aar_file:
        raise FileNotFoundError(f"No AAR file found at {module_name} module")

    # Install Library to the local Maven repository
    install_file_to_local_maven(
        file_path=aar_file,
        group_id=group_id,
        artifact_id=artifact_id,
        version=version,
        packaging="aar",
        sources=sources_file,
        javadoc=javadoc_file
    )