#!/usr/bin/env bash

BASEDIR=$(dirname "$(pwd)")
PROTOCOL_ROOT="$BASEDIR/protocol"
PROJECT_ROOT=$(pwd)
GENERATED_CODE_ROOT="$BASEDIR/generated"
PROTOCOL_PULL_LATEST="y"

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
    git fetch "$PROTOCOL_ROOT"
  fi

else
  git clone git@github.com:GetStream/protocol.git "$PROTOCOL_ROOT"
fi

openapi-generator generate -i "$PROTOCOL_ROOT/openapi/video-openapi.yaml" -g kotlin -o "$GENERATED_CODE_ROOT" --additional-properties=library=jvm-retrofit2,useCoroutines --skip-validate-spec

CLIENT_ROOT="$GENERATED_CODE_ROOT/src/main/kotlin/org/openapitools/client"
APIS_ROOT="$CLIENT_ROOT/apis"
INFRASTRUCTURE_ROOT="$CLIENT_ROOT/infrastructure"

rm -rf "$CLIENT_ROOT/auth"
rm "$INFRASTRUCTURE_ROOT/ApiClient.kt" "$INFRASTRUCTURE_ROOT/ResponseExt.kt"
rm "$APIS_ROOT/UsersApi.kt"

API_REQUEST_REGEX="@(?:POST|DELETE|GET|PUT)\(\"(.*?)\""

for FILE in $APIS_ROOT/*.kt; do
  echo Processing $FILE
  sed -i '' 's/kotlin.//g; s/Response<//g; s/>//g' $FILE

  grep -iE "$API_REQUEST_REGEX" "$FILE" | while read -r line; do
    UPDATED_REQUEST=$(sed 's/("/("\/video\//g' <<<$line)
    ESCAPED_LINE=$(printf '%s\n' "$line" | sed -e 's/[\/&]/\\&/g')
    ESCAPED_REQUEST=$(printf '%s\n' "$UPDATED_REQUEST" | sed -e 's/[\/&]/\\&/g')
    sed -i '' "s/$ESCAPED_LINE/$ESCAPED_REQUEST/g" "$FILE"
  done

done

echo Copying models and services

cp -r "$CLIENT_ROOT/" "$PROJECT_ROOT/stream-video-android-core/src/main/kotlin/org/openapitools/client"
cp -r "$PROTOCOL_ROOT/protobuf/." "$PROJECT_ROOT/stream-video-android-core/src/main/proto"

echo Done!
