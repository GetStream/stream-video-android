#!/usr/bin/env bash

set -e

BASEDIR=$(dirname "$(pwd)")
PROTOCOL_ROOT="$BASEDIR/protocol"
PROJECT_ROOT=$(pwd)
GENERATED_CODE_ROOT="$BASEDIR/generated"
PROTOCOL_PULL_LATEST="n"

echo "Enter path to protocol root. If the project is missing will clone the repo to the path, default: ($PROTOCOL_ROOT):"
read -r PROTOCOL_ROOT_TEMP
if [ -n "$PROTOCOL_ROOT_TEMP" ]; then
  PROTOCOL_ROOT=$PROTOCOL_ROOT_TEMP
fi

echo "Enter path to project root, default: ($PROJECT_ROOT):"
read -r PROJECT_ROOT_TEMP
if [ -n "$PROJECT_ROOT_TEMP" ]; then
  PROJECT_ROOT=$PROJECT_ROOT_TEMP
fi

echo "Enter path to generated root, default: ($GENERATED_CODE_ROOT):"
read -r GENERATED_CODE_ROOT_TEMP
if [ -n "$GENERATED_CODE_ROOT_TEMP" ]; then
  GENERATED_CODE_ROOT=$GENERATED_CODE_ROOT_TEMP
fi

echo "Do you want to pull the latest protocol if exists, default:($PROTOCOL_PULL_LATEST); (y/n):"
read -r PROTOCOL_PULL_LATEST_TEMP
if [ -n "$PROTOCOL_PULL_LATEST_TEMP" ]; then
  PROTOCOL_PULL_LATEST=$PROTOCOL_PULL_LATEST_TEMP
fi

echo Generating OpenAPI spec

if [ -d "$PROTOCOL_ROOT" ]; then
  if [ "$PROTOCOL_PULL_LATEST" = "y" ]; then
    git -C "$PROTOCOL_ROOT" pull
  fi

else
  git clone git@github.com:GetStream/protocol.git "$PROTOCOL_ROOT"
fi

CLIENT_ROOT="$GENERATED_CODE_ROOT/src/main/kotlin/org/openapitools/client"
rm -rf "${CLIENT_ROOT}"

# you can use this to use openapi templates from your laptop (openapi-generator needs to be in ~/src)
#java -jar ~/src/openapi-generator/modules/openapi-generator-cli/target/openapi-generator-cli.jar generate \
#   -i ~/src/protocol/openapi/video-openapi.yaml --additional-properties=library=jvm-retrofit2,useCoroutines,dateLibrary=threetenbp \
#   -t ~/src/openapi-generator/modules/openapi-generator/src/main/resources/kotlin-client/ \
#   -g kotlin \
#   -o "${GENERATED_CODE_ROOT}"

docker pull ghcr.io/getstream/openapi-generator:master

if [[ -n "${LOCAL_ENV}" ]]; then
  # Local environment needs an additional mount for the newly generated openapi
  # spec. We assume that you have cloned the GetStream/protocol repository and
  # the file is located in "${PROTOCOL_ROOT}/openapi/video-openapi.yaml"
  #
  # This mode is used when you want to check the generated Kotlin code from the yet
  # to be merged changes to the OpenAPI spec (YAML file) as a result of Coordinator
  # code changes. This is needed only in advanced use cases.
  if [[ -n "${OPENAPI_ROOT_USE_COORDINATOR_REPO}" ]]; then
    OPENAPI_CONFIG_ROOT="${BASEDIR}/chat/releases/"
    echo "using OpenAPI spec from the local coordinator repository (chat): ${OPENAPI_CONFIG_ROOT}"
  else
    OPENAPI_CONFIG_ROOT="${PROTOCOL_ROOT}/openapi/"
    echo "using OpenAPI spec from the local protocol repository: ${OPENAPI_CONFIG_ROOT}"
  fi

  docker run --rm \
    -v "${GENERATED_CODE_ROOT}:/local" \
    -v "${OPENAPI_CONFIG_ROOT}:/config" \
    ghcr.io/getstream/openapi-generator:master generate \
    -i "/config/video-openapi.yaml" \
    --additional-properties=library=jvm-retrofit2,useCoroutines,dateLibrary=threetenbp \
    -g kotlin \
    -o /local

else
  # This is the default used in CI/CD pipelines. Here, we pull the OpenAPI spec
  # from the protocol repository. Most of the times, this is what you would be doing.
  docker run --rm \
    -v "${GENERATED_CODE_ROOT}:/local" \
    ghcr.io/getstream/openapi-generator:master generate \
    -i https://raw.githubusercontent.com/GetStream/protocol/main/openapi/video-openapi.yaml \
    --additional-properties=library=jvm-retrofit2,useCoroutines,dateLibrary=threetenbp \
    -g kotlin \
    -o /local
fi

# delete all files in the target path of openapi to make sure we do not leave legacy code around
rm -rf "${PROJECT_ROOT}/stream-video-android-core/src/main/kotlin/org/openapitools/client"

APIS_ROOT="$CLIENT_ROOT/apis"
INFRASTRUCTURE_ROOT="$CLIENT_ROOT/infrastructure"

rm -rf "$CLIENT_ROOT/auth"
rm "$INFRASTRUCTURE_ROOT/ApiClient.kt" "$INFRASTRUCTURE_ROOT/ResponseExt.kt"
rm "$APIS_ROOT/UsersApi.kt"

API_REQUEST_REGEX="@(?:POST|DELETE|GET|PUT|PATCH)\(\"(.*?)\""
RETROFIT_IMPORTS_REGEX="(Body)|^[[:space:]]*@([^()]*)\("

find "$CLIENT_ROOT" -type f -name '*.kt' -exec sed -i '' 's@): VideoEvent()@) : VideoEvent()@g' {} \;

for FILE in "$APIS_ROOT"/*.kt; do
  echo "Processing ${FILE}"
  sed -i '' 's/kotlin.//g; s/Response<//g; s/>//g' "${FILE}"
done

for FILE in "$APIS_ROOT"/*.kt; do
  echo "Processing ${FILE}"

  grep -iE "$API_REQUEST_REGEX" "$FILE" | while read -r line; do
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
