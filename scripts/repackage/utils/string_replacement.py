import os


def replace_string_in_file(file_path, search_string, replace_string):
    """
    Replace all occurrences of `search_string` with `replace_string` in the specified file.
    """
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()

    new_content = content.replace(search_string, replace_string)

    if content != new_content:  # Write new content only if there's a change
        with open(file_path, 'w', encoding='utf-8') as file:
            file.write(new_content)


def replace_string_in_directory(directory_path, search_string, replace_string):
    """
    Recursively replace all occurrences of `search_string` with `replace_string` in all files within the directory.
    """
    for subdir, _, files in os.walk(directory_path):
        for filename in files:
            file_path = os.path.join(subdir, filename)
            replace_string_in_file(file_path, search_string, replace_string)