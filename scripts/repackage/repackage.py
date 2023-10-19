import argparse
import os

from utils.project_configuration import extract_version_name_and_artifact_group
from utils.repackage_video_sdk import repackage_and_install_video_sdk
from utils.maven import install_file_to_local_maven

from utils.repackage_webrtc_android import repackage_and_install_webrtc_android


def repackage_and_install(
        repackaged_webrtc: str,
        webrtc_android: str
):
    original_cwd = os.getcwd()

    # Install webrtc aar
    os.chdir(webrtc_android)
    configuration_path = os.path.join(
        "buildSrc", "src", "main", "kotlin", "io", "getstream", "Configurations.kt"
    )
    webrtc_android_version, group_id = extract_version_name_and_artifact_group(configuration_path)
    install_file_to_local_maven(
        file_path=repackaged_webrtc,
        group_id=group_id,
        artifact_id="streamx-webrtc-android",
        version=webrtc_android_version,
        packaging="aar"
    )

    # Repackage webrtc_android project
    repackage_and_install_webrtc_android(webrtc_android)

    # Repackage video_sdk project
    os.chdir(original_cwd)
    video_sdk_path = os.path.dirname(os.path.dirname(original_cwd))
    repackage_and_install_video_sdk(video_sdk_path, webrtc_android_version)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Install Library B to local Maven repository.")
    parser.add_argument('--repackaged_webrtc', required=True,
                        help='Path to AAR file with repackaged webrtc')
    parser.add_argument('--webrtc_android', required=True, help='Path to webrtc-android repo')

    args = parser.parse_args()

    repackaged_webrtc = args.repackaged_webrtc
    webrtc_android = args.webrtc_android
    print(f">>> repackaged_webrtc: {repackaged_webrtc}")
    print(f">>> webrtc_android: {webrtc_android}")

    repackage_and_install(
        repackaged_webrtc,
        webrtc_android
    )
