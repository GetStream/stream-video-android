def modify_gradle_settings():
    file_path = 'settings.gradle.kts'

    # Read the content of the file
    with open(file_path, 'r') as f:
        lines = f.readlines()

    # Add mavenLocal() at appropriate places
    for i, line in enumerate(lines):
        if line.strip() == "repositories {":
            space_count = len(line) - len(line.lstrip())
            indent = ' ' * space_count * 2
            lines.insert(i + 1, f"{indent}mavenLocal()\n")

    # Remove the line include(":stream-webrtc-android")
    lines = [line for line in lines if 'include(":stream-webrtc-android")' not in line]

    # Write the modified content back to the original file
    with open(file_path, 'w') as f:
        f.writelines(lines)

    print(f"{file_path} has been modified.")