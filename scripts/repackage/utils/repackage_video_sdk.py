import os
from datetime import datetime

from utils.gradle_publish import override_gradle_publish
from utils.gradle_settings import modify_gradle_settings
from utils.maven import install_android_lib_module_to_local_maven
from utils.project_configuration import extract_version_name_and_artifact_group
from utils.string_replacement import replace_string_in_directory


def repackage_and_install_video_sdk(path: str, repackaged_webrtc_version: str):
    start_time = int(datetime.utcnow().timestamp() * 1000)
    os.chdir(path)

    configuration_path = os.path.join(
        "buildSrc", "src", "main", "kotlin", "io", "getstream", "video", "android",
        "Configuration.kt"
    )
    project_version, group_id = extract_version_name_and_artifact_group(configuration_path)
    print(f"> VideoSDK: version => {project_version}")
    print(f"> VideoSDK: groupId => {group_id}")

    # Repackage
    print(f"> VideoSDK: Repackage Started")
    replace_string_in_directory(
        directory_path=path,
        search_string="org.webrtc",
        replace_string="io.getstream.webrtc"
    )
    print(f"> VideoSDK: Repackage Completed")

    # Modify settings.gradle file
    os.chdir(path)
    modify_gradle_settings()
    print(f"> VideoSDK: settings.gradle has been modified")

    # Modify publish-module.gradle file
    os.chdir(path)
    override_gradle_publish(os.path.join('scripts', 'publish-module.gradle'))
    print("> VideoSDK: publish-module.gradle has been modified")

    # Modify libs.versions.toml file
    os.chdir(path)
    _modify_gradle_libs_version(repackaged_webrtc_version)

    # Modify build.gradle files
    os.chdir(path)
    _modify_build_gradle_files(path)

    # Install modules
    os.chdir(path)
    _install_modules()

    now = int(datetime.utcnow().timestamp() * 1000)
    elapsed = now - start_time
    print(f"\nREPACKAGE SUCCESSFUL (VideoSDK) in {elapsed}ms")


def _modify_gradle_libs_version(repackaged_webrtc_version: str):
    file_path = os.path.join("gradle", "libs.versions.toml")

    # streamWebRTC = "1.1.0"
    with open(file_path, 'r') as file:
        lines = file.readlines()

    with open(file_path, 'w') as file:
        for line in lines:
            if line.startswith("streamWebRTC"):
                line = f'streamWebRTC = "{repackaged_webrtc_version}"\n'
            elif "stream-webrtc-android" in line:
                line = line.replace("stream-webrtc-android", "streamx-webrtc-android")
            file.write(line)
    print(f"{file_path} has been modified.")


def _modify_build_gradle_files(folder: str) -> None:
    for subdir, _, files in os.walk(folder):
        for filename in files:
            if 'build.gradle' in filename:
                file_path = os.path.join(subdir, filename)
                _modify_build_gradle(file_path)


def _modify_build_gradle(file_path: str) -> None:
    # # Read the content of the file
    # with open(file_path, 'r') as f:
    #     content = f.read()
    #
    # # Perform replacements
    # content = content.replace(
    #     'implementation(project(":stream-video',
    #     'implementation(project(":streamx-video'
    # )
    # content = content.replace(
    #     'api(project(":stream-video',
    #     'api(project(":streamx-video'
    # )
    # content = content.replace(
    #     'compileOnly(project(":stream-video',
    #     'compileOnly(project(":streamx-video'
    # )
    #
    # # Write the modified content back to the original file
    # with open(file_path, 'w') as f:
    #     f.write(content)
    #
    # print(f"build.gradle.kts in {file_path} has been modified.")

    # Read the content of the file
    with open(file_path, 'r') as file:
        lines = file.readlines()

    with open(file_path, 'w') as file:
        for line in lines:
            if "PUBLISH_ARTIFACT_ID" in line:
                line = line.replace("stream-video-android", "streamx-video-android")
            file.write(line)

    print(f"...{file_path} has been modified.")


def _install_modules():
    modules = [
        "stream-video-android-mock",
        "stream-video-android-model",
        "stream-video-android-datastore",
        "stream-video-android-tooling",
        "stream-video-android-core",
        "stream-video-android-ui-common",
        "stream-video-android-compose",
        "stream-video-android-xml",
    ]
    for module in modules:
        install_android_lib_module_to_local_maven(module)
