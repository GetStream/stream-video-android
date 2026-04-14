#!/usr/bin/env bash

set -e

REPO_URL="git@github.com:GetStream/protocol.git"
REFERENCE_TYPE="branch"
REFERENCE_VALUE="main"

PROJECT_ROOT="$(dirname "$(realpath "$0")")/"
BUILD_DIR="$PROJECT_ROOT/build"
CLONE_DIR="$BUILD_DIR/protocol-repo"
OUTPUT_CLIENT_PATH="$PROJECT_ROOT/stream-video-android-core/src/main/proto/video/sfu/"

# Step 1: Delete OUTPUT_CLIENT_PATH if exists else create an empty directory
echo "🧹 Preparing output directory: $OUTPUT_CLIENT_PATH"
rm -rf "$OUTPUT_CLIENT_PATH"
mkdir -p "$OUTPUT_CLIENT_PATH"

# Step 2: Clone the repository with shallow depth
echo "🚀 Cloning repository: $REPO_URL (Type: $REFERENCE_TYPE, Value: $REFERENCE_VALUE)..."
rm -rf "$CLONE_DIR"
mkdir -p "$BUILD_DIR"
git clone --depth=1 --branch "$REFERENCE_VALUE" "$REPO_URL" "$CLONE_DIR"

cd "$CLONE_DIR"

# Step 3: Checkout to the correct branch, tag, or commit
if [ "$REFERENCE_TYPE" == "branch" ]; then
    git checkout "$REFERENCE_VALUE"
elif [ "$REFERENCE_TYPE" == "tag" ]; then
    git fetch --tags
    git checkout "tags/$REFERENCE_VALUE"
elif [ "$REFERENCE_TYPE" == "commit" ]; then
    git fetch --depth=1 origin "$REFERENCE_VALUE"
    git checkout "$REFERENCE_VALUE"
else
    echo "❌ ERROR: Invalid reference type '$REFERENCE_TYPE'. Use 'branch', 'tag', or 'commit'."
    exit 1
fi

# Step 4: Copy content from CLONE_DIR/protobuf/video/sfu to OUTPUT_CLIENT_PATH
echo "📦 Copying proto files..."
SOURCE_PROTO_PATH="$CLONE_DIR/protobuf/video/sfu"

if [ ! -d "$SOURCE_PROTO_PATH" ]; then
    echo "❌ ERROR: Source proto directory does not exist: $SOURCE_PROTO_PATH"
    exit 1
fi

cp -R "$SOURCE_PROTO_PATH/"* "$OUTPUT_CLIENT_PATH"

# Step 5: Run Spotless (from project root, not inside cloned repo)
echo "✨ Running Spotless..."
cd "$PROJECT_ROOT"
./gradlew spotlessApply

# Step 6: Delete CLONE_DIR
echo "🗑 Cleaning up cloned repo..."
rm -rf "$CLONE_DIR"

echo "✅ Done."