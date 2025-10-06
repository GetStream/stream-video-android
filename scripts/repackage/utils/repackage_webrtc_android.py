import os
import shutil
from datetime import datetime, time

from utils.gradle_publish import override_gradle_publish
from utils.gradle_settings import modify_gradle_settings
from utils.maven import install_android_lib_module_to_local_maven
from utils.project_configuration import extract_version_name_and_artifact_group
from utils.string_replacement import replace_string_in_directory


def repackage_and_install_webrtc_android(path: str) -> str:
    start_time = int(datetime.utcnow().timestamp() * 1000)
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
    print("> WebRTC-Android: Repackage Started")
    replace_string_in_directory(
        directory_path=path,
        search_string="io.getstream.webrtc",
        replace_string="io.getstream.webrtc"
    )
    print("> WebRTC-Android: Repackage Completed")

    # Modify settings.gradle file
    os.chdir(path)
    modify_gradle_settings()
    print("> WebRTC-Android: settings.gradle has been modified")

    # Modify publish-module.gradle file
    os.chdir(path)
    override_gradle_publish(os.path.join('scripts', 'publish-module.gradle'))
    print("> WebRTC-Android: publish-module.gradle has been modified")

    # Modify build.gradle files
    print("> WebRTC-Android: modify build.gradle files")
    _modify_build_gradle_files(path, project_version)
    print("> WebRTC-Android: build.gradle files have been modified")

    # Install modules
    os.chdir(path)
    print(f"> WebRTC-Android: install modules")
    _install_modules()
    print("> WebRTC-Android: modules have been installed")

    now = int(datetime.utcnow().timestamp() * 1000)
    elapsed = now - start_time
    print(f"\nREPACKAGE SUCCESSFUL (WebRTC-Android) in {elapsed}ms")
    return project_version


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


def _modify_build_gradle_files(path: str, project_version: str) -> None:
    os.chdir(path)
    for subdir, _, files in os.walk(path):
        for filename in files:
            if 'build.gradle' in filename:
                file_path = os.path.join(subdir, filename)
                _modify_build_gradle(file_path, project_version)


def _modify_build_gradle(file_path: str, project_version: str) -> None:
    # Read the content of the file
    with open(file_path, 'r') as file:
        lines = file.readlines()

    with open(file_path, 'w') as file:
        for line in lines:
            if "PUBLISH_ARTIFACT_ID" in line:
                line = line.replace("stream-webrtc-android", "streamx-webrtc-android")
            elif 'api(project(":stream-webrtc-android"))' in line:
                line = line.replace(
                    'api(project(":stream-webrtc-android"))',
                    f'api("io.getstream:streamx-webrtc-android:{project_version}")'
                )
            elif 'implementation(project(":stream-webrtc-android"))' in line:
                line = line.replace(
                    'implementation(project(":stream-webrtc-android"))',
                    f'implementation("io.getstream:streamx-webrtc-android:{project_version}")'
                )
            file.write(line)

    print(f"...{file_path} has been modified.")


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

    print(f"...build.gradle.kts in {file_path} has been modified.")


def _install_modules():
    modules = [
        'stream-webrtc-android-ui',
        'stream-webrtc-android-ktx',
        'stream-webrtc-android-compose'
    ]
    for module in modules:
        install_android_lib_module_to_local_maven(module)
