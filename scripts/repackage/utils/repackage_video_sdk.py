import os

from maven import install_android_lib_module_to_local_maven
from project_configuration import extract_version_name_and_artifact_group
from string_replacement import replace_string_in_directory


def repackage_and_install_video_sdk(path: str, repackaged_webrtc_version: str):
    os.chdir(path)

    configuration_path = os.path.join(
        "buildSrc", "src", "main", "kotlin", "io", "getstream", "video", "android",
        "Configuration.kt"
    )
    project_version, group_id = extract_version_name_and_artifact_group(configuration_path)
    print(f"VideoSDK version: {project_version}")
    print(f"VideoSDK groupId: {group_id}")

    # Repackage
    replace_string_in_directory(
        directory_path=path,
        search_string="org.webrtc",
        replace_string="io.getstream.webrtc"
    )

    # Modify settings.gradle file
    os.chdir(path)
    _modify_settings_gradle()

    # Modify libs.versions.toml file
    os.chdir(path)
    _modify_gradle_libs_version(repackaged_webrtc_version)

    # Modify build.gradle files
    os.chdir(path)
    _modify_build_gradle_files(path)

    # Install modules
    os.chdir(path)
    _install_modules(path=path, group_id=group_id, project_version=project_version)

def _modify_settings_gradle():
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

    print(f"mavenLocal() was added to {file_path}.")


def _modify_gradle_libs_version(repackaged_webrtc_version: str):
    file_path = os.path.join("gradle", "libs.versions.toml")

    # streamWebRTC = "1.1.0"
    with open(file_path, 'r') as file:
        lines = file.readlines()

    with open(file_path, 'w') as file:
        for line in lines:
            if 'streamWebRTC' in line:
                line = line.split('=')[0] + '= "' + repackaged_webrtc_version + '"\n'
            file.write(line)

    # Read the content of the file
    with open(file_path, 'r') as f:
        content = f.read()

    # Perform replacements
    content = content.replace(
        'stream-webrtc = { group = "io.getstream", name = "stream-webrtc-android", version.ref = "streamWebRTC" }',
        'stream-webrtc = { group = "io.getstream", name = "streamx-webrtc-android", version.ref = "streamWebRTC" }'
    )
    content = content.replace(
        'stream-webrtc-ui = { group = "io.getstream", name = "stream-webrtc-android-ui", version.ref = "streamWebRTC" }',
        'stream-webrtc-ui = { group = "io.getstream", name = "streamx-webrtc-android-ui", version.ref = "streamWebRTC" }'
    )
    content = content.replace(
        'stream-webrtc-ktx = { group = "io.getstream", name = "stream-webrtc-android-ktx", version.ref = "streamWebRTC" }',
        'stream-webrtc-ktx = { group = "io.getstream", name = "streamx-webrtc-android-ktx", version.ref = "streamWebRTC" }'
    )

    # Write the modified content back to the original file
    with open(file_path, 'w') as f:
        f.write(content)

    print(f"{file_path} has been modified.")


def _modify_build_gradle_files(path: str):
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
    for module_name, _ in modules:
        os.chdir(path)
        _modify_build_gradle(module_name)


def _modify_build_gradle(module_name: str):
    file_path = os.path.join(module_name, "build.gradle.kts")

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
