#!/usr/bin/env bash

set -e

# Default values
REPO_URL="git@github.com:GetStream/chat.git"
REFERENCE_TYPE="branch"
REFERENCE_VALUE="feature/rahullohra/kotlin_open_api_generator"
API_SERVICE_CLASS_NAME="ProductvideoApi"
MODEL_PACKAGE="io.getstream.android.video.generated.models"
API_SERVICE_PACKAGE="io.getstream.android.video.generated.apis"
MODEL_DIR="models"
API_SERVICE_DIR="apis"
MOSHI_ADAPTER_DIR="infrastructure"
MOSHI_ADAPTER_PACKAGE="io.getstream.android.video.generated.infrastructure"
CLASSES_TO_SKIP="PrivacySettingsResponse,PrivacySettings,StopRTMPBroadcastsRequest,LocalCallAcceptedPostEvent,LocalCallRejectedPostEvent"
ANDROID_SDK="video"
KEEP_CLASSES="WSAuthMessageRequest.kt"
OUTPUT_CLIENT_PATH="./stream-video-android-core/src/main/kotlin/io/getstream/android/video/generated/"

# Parse key-value arguments
for arg in "$@"; do
  case $arg in
    --repo-url=*)
      REPO_URL="${arg#*=}"
      shift
      ;;
    --ref-type=*)
      REFERENCE_TYPE="${arg#*=}"
      shift
      ;;
    --ref-value=*)
      REFERENCE_VALUE="${arg#*=}"
      shift
      ;;
    --source-path=*)
      SOURCE_PATH="${arg#*=}"
      shift
      ;;
    --output-spec=*)
      OUTPUT_SPEC_PATH="${arg#*=}"
      shift
      ;;
    --output-client=*)
      OUTPUT_CLIENT_PATH="${arg#*=}"
      shift
      ;;
    --model-package-name=*)
      MODEL_PACKAGE="${arg#*=}"
      shift
      ;;
    --model-dir=*)
      MODEL_DIR="${arg#*=}"
      shift
      ;;
    --api-service-dir=*)
      API_SERVICE_DIR="${arg#*=}"
      shift
      ;;
    --api-service-package-name=*)
      API_SERVICE_PACKAGE="${arg#*=}"
      shift
      ;;
    --api-service-class-name=*)
      API_SERVICE_CLASS_NAME="${arg#*=}"
      shift
      ;;
    --moshi-adapters-dir=*)
      MOSHI_ADAPTER_DIR="${arg#*=}"
      shift
      ;;
    --moshi-adapters-package-name=*)
      MOSHI_ADAPTER_PACKAGE="${arg#*=}"
      shift
      ;;
    --classes-to-skip=*)
      CLASSES_TO_SKIP="${arg#*=}"
      shift
      ;;
    --androidSdk=*)
      ANDROID_SDK="${arg#*=}"
      shift
      ;;
    --keep-classes=*)
      KEEP_CLASSES="${arg#*=}"
      shift
      ;;
    *)
      echo "❌ ERROR: Unknown argument: $arg"
      exit 1
      ;;
  esac
done

# Define working directories
PROJECT_ROOT="$(dirname "$(realpath "$0")")/"
BUILD_DIR="$PROJECT_ROOT/build"
CLONE_DIR="$BUILD_DIR/openapi-generator-repo"
PROGRAM_PATH="$CLONE_DIR/cmd/chat-manager"
SPEC_FILE="$CLONE_DIR/releases/video-openapi-clientside.yaml"
OUTPUT_DIR="$CLONE_DIR/output"
SOURCE_PATH=CLONE_DIR
OUTPUT_SPEC_PATH="$CLONE_DIR/releases/video-openapi-clientside"
OUTPUT_CLIENT_ABSOLUTE_PATH="$PROJECT_ROOT$OUTPUT_CLIENT_PATH"

# Set environment variables
export APP_CONFIG_FILE="configs/test.yaml"

#mkdir -p $OUTPUT_CLIENT_PATH //Create and delete files here TODO Rahul

