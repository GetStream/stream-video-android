import os
import shutil

from utils.gradle_settings import modify_settings_gradle
from utils.maven import install_android_lib_module_to_local_maven
from utils.project_configuration import extract_version_name_and_artifact_group
from utils.string_replacement import replace_string_in_directory


def repackage_and_install_webrtc_android(path: str, repackaged_webrtc_version: str):
    os.chdir(path)

    configuration_path = os.path.join(
        "buildSrc", "src", "main", "kotlin", "io", "getstream", "Configurations.kt"
    )
    project_version, group_id = extract_version_name_and_artifact_group(configuration_path)
    print(f"> WebRTC-Android: version => {project_version}")
    print(f"> WebRTC-Android: groupId => {group_id}")

    # Copy WebRTCException to `stream-webrtc-android-ktx`
    file_name = 'WebRTCException.kt'
    file_web_rtc_exception = os.path.join('stream-webrtc-android-utils', file_name)
    target_dir = os.path.join('stream-webrtc-android-ktx', 'src', 'main', 'kotlin', 'io',
                              'getstream', 'webrtc')
    shutil.copy(file_web_rtc_exception, target_dir)
    _make_kotlin_class_public(os.path.join(target_dir, file_name))

    # Repackage
    print(f"> WebRTC-Android: Repackage Started")
    replace_string_in_directory(
        directory_path=path,
        search_string="org.webrtc",
        replace_string="io.getstream.webrtc"
    )
    print(f"> WebRTC-Android: Repackage Completed")

    # Modify settings.gradle file
    os.chdir(path)
    modify_settings_gradle()
    print(f"> WebRTC-Android: settings.gradle has been modified")

    # Modify build.gradle files
    print(f"> WebRTC-Android: modify build.gradle files")
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
    _modify_build_gradle_webrtc_android_module(
        module_name="app",
        repackaged_module_name="app",
        repackaged_webrtc_version=repackaged_webrtc_version
    )
    print(f"> WebRTC-Android: build.gradle files have been modified")

    # Install modules
    os.chdir(path)
    install_android_lib_module_to_local_maven(
        module_name="stream-webrtc-android-ui",
        artifact_id="streamx-webrtc-android-ui",
        group_id=group_id,
        version=project_version
    )
    print(f"> WebRTC-Android: installed stream-webrtc-android-ui")
    os.chdir(path)
    install_android_lib_module_to_local_maven(
        module_name="stream-webrtc-android-ktx",
        artifact_id="streamx-webrtc-android-ktx",
        group_id=group_id,
        version=project_version
    )
    print(f"> WebRTC-Android: installed stream-webrtc-android-ktx")


def _make_kotlin_class_public(file_path: str) -> None:
    """
    Make the class declaration in a file explicitly public.
    """
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.readlines()

    modified_content = []
    for line in content:
        stripped_line = line.strip()
        if stripped_line.startswith("class ") and "public class" not in stripped_line:
            indent = line[:line.index("class")]
            modified_content.append(indent + "public " + line.lstrip())
        else:
            modified_content.append(line)

    with open(file_path, 'w', encoding='utf-8') as file:
        file.writelines(modified_content)


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
        f'api(project(":stream-webrtc-android"))',
        f'api("io.getstream:streamx-webrtc-android:{repackaged_webrtc_version}")'
    )
    content = content.replace(
        f'implementation(project(":stream-webrtc-android"))',
        f'implementation("io.getstream:streamx-webrtc-android:{repackaged_webrtc_version}")'
    )
    content = content.replace(module_name, repackaged_module_name)

    # Write the modified content back to the original file
    with open(file_path, 'w') as f:
        f.write(content)

    print(f"build.gradle.kts in {file_path} has been modified.")
