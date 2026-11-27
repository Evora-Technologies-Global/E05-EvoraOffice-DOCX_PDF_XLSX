#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JNI_DIR="$ROOT_DIR/engine/office/src/main/jniLibs/arm64-v8a"
ASSETS_DIR="$ROOT_DIR/engine/office/src/main/assets"
LOCK_FILE="$ROOT_DIR/engine/office/lok-prebuilt.sha256"

ALLOW_MISSING=false
CI_MODE=false
RUN_ID=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --allow-missing)
      ALLOW_MISSING=true
      shift
      ;;
    --ci)
      CI_MODE=true
      shift
      ;;
    --run-id)
      RUN_ID="${2:-}"
      shift 2
      ;;
    *)
      echo "Unknown flag: $1" >&2
      exit 2
      ;;
  esac
done

verify_checksums() {
  if [[ ! -f "$LOCK_FILE" ]]; then
    return 1
  fi
  local missing=0
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    local name="${line##*  }"
    if [[ ! -f "$JNI_DIR/$name" ]]; then
      missing=1
      break
    fi
  done < "$LOCK_FILE"
  if [[ "$missing" -ne 0 ]]; then
    return 1
  fi
  if command -v sha256sum >/dev/null 2>&1; then
    (cd "$JNI_DIR" && sha256sum -c "$LOCK_FILE" >/dev/null 2>&1)
  else
    (cd "$JNI_DIR" && shasum -a 256 -c "$LOCK_FILE" >/dev/null 2>&1)
  fi
}

LOCAL_ASSETS_CACHE="$ROOT_DIR/.local/lok_ci_artifacts/lok-android-runtime-assets"

sync_local_runtime_assets() {
  if [[ "$CI_MODE" == false && -d "$LOCAL_ASSETS_CACHE/program" && ! -f "$ASSETS_DIR/unpack/program/udkapi.rdb" ]]; then
    mkdir -p "$ASSETS_DIR"
    cp -R "$LOCAL_ASSETS_CACHE"/* "$ASSETS_DIR/"
    echo "Copied LibreOfficeKit runtime assets from $LOCAL_ASSETS_CACHE"
  fi
}

if verify_checksums; then
  sync_local_runtime_assets
  echo "LibreOfficeKit prebuilt .so binaries already verified in $JNI_DIR"
  exit 0
fi

LOCAL_CACHE="$ROOT_DIR/.local/lok_ci_artifacts/lok-android-arm64-16kb"
if [[ "$CI_MODE" == false && -d "$LOCAL_CACHE" && -f "$LOCAL_CACHE/liblo-native-code.so" ]]; then
  mkdir -p "$JNI_DIR"
  cp -f "$LOCAL_CACHE"/*.so "$JNI_DIR/"
  if verify_checksums; then
    sync_local_runtime_assets
    echo "Copied and verified LibreOfficeKit .so binaries from $LOCAL_CACHE"
    exit 0
  fi
fi

if ! command -v gh >/dev/null 2>&1; then
  if [[ "$ALLOW_MISSING" == true ]]; then
    echo "WARN: gh CLI unavailable and prebuilt LOK .so not found locally; continuing."
    exit 0
  fi
  echo "ERROR: gh CLI not found and LOK .so binaries missing at $JNI_DIR" >&2
  exit 1
fi

if [[ -z "$RUN_ID" ]]; then
  RUN_ID="$(gh run list --workflow "lok-android-16kb.yml" --status success --limit 1 --json databaseId --jq '.[0].databaseId // empty' 2>/dev/null || true)"
fi

if [[ -z "$RUN_ID" ]]; then
  if [[ "$ALLOW_MISSING" == true ]]; then
    echo "WARN: No successful R-01 workflow run found; continuing without LOK .so."
    exit 0
  fi
  echo "ERROR: No successful R-01 lok-android-16kb.yml run found." >&2
  exit 1
fi

TMP_DL="$(mktemp -d)"
trap 'rm -rf "$TMP_DL"' EXIT

echo "Downloading R-01 artifacts from GitHub Actions run $RUN_ID..."
gh run download "$RUN_ID" -n lok-android-arm64-16kb -D "$TMP_DL/so"
mkdir -p "$JNI_DIR"
cp -f "$TMP_DL/so"/*.so "$JNI_DIR/"

if gh run download "$RUN_ID" -n lok-android-runtime-assets -D "$TMP_DL/assets" 2>/dev/null; then
  mkdir -p "$ASSETS_DIR"
  cp -R "$TMP_DL/assets"/* "$ASSETS_DIR/"
  echo "Installed R-01 runtime assets into $ASSETS_DIR"
else
  echo "Note: lok-android-runtime-assets artifact not present on run $RUN_ID (OOXML Kotlin fallback remains active)."
fi

python3 -I "$ROOT_DIR/scripts/verify_16kb_elf.py" "$JNI_DIR"
echo "Verified R-01 .so binaries installed in $JNI_DIR"
