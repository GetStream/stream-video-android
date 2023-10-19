import argparse
import os

import sys
print(sys.path)

from utils.repackage_video_sdk import repackage_and_install_video_sdk
from utils.maven import install_file_to_local_maven

from utils.repackage_webrtc_android import repackage_and_install_webrtc_android


def repackage_and_install(
        repackaged_webrtc: str,
        repackaged_webrtc_version: str,
        webrtc_android: str
):
    install_file_to_local_maven(
        file_path=repackaged_webrtc,
        group_id="io.getstream",
        artifact_id="streamx-webrtc-android",
        version=repackaged_webrtc_version,
        packaging="aar"
    )
    original_cwd = os.getcwd()
    repackage_and_install_webrtc_android(webrtc_android, repackaged_webrtc_version)
    os.chdir(original_cwd)
    video_sdk_path = os.path.dirname(os.path.dirname(original_cwd))  # Move up two levels
    print(video_sdk_path)
    repackage_and_install_video_sdk(video_sdk_path, repackaged_webrtc_version)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Install Library B to local Maven repository.")
    parser.add_argument('--repackaged_webrtc', required=True,
                        help='Path to AAR file with repackaged webrtc')
    parser.add_argument('--repackaged_webrtc_version', required=True,
                        help='Repackaged webrtc version')
    parser.add_argument('--webrtc_android', required=True, help='Path to webrtc-android repo')

    args = parser.parse_args()

    repackage_and_install(
        args.repackaged_webrtc,
        args.repackaged_webrtc_version,
        args.webrtc_android
    )
