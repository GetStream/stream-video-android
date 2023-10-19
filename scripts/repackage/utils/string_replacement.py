import os
from typing import Optional, List


def replace_string_in_directory(directory_path: str, search_string: str, replace_string: str, extensions: Optional[List[str]] = None) -> None:
    """
    Recursively replace all occurrences of `search_string` with `replace_string` in all files within the directory
    that have the specified extensions.
    """
    if extensions is None:
        extensions = ['.txt', '.md', '.kt', '.java', '.xml']  # default extensions

    for subdir, _, files in os.walk(directory_path):
        for filename in files:
            if filename.endswith(tuple(extensions)):
                file_path = os.path.join(subdir, filename)
                _replace_string_in_file(file_path, search_string, replace_string)


def _replace_string_in_file(file_path: str, search_string: str, replace_string: str) -> None:
    """
    Replace all occurrences of `search_string` with `replace_string` in the specified file.
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()
    except UnicodeDecodeError:
        print(f"Failed to read {file_path} as UTF-8. Skipping...")
        return

    new_content = content.replace(search_string, replace_string)

    if content != new_content:  # Write new content only if there's a change
        with open(file_path, 'w', encoding='utf-8') as file:
            file.write(new_content)
