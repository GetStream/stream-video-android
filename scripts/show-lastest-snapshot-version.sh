#!/bin/bash

# URL for the maven metadata
URL="https://central.sonatype.com/repository/maven-snapshots/io/getstream/stream-video-android-bom/maven-metadata.xml"

# Make curl request and extract the latest version
echo "Fetching maven metadata..."
latest_version=$(curl -s "$URL" | sed -n 's/.*<latest>\(.*\)<\/latest>.*/\1/p')

# Check if we got a result
if [ -z "$latest_version" ]; then
    echo "Error: Could not extract latest version"
    exit 1
fi

echo "Latest version: $latest_version"