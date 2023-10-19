import os

from utils.gradle_settings import modify_settings_gradle
from utils.maven import install_android_lib_module_to_local_maven
from utils.project_configuration import extract_version_name_and_artifact_group
from utils.string_replacement import replace_string_in_directory


def repackage_and_install_video_sdk(path: str, repackaged_webrtc_version: str):
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
    modify_settings_gradle()
    print(f"> VideoSDK: settings.gradle has been modified")

    # Modify libs.versions.toml file
    os.chdir(path)
    _modify_gradle_libs_version(repackaged_webrtc_version)

    # Modify build.gradle files
    os.chdir(path)
    # _modify_build_gradle_files(path)

    # Install modules
    os.chdir(path)
    _install_modules(path=path, group_id=group_id, project_version=project_version)


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


def _modify_build_gradle_files(path: str) -> None:
    modules = [
        "stream-video-android-bom",
        "stream-video-android-mock",
        "stream-video-android-model",
        "stream-video-android-datastore",
        "stream-video-android-tooling",
        "stream-video-android-core",
        "stream-video-android-ui-common",
        "stream-video-android-compose",
        "stream-video-android-xml",
        "tutorials",
        "dogfooding",
        "app",
    ]
    os.chdir(path)
    for subdir, _, files in os.walk(path):
        for filename in files:
            if 'build.gradle' in filename:
                file_path = os.path.join(subdir, filename)
                _modify_build_gradle(file_path)

    # for module_name in modules:
    #     os.chdir(path)
    #     _modify_build_gradle(module_name)


def _modify_build_gradle(file_path: str) -> None:
    # file_path = os.path.join(module_name, "build.gradle.kts")

    # Read the content of the file
    with open(file_path, 'r') as f:
        content = f.read()

    # Perform replacements
    content = content.replace(
        'implementation(project(":stream-video',
        'implementation(project(":streamx-video'
    )
    content = content.replace(
        'api(project(":stream-video',
        'api(project(":streamx-video'
    )
    content = content.replace(
        'compileOnly(project(":stream-video',
        'compileOnly(project(":streamx-video'
    )

    # Write the modified content back to the original file
    with open(file_path, 'w') as f:
        f.write(content)

    print(f"build.gradle.kts in {file_path} has been modified.")


def _install_modules(path: str, group_id: str, project_version: str):
    modules = [
        ("stream-video-android-mock", "streamx-video-android-mock"),
        ("stream-video-android-model", "streamx-video-android-model"),
        ("stream-video-android-datastore", "streamx-video-android-datastore"),
        ("stream-video-android-tooling", "streamx-video-android-tooling"),
        ("stream-video-android-core", "streamx-video-android-core"),
        ("stream-video-android-ui-common", "streamx-video-android-ui-common"),
        ("stream-video-android-compose", "streamx-video-android-compose"),
        ("stream-video-android-xml", "streamx-video-android-xml")
    ]

    # Install modules
    for module_name, artifact_id in modules:
        os.chdir(path)
        install_android_lib_module_to_local_maven(
            module_name=module_name,
            artifact_id=artifact_id,
            group_id=group_id,
            version=project_version
        )
