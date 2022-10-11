#!/bin/bash

set -e

REPO="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PB=$REPO/protobuf

export PATH=$REPO/.protoc/bin:$PATH

GEN_PROFILE=${1:-go}
GEN_OUTPUT=$(realpath ${2:-${PB}})
GEN_GO_IMPORT_PREFIX=${3:-github.com/GetStream/video-proto/protobuf}

mkdir -p $GEN_OUTPUT

cd $PB


PROTO_DIRS=$(find $PB -type d)

# All packages that required to generate client SDK (no server-only stuff)
CLIENT_SDK_PROTO_DIRS=$(cat << EOF
$PB/video/coordinator/broadcast_v1
$PB/video/coordinator/call_v1
$PB/video/coordinator/client_v1_rpc
$PB/video/coordinator/edge_v1
$PB/video/coordinator/event_v1
$PB/video/coordinator/member_v1
$PB/video/coordinator/push_v1
$PB/video/coordinator/utils_v1
$PB/video/coordinator/participant_v1
$PB/video/coordinator/stat_v1
$PB/video/coordinator/user_v1
$PB/video/sfu/models
$PB/video/sfu/event
$PB/video/sfu/signal_rpc
$PB/video/sfu
EOF
    )

PROTOC_ARGS=""
case $GEN_PROFILE in
  go)
    protoc=$(cat << EOF
    protoc
      -I=$PB
      --go_out=paths=source_relative:$GEN_OUTPUT
      --go-vtproto_out=paths=source_relative,features=marshal+unmarshal+size:$GEN_OUTPUT
      --twirp_out=paths=source_relative:$GEN_OUTPUT
      --validate_out=lang=go:$GEN_OUTPUT
EOF
    )
    # Map imports
    for DIR in $PROTO_DIRS; do
      DIR=$(realpath --relative-to "$PB" "$DIR")
      FILES=$(find $DIR -maxdepth 1 -name '*.proto' | sort)
      for FILE in $FILES; do
        PROTOC_ARGS="$PROTOC_ARGS --twirp_opt=M${FILE}=${GEN_GO_IMPORT_PREFIX}/${DIR} --go_opt=M${FILE}=${GEN_GO_IMPORT_PREFIX}/${DIR} --go-vtproto_opt=M${FILE}=${GEN_GO_IMPORT_PREFIX}/${DIR}"
      done
    done

    ;;

  python)
    PROTO_DIRS="$PROTO_DIRS $REPO/.protoc/include/validate/validate.proto"
    protoc=$(cat << EOF
    protoc
      -I=$PB
      -I=../.protoc/include
      --python_out=$GEN_OUTPUT
      --twirpy_out=$GEN_OUTPUT
      --mypy_out=$GEN_OUTPUT
EOF
    )
    # Map imports
    for DIR in $PROTO_DIRS; do
      DIR=$(realpath --relative-to "$PB" "$DIR")
      FILES=$(find $DIR -maxdepth 1 -name '*.proto' | sort)
      for FILE in $FILES; do
        PROTOC_ARGS="$PROTOC_ARGS --twirpy_opt=M${FILE}=${GEN_GO_IMPORT_PREFIX}/${DIR} --mypy_opt=M${FILE}=${GEN_GO_IMPORT_PREFIX}/${DIR}"
      done
    done

    ;;

  ts)
    PROTO_DIRS=$CLIENT_SDK_PROTO_DIRS
    protoc=$(cat << EOF
    protoc
      -I=$PB
      --ts_out=$GEN_OUTPUT
      --ts_opt long_type_string
      --ts_opt client_generic
      --ts_opt server_none
      --ts_opt eslint_disable
EOF
    )
    ;;

  dart)
    PROTO_DIRS=$CLIENT_SDK_PROTO_DIRS
    if ! command -v dart &> /dev/null
    then
      echo "dart is required to generate dart profile"
      exit 1
    fi

    protoc=$(cat << EOF
    protoc
      -I=$PB
      --dart_out=$GEN_OUTPUT
      --dart-twirp_out=paths=source_relative:$GEN_OUTPUT
EOF
    )

    # Map imports
    for DIR in $PROTO_DIRS; do
      DIR=$(realpath --relative-to "$PB" "$DIR")
      FILES=$(find $DIR -maxdepth 1 -name '*.proto' | sort)
      for FILE in $FILES; do
        PROTOC_ARGS="$PROTOC_ARGS --dart-twirp_opt=M${FILE}=${GEN_GO_IMPORT_PREFIX}/${DIR}"
      done
    done

    ;;

  swift)
    PROTO_DIRS=$CLIENT_SDK_PROTO_DIRS
    if ! command -v swift &> /dev/null
    then
      echo "swift is required to generate swift profile"
      exit 1
    fi

    protoc=$(cat << EOF
    protoc
      -I=$PB
      --swift_opt=FileNaming=FullPath
      --swift_out=$GEN_OUTPUT
      --swift-twirp_out=paths=source_relative:$GEN_OUTPUT
EOF
    )

    # Map imports
    for DIR in $PROTO_DIRS; do
      DIR=$(realpath --relative-to "$PB" "$DIR")
      FILES=$(find $DIR -maxdepth 1 -name '*.proto' | sort)
      for FILE in $FILES; do
        PROTOC_ARGS="$PROTOC_ARGS --swift-twirp_opt=M${FILE}=${GEN_GO_IMPORT_PREFIX}/${DIR}"
      done
    done

    ;;

  *)
    echo "unknown profile $GEN_PROFILE"
    exit 1
    ;;
esac

for DIR in $PROTO_DIRS; do
  DIR=$(realpath --relative-to "$PB" "$DIR")
  FILES=$(find $DIR -maxdepth 1 -name '*.proto' | sort)
  ARGS=$PROTOC_ARGS
  if [[ $GEN_PROFILE = "go" ]]; then
    # validate generator does not support go import mapping, so we need to specify them for each package
    VALIDATE_IMPORT_PATH=$(basename $DIR)
    ARGS="$ARGS --validate_opt=import_path=${VALIDATE_IMPORT_PATH} --validate_opt=import_prefix=${GEN_GO_IMPORT_PREFIX}/"
  fi
  if [[ ${FILES} ]]; then
    $protoc ${ARGS} ${FILES}
  fi
done

if [[ $GEN_PROFILE = "go" ]]; then
  # Replace proto.(Un)Marshal with *VT methods
  TWIRP_FILES=$(find $GEN_OUTPUT -name '*.twirp.go')
  for FILE in $TWIRP_FILES; do
    # Wasted 2 hours here (lol)
    if [[ "$OSTYPE" == "darwin"* ]]; then
      sed -i '' -e 's/respBytes, err := proto.Marshal(respContent)/respBytes, err := respContent.MarshalVT()/g' "$FILE"
      sed -i '' -e 's/if err = proto.Unmarshal(buf, reqContent); err != nil {/if err = reqContent.UnmarshalVT(buf); err != nil {/g' "$FILE"
    else
      sed -i'' -e 's/respBytes, err := proto.Marshal(respContent)/respBytes, err := respContent.MarshalVT()/g' "$FILE"
      sed -i'' -e 's/if err = proto.Unmarshal(buf, reqContent); err != nil {/if err = reqContent.UnmarshalVT(buf); err != nil {/g' "$FILE"
    fi
  done
fi
