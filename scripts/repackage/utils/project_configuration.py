import re


def extract_version_name_and_artifact_group(configuration_path):
    with open(configuration_path, 'r') as file:
        content = file.read()

        # Extract raw versionName string
        version_raw_match = re.search(r'const val versionName = "(.*?)"', content)
        version_raw = version_raw_match.group(1) if version_raw_match else None

        # Extract version components
        major_version_match = re.search(r'const val majorVersion = (\d+)', content)
        major_version = major_version_match.group(1) if major_version_match else None

        minor_version_match = re.search(r'const val minorVersion = (\d+)', content)
        minor_version = minor_version_match.group(1) if minor_version_match else None

        patch_version_match = re.search(r'const val patchVersion = (\d+)', content)
        patch_version = patch_version_match.group(1) if patch_version_match else None

        # Resolve versionName
        version_name = version_raw\
            .replace("$majorVersion", major_version)\
            .replace("$minorVersion", minor_version)\
            .replace("$patchVersion", patch_version)

        # Extract artifactGroup
        group_match = re.search(r'const val artifactGroup = "(.*?)"', content)
        artifact_group = group_match.group(1) if group_match else None

    return version_name, artifact_group