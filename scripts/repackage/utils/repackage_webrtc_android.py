import os
import shutil

from maven import install_android_lib_module_to_local_maven
from project_configuration import extract_version_name_and_artifact_group
from string_replacement import replace_string_in_directory


def repackage_and_install_webrtc_android(path: str, repackaged_webrtc_version: str):
    os.chdir(path)

    configuration_path = os.path.join(
        "buildSrc", "src", "main", "kotlin", "io", "getstream", "Configurations.kt"
    )
    project_version, group_id = extract_version_name_and_artifact_group(configuration_path)
    print(f"WebRTC-Android version: {project_version}")
    print(f"WebRTC-Android groupId: {group_id}")

    # Copy WebRTCException to `stream-webrtc-android-ktx`
    file_web_rtc_exception = os.path.join('stream-webrtc-android-utils', 'WebRTCException.kt')
    target_dir = os.path.join('stream-webrtc-android-ktx', 'src', 'main', 'kotlin', 'io',
                              'getstream', 'webrtc')
    shutil.copy(file_web_rtc_exception, target_dir)

    # Repackage
    replace_string_in_directory(
        directory_path=path,
        search_string="org.webrtc",
        replace_string="io.getstream.webrtc"
    )
    os.chdir(path)
    _modify_settings_gradle_in_webrtc_android()
    os.chdir(path)
    _modify_build_gradle_webrtc_android_module(
        module_name="stream-webrtc-android-ui",
        repackaged_module_name="streamx-webrtc-android-ui",
        repackaged_webrtc_version=repackaged_webrtc_version
    )
    os.chdir(path)
    _modify_build_gradle_webrtc_android_module(
        module_name="stream-webrtc-android-ktx",
        repackaged_module_name="streamx-webrtc-android-ktx",
        repackaged_webrtc_version=repackaged_webrtc_version
    )
    os.chdir(path)
    _modify_build_gradle_webrtc_android_module(
        module_name="stream-webrtc-android-bom",
        repackaged_module_name="streamx-webrtc-android-bom",
        repackaged_webrtc_version=repackaged_webrtc_version
    )
    os.chdir(path)
    install_android_lib_module_to_local_maven(
        module_name="stream-webrtc-android-ui",
        artifact_id="streamx-webrtc-android-ui",
        group_id=group_id,
        version=project_version
    )
    os.chdir(path)
    install_android_lib_module_to_local_maven(
        module_name="stream-webrtc-android-ktx",
        artifact_id="streamx-webrtc-android-ktx",
        group_id=group_id,
        version=project_version
    )


def _modify_settings_gradle_in_webrtc_android():
    file_path = 'settings.gradle.kts'

    # Read the content of the file
    with open(file_path, 'r') as f:
        lines = f.readlines()

    # Add mavenLocal() at appropriate places
    for i, line in enumerate(lines):
        if line.strip() == "google()":
            lines.insert(i, "mavenLocal()\n")

    # Remove the line include(":stream-webrtc-android")
    lines = [line for line in lines if 'include(":stream-webrtc-android")' not in line]

    # Write the modified content back to the original file
    with open(file_path, 'w') as f:
        f.writelines(lines)

    print(f"{file_path} has been modified.")


def _modify_build_gradle_webrtc_android_module(
        module_name: str,
        repackaged_module_name: str,
        repackaged_webrtc_version: str,
):
    file_path = os.path.join(module_name, "build.gradle.kts")

    # Read the content of the file
    with open(file_path, 'r') as f:
        content = f.read()

    # Perform replacements
    content = content.replace(
        'api(project(":stream-webrtc-android"))',
        f'api("io.getstream:streamx-webrtc-android:{repackaged_webrtc_version}")'
    )
    content = content.replace(module_name, repackaged_module_name)

    # Write the modified content back to the original file
    with open(file_path, 'w') as f:
        f.write(content)

    print(f"build.gradle.kts in {file_path} has been modified.")