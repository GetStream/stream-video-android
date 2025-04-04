import subprocess
import os
import shutil

TOTAL_STEPS = 7
TOTAL_REVERT_STEPS = 4

def rename_files(project_root):

    old_webrtc_build_gradle_path = os.path.join(project_root, "webrtc-android", "build.gradle.kts")
    new_webrtc_build_gradle_path = os.path.join(project_root, "webrtc-android", "_build.gradle.kts")

    old_webrtc_settings_gradle_path = os.path.join(project_root, "webrtc-android", "settings.gradle.kts")
    new_webrtc_settings_gradle_path = os.path.join(project_root, "webrtc-android", "_settings.gradle.kts")

    old_webrtc_buildsrc_path = os.path.join(project_root, "webrtc-android", "buildSrc")
    new_webrtc_buildsrc_path = os.path.join(project_root, "webrtc-android", "_buildSrc")

    if os.path.exists(old_webrtc_build_gradle_path):
        os.rename(old_webrtc_build_gradle_path, new_webrtc_build_gradle_path)
    else:  print(f"Warning: File '{old_webrtc_build_gradle_path}' not found.")

    if os.path.exists(old_webrtc_settings_gradle_path):
        os.rename(old_webrtc_settings_gradle_path, new_webrtc_settings_gradle_path)
    else:  print(f"Warning: File '{old_webrtc_settings_gradle_path}' not found.")

    if os.path.exists(old_webrtc_buildsrc_path):
        os.rename(old_webrtc_buildsrc_path, new_webrtc_buildsrc_path)
    else:  print(f"Warning: File '{old_webrtc_buildsrc_path}' not found.")

def refactor_imports(project_root):
    build_gradle_paths = [
        os.path.join(project_root, "webrtc-android", "stream-webrtc-android", "build.gradle.kts"),
        os.path.join(project_root, "webrtc-android", "stream-webrtc-android-ui", "build.gradle.kts")
    ]
    for file_path in build_gradle_paths:
        try:
            with open(file_path, 'r') as file:
                file_content = file.read()

                file_content = file_content.replace("Configurations.", "Configuration.")
                file_content = file_content.replace("import io.getstream.Configurations", "import io.getstream.video.android.Configuration")

                with open(file_path, 'w') as file:
                    file.write(file_content)
        except Exception as e:
            print(f"An unexpected error occurred for file '{file_path}': {e}")

def rewrite_dependencies(project_root):
     build_gradle_paths = [
            os.path.join(project_root, "webrtc-android", "stream-webrtc-android-ui", "build.gradle.kts")
        ]

     for file_path in build_gradle_paths:
            try:
                with open(file_path, 'r') as file:
                    file_content = file.read()

                    file_content = file_content.replace(":stream-webrtc-android", ":webrtc-android:stream-webrtc-android")

                    with open(file_path, 'w') as file:
                        file.write(file_content)
            except Exception as e:
                print(f"An unexpected error occurred for file '{file_path}': {e}")

def delete_directory(directory_path):
    if os.path.exists(directory_path):
        try:
            shutil.rmtree(directory_path)
            print(f"Directory '{directory_path}' and its contents deleted successfully.")
        except FileNotFoundError:
            print(f"Error: Directory '{directory_path}' not found.")
        except OSError as e:
            print(f"Error: Could not delete directory '{directory_path}'. {e}")

def update_compile_options(project_root):
    build_gradle_paths = [            
            os.path.join(project_root, "webrtc-android", "stream-webrtc-android", "build.gradle.kts"),
            os.path.join(project_root, "webrtc-android", "stream-webrtc-android-ui", "build.gradle.kts")
        ]

    for file_path in build_gradle_paths:
        try:
            with open(file_path, 'r') as file:
                file_content = file.read()

                file_content = file_content.replace("JavaVersion.VERSION_11", "JavaVersion.VERSION_21")

                with open(file_path, 'w') as file:
                    file.write(file_content)
        except Exception as e:
            print(f"An unexpected error occurred for file '{file_path}': {e}")


