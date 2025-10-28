#!/bin/bash

# URL for the maven metadata
URL="https://central.sonatype.com/repository/maven-snapshots/io/getstream/stream-video-android-bom/maven-metadata.xml"

# Make curl request and extract the latest version
echo "Fetching maven metadata..."
latest_version=$(curl -s "$URL" | sed -n 's/.*<latest>\(.*\)<\/latest>.*/\1/p')

if [ -n "$latest_version" ]; then
    echo "$latest_version"
else
    echo "No version found for $artifact"
fi