def override_gradle_publish(file_path):
    content = """
apply plugin: 'maven-publish'

group = PUBLISH_GROUP_ID
version = PUBLISH_VERSION

afterEvaluate {
  publishing {
    publications {
      release(MavenPublication) {
        groupId PUBLISH_GROUP_ID
        artifactId PUBLISH_ARTIFACT_ID
        version PUBLISH_VERSION
        if (project.plugins.findPlugin("com.android.library")) {
          from components.release
        } else {
          from components.java
        }
      }
    }
  }
}
"""
    with open(file_path, 'w') as file:
        file.write(content)