def integrate_webrtc_as_submodule(project_root):
    """
    Integrates the webrtc-android repository as a submodule into the given project.

    Args:
        project_root (str): The path to the root directory of the Stream Video SDK project.
    """

    webrtc_repo_url = "https://github.com/GetStream/webrtc-android.git"
    webrtc_submodule_path = os.path.join(project_root, "webrtc-android")

    try:
        # Add the submodule
        subprocess.run(
            ["git", "submodule", "add", webrtc_repo_url, "webrtc-android"],
            cwd=project_root,
            check=True,
        )

        # Initialize and update the submodule
        subprocess.run(["git", "submodule", "init"], cwd=project_root, check=True)
        subprocess.run(["git", "submodule", "update"], cwd=project_root, check=True)

        print(f'[1/{TOTAL_STEPS}] Completed: Add submodule of webrtc-android')

        # Modify settings.gradle
        settings_gradle_path = os.path.join(project_root, "settings.gradle.kts")
        if os.path.exists(settings_gradle_path):
            with open(settings_gradle_path, "r") as f:
                settings_gradle_content = f.readlines()

            if not any("include ':stream-webrtc-android'" in line for line in settings_gradle_content):
                settings_gradle_content.append('''\ninclude(":webrtc-android:stream-webrtc-android")''')

            if not any("include ':stream-webrtc-android-ui'" in line for line in settings_gradle_content):
                settings_gradle_content.append('''\ninclude(":webrtc-android:stream-webrtc-android-ui")''')

            with open(settings_gradle_path, "w") as f:
                f.writelines(settings_gradle_content)

            print(f'[2/{TOTAL_STEPS}] Completed: Update settings.gradle.kts to include new modules')
        else:
            raise FileNotFoundError(f"settings.gradle.kts not found at {settings_gradle_path}")


        # Modify app/build.gradle
        app_build_gradle_path = os.path.join(project_root, "stream-video-android-core", "build.gradle.kts")
        if os.path.exists(app_build_gradle_path):
            with open(app_build_gradle_path, "r") as f:
                app_build_gradle_content = f.read()

            # Replace the lines
            app_build_gradle_content = app_build_gradle_content.replace(
                'api(libs.stream.webrtc)',
                'api(project(":webrtc-android:stream-webrtc-android"))'
            )
            app_build_gradle_content = app_build_gradle_content.replace(
                'api(libs.stream.webrtc.ui)',
                'api(project(":webrtc-android:stream-webrtc-android-ui"))'
            )

            with open(app_build_gradle_path, "w") as f:
                f.write(app_build_gradle_content)

        else:
            raise FileNotFoundError(f"stream-video-android-core/build.gradle.kts not found at {app_build_gradle_path}")
        print(f'[3/{TOTAL_STEPS}] Completed: Update build.gradle.kts of stream-video-android-core.')

        rename_files(project_root)
        print(f'[4/{TOTAL_STEPS}] Completed: Rename webrtc-android internal conflicting gradle files with _<existing file>')
        refactor_imports(project_root)
        print(f'[5/{TOTAL_STEPS}] Completed: Refactor import statements of webrtc-android\'s gradle files')
        rewrite_dependencies(project_root)
        print(f'[6/{TOTAL_STEPS}] Completed: Rewrite dependencies of webrtc-android\'s build.gradle files')
        update_compile_options(project_root)
        print(f'[7/{TOTAL_STEPS}] Completed: Update compile options of webrtc-android\'s build.gradle files')

    except subprocess.CalledProcessError as e:
        print(f"Error integrating webrtc-android: {e}")
    except FileNotFoundError as e:
        print(f"Error: File not found: {e}")
    except Exception as e:
      print(f"An unexpected error occurred: {e}")