# Step 1: Delete/Create directory, and keep classes
if [[ ! -d "$OUTPUT_CLIENT_PATH" ]]; then
    echo "Error: Directory $OUTPUT_CLIENT_PATH does not exist."
    # Function to create a directory if it doesn't exist
    create_directory() {
        local DIR_PATH=$OUTPUT_CLIENT_PATH

        if [ -z $DIR_PATH ]; then
            echo "Usage: create_directory <directory_path>"
            return 1
        fi

        if [ ! -d "$DIR_PATH" ]; then
            echo "Directory does not exist. Creating: $DIR_PATH"
            mkdir -p $DIR_PATH
            echo "Directory created successfully."
        else
            echo "Directory already exists: $DIR_PATH"
        fi
    }
    create_directory $OUTPUT_CLIENT_PATH

else
  # Function to check if a file should be kept
  should_keep() {
      local file_name=$(basename "$1")
      for keep in "${KEEP_CLASSES[@]}"; do
         echo "file_name = $file_name, keep = $keep"
          if [[ "$file_name" == "$keep" ]]; then
              return 0  # File should be kept
          fi
      done
      return 1  # File should be deleted
  }

  # Iterate through files in the directory recursively
  find "$OUTPUT_CLIENT_PATH" -type f | while read -r file; do
        if ! should_keep "$file"; then
            rm "$file"
            echo "Deleted: $file"
        else
            echo "Kept: $file"
        fi
    done

  # Remove empty directories
  find "$OUTPUT_CLIENT_PATH" -type d -empty -delete

  echo "🧹🧹Cleanup completed!"
fi

# Step 2: Clone the repository with shallow depth
echo "🚀 Cloning repository: $REPO_URL (Type: $REFERENCE_TYPE, Value: $REFERENCE_VALUE)..."
rm -rf "$CLONE_DIR"
git clone --depth=1 --branch "$REFERENCE_VALUE" "$REPO_URL" "$CLONE_DIR"
cd "$CLONE_DIR"


# Step 3: Checkout to the correct branch, tag, or commit
if [ "$REFERENCE_TYPE" == "branch" ]; then
    git checkout "$REFERENCE_VALUE"
elif [ "$REFERENCE_TYPE" == "tag" ]; then
    git checkout "tags/$REFERENCE_VALUE"
elif [ "$REFERENCE_TYPE" == "commit" ]; then
    git checkout "$REFERENCE_VALUE"
else
    echo "❌ ERROR: Invalid reference type '$REFERENCE_TYPE'. Use 'branch', 'tag', or 'commit'."
    exit 1
fi

# Step 4: Run the Go program with OpenAPI arguments
echo "⚙️ Running OpenAPI Spec Generation..."
go run ./cmd/chat-manager openapi generate-spec \
  -products video \
  -version v1 \
  -clientside \
  -output "$OUTPUT_SPEC_PATH"

# Step 5: Generating Kt files
echo "⚙️ Running OpenAPI Client Generation..."
go run ./cmd/chat-manager openapi generate-client \
  --language kotlin \
  --spec "$OUTPUT_SPEC_PATH.yaml" \
  --api-service-class-name "$API_SERVICE_CLASS_NAME" \
  --api-service-package "$API_SERVICE_PACKAGE" \
  --api-service-dir "$API_SERVICE_DIR" \
  --model-package "$MODEL_PACKAGE" \
  --model-dir "$MODEL_DIR" \
  --moshi-adapters-dir "$MOSHI_ADAPTER_DIR" \
  --moshi-adapters-package "$MOSHI_ADAPTER_PACKAGE" \
  --classes-to-skip "$CLASSES_TO_SKIP" \
  --androidSdk "$ANDROID_SDK" \
  --output "$OUTPUT_CLIENT_ABSOLUTE_PATH"

echo "👉 Your autogenerated files are available under $OUTPUT_CLIENT_ABSOLUTE_PATH"

# Step 6: Delete files
#rm -rf "$CLONE_DIR"

echo "✅ OpenAPI Kotlin client generation completed successfully!"
