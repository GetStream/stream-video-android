#!/bin/bash

# Run the Gradle task to get the artifact list and process the output line by line
./gradlew -q printAllArtifacts | while IFS= read -r artifact; do
    # Extract groupId, artifactId, and snapshot version from the artifact string
    groupId=$(echo $artifact | cut -d: -f1)
    artifactId=$(echo $artifact | cut -d: -f2)

    # Format the URL for the maven-metadata.xml file in the Nexus repository
    url="https://central.sonatype.com/repository/maven-snapshots/io/getstream/stream-video-android-bom/maven-metadata.xml"

    # Fetch the maven-metadata.xml using curl and extract the latest release version using sed
    latest_version=$(curl -s "url" | sed -n 's/.*<latest>\(.*\)<\/latest>.*/\1/p')

    # Print the result with the latest stable version
    if [ -n "$latest_version" ]; then
        echo "$groupId:$artifactId:$latest_version"
    else
        echo "No version found for $artifact"
    fi
done