def revert_webrtc_submodule_integration(project_root):
    """
    Reverts the integration of webrtc-android as a submodule.

    Args:
        project_root (str): The path to the root directory of the Stream Video SDK project.
        webrtc_maven_artifact (str): The maven artifact string that was removed.
    """

    try:
        # Check if the submodules are known to Git
        try:
            subprocess.run(
                ["git", "ls-files", "webrtc-android"],
                cwd=project_root,
                check=True,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )            
        except Exception as e:
            print(f"An unexpected error occurred: {e}")

        try:
            subprocess.run(
                ["git", "ls-files", "webrtc-android-ui"],
                cwd=project_root,
                check=True,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
        except Exception as e:
            print(f"An unexpected error occurred: {e}")

        
        try:
            subprocess.run(
                ["git", "submodule", "deinit", "-f", "webrtc-android"],
                cwd=project_root,
                check=True,
            )
        except Exception as e:
            print(f"An unexpected error occurred: {e}")

        try:
            subprocess.run(
                ["git", "rm", "-f", "webrtc-android"],
                cwd=project_root,
                check=True,
            )
        except Exception as e:
            print(f"An unexpected error occurred: {e}")

        try:
            subprocess.run(
                ["git", "rm", "--cached", "webrtc-android"],
                cwd=project_root,
                check=True,
            )
        except Exception as e:
            print(f"An unexpected error occurred: {e}")

        webrtc_modules_path = os.path.join(project_root, ".git", "modules", "webrtc-android")

        if os.path.exists(webrtc_modules_path):
            subprocess.run(
                ["rm", "-rf", webrtc_modules_path],
                cwd=project_root,
                check=True,
            )

        gitmodules_path = os.path.join(project_root,".git", ".gitmodules")
        if os.path.exists(gitmodules_path):
            try:
                subprocess.run(
                    ["git", "rm", "-f", gitmodules_path],
                    cwd=project_root,
                    check=True,
                )
            except Exception as e:
                print(f"An unexpected error occurred: {e}")
            
        print(f"[1/{TOTAL_REVERT_STEPS}] Completed: Changes in git submodule")
        
        # Modify settings.gradle
        settings_gradle_path = os.path.join(project_root, "settings.gradle.kts")
        if os.path.exists(settings_gradle_path):
            with open(settings_gradle_path, "r") as f:
                settings_gradle_content = f.readlines()

            settings_gradle_content = [
                line for line in settings_gradle_content if 'include(":webrtc-android:stream-webrtc-android")' not in line and 'include(":webrtc-android:stream-webrtc-android-ui")' not in line
            ]

            with open(settings_gradle_path, "w") as f:
                f.writelines(settings_gradle_content)
        
        print(f"[2/{TOTAL_REVERT_STEPS}] Completed: Changes in git submodule")
        
        # Modify app/build.gradle
        app_build_gradle_path = os.path.join(project_root, "stream-video-android-core", "build.gradle.kts")
        if os.path.exists(app_build_gradle_path):
            with open(app_build_gradle_path, "r") as f:
                app_build_gradle_content = f.read()

            app_build_gradle_content = app_build_gradle_content.replace(
                'api(project(":webrtc-android:stream-webrtc-android"))',
                "api(libs.stream.webrtc)"
            )

            app_build_gradle_content = app_build_gradle_content.replace(
                'api(project(":webrtc-android:stream-webrtc-android-ui"))',
                "api(libs.stream.webrtc.ui)"
            )

            with open(app_build_gradle_path, "w") as f:
                f.write(app_build_gradle_content)
        
        print(f"[3/{TOTAL_REVERT_STEPS}] Completed: Revert the changes in stream-video-android-core/build.gradle.kts")        
        
        delete_directory(os.path.join(project_root, "webrtc-android"))
        print(f"[4/{TOTAL_REVERT_STEPS}] Completed: Delete the webrtc-android directory")
        
    except subprocess.CalledProcessError as e:
        print(f"Error reverting webrtc-android integration: {e}")
    except FileNotFoundError as e:
        print(f"Error: File not found: {e}")
    except Exception as e:
      print(f"An unexpected error occurred: {e}")

if __name__ == "__main__":
    import sys

    project_root = "./"  # Replace with your project path
    webrtc_maven_artifact = 'api(libs.stream.webrtc)'  # Replace with the correct maven artifact string.
    webrtc_ui_maven_artifact = 'api(libs.stream.webrtc.ui)'  # Replace with the correct maven artifact string.

    if len(sys.argv) > 1:
        if sys.argv[1] == "integrate":
            integrate_webrtc_as_submodule(project_root)
        elif sys.argv[1] == "revert":
            revert_webrtc_submodule_integration(project_root)
        else:
            print("Usage: python script_name.py [integrate|revert]")
    else:
        print("Usage: python script_name.py [integrate|revert]")