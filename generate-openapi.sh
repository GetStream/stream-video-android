#!/usr/bin/env bash

set -e

BASEDIR=$(dirname "$(pwd)")
PROTOCOL_ROOT="$BASEDIR/protocol"
PROJECT_ROOT=$(pwd)
GENERATED_CODE_ROOT="$BASEDIR/generated"
PROTOCOL_PULL_LATEST="n"
OPENAPI_SPEC_PATH="https://raw.githubusercontent.com/GetStream/protocol/main/openapi/video-openapi-clientside.yaml"
DOCKER_EXTRA_MOUNT=""

if [ ! -z $1 ]
then
    DOCKER_EXTRA_MOUNT="-v $(dirname $1):/manifest"
    OPENAPI_SPEC_PATH="/manifest/$(basename $1)"
fi

CLIENT_ROOT="$GENERATED_CODE_ROOT/src/main/kotlin/org/openapitools/client"
rm -rf "${CLIENT_ROOT}"

docker pull ghcr.io/getstream/openapi-generator:master

docker run --rm \
  -v "${GENERATED_CODE_ROOT}:/local" ${DOCKER_EXTRA_MOUNT} \
  ghcr.io/getstream/openapi-generator:master generate \
  -i "$OPENAPI_SPEC_PATH" \
  --additional-properties=library=jvm-retrofit2,useCoroutines,dateLibrary=threetenbp \
  -g kotlin \
  -o /local

# delete all files in the target path of openapi to make sure we do not leave legacy code around
rm -rf "${PROJECT_ROOT}/stream-video-android-core/src/main/kotlin/org/openapitools/client"

APIS_ROOT="$CLIENT_ROOT/apis"
INFRASTRUCTURE_ROOT="$CLIENT_ROOT/infrastructure"

rm -rf "$CLIENT_ROOT/auth"
rm "$INFRASTRUCTURE_ROOT/ApiClient.kt" "$INFRASTRUCTURE_ROOT/ResponseExt.kt"

API_REQUEST_REGEX="@(?:POST|DELETE|GET|PUT|PATCH)\(\"(.*?)\""
RETROFIT_IMPORTS_REGEX="(Body)|^[[:space:]]*@([^()]*)\("

find "$CLIENT_ROOT" -type f -name '*.kt' -exec sed -i '' 's@): VideoEvent()@) : VideoEvent()@g' {} \;

for FILE in "$APIS_ROOT"/*.kt; do
  echo "Processing ${FILE}"
  sed -i '' 's/kotlin.//g; s/Response<//g; s/>//g' "${FILE}"
done

for FILE in "$APIS_ROOT"/*.kt; do
  echo "Processing ${FILE}"

  grep -iE "${API_REQUEST_REGEX}" "$FILE" | while read -r line; do
    # adds the /video prefix to the URI
    UPDATED_REQUEST=$(sed 's/("/("\/video\//g' <<<$line)
    ESCAPED_LINE=$(printf '%s\n' "$line" | sed -e 's/[\/&]/\\&/g')
    ESCAPED_REQUEST=$(printf '%s\n' "$UPDATED_REQUEST" | sed -e 's/[\/&]/\\&/g')
    sed -i '' "s/$ESCAPED_LINE/$ESCAPED_REQUEST/g" "$FILE"
  done

  REPLACEMENT="\n        &"
  sed -i '' "s/@Path/$REPLACEMENT/g; s/@Body/$REPLACEMENT/g; s/@Query/$REPLACEMENT/g" "$FILE"
  sed -i '' 's/):/\n    &/g' "$FILE"

  CLEANUP_STRINGS=("kotlin." "Response<" ">" "import org.openapitools.client.infrastructure.CollectionFormats.*"  "import retrofit2.Response" "import okhttp3.RequestBody" "import com.squareup.moshi.Json" "import org.openapitools.client.models.APIError")
  for cleanup_string in "${CLEANUP_STRINGS[@]}"; do
    sed -i '' "s/$cleanup_string//g" "$FILE"
  done

  RETROFIT_IMPORTS=($(grep -iE -o "$RETROFIT_IMPORTS_REGEX" "$FILE" | sed 's/@//;s/[()]//g' | tr ' ' '\n' | sort -u))
  PREPARED_IMPORTS=""
  for retrofit_import in "${RETROFIT_IMPORTS[@]}"; do
    PREPARED_IMPORTS+="import retrofit2.http.$retrofit_import\n"
  done

  sed -i '' "s/import retrofit2\.http\.\*/$PREPARED_IMPORTS/g" "$FILE"
done

# copy the massaged openapi generated code in the right path
cp -r "${CLIENT_ROOT}/" "${PROJECT_ROOT}/stream-video-android-core/src/main/kotlin/org/openapitools/client"

# delete all files in the target path of proto to avoid leaving legacy stuff around
rm -rf ${PROJECT_ROOT}/stream-video-android-core/src/main/proto
cp -r "${PROTOCOL_ROOT}/protobuf/." "${PROJECT_ROOT}/stream-video-android-core/src/main/proto"

# move to the project root and run spotlessApply to reformat generated codes.
cd "${PROJECT_ROOT}"
#./gradlew spotlessApply
