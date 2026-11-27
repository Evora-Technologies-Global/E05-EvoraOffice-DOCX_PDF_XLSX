#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DISCLOSURE_DIR="$ROOT_DIR/engine/office/mplv2-disclosure"
WORKFLOW_FILE="$ROOT_DIR/.github/workflows/lok-android-16kb.yml"
LOCK_FILE="$ROOT_DIR/engine/office/lok-prebuilt.sha256"
SETTINGS_FILE="$ROOT_DIR/feature/settings/src/main/kotlin/com/evora/technologies/office/feature/settings/SettingsRoute.kt"
OUT_DIR="${1:-$ROOT_DIR/.local/release_kit}"
ARCHIVE_PATH="$OUT_DIR/evora-office-v1.0.0-mplv2-lok-disclosure.tar.gz"

for required in \
  "$DISCLOSURE_DIR/autogen.input" \
  "$DISCLOSURE_DIR/lo-core-commit.txt" \
  "$DISCLOSURE_DIR/lok_patches.diff" \
  "$WORKFLOW_FILE" \
  "$LOCK_FILE" \
  "$SETTINGS_FILE"; do
  if [[ ! -f "$required" ]]; then
    echo "ERROR: Required MPLv2 disclosure input missing: $required" >&2
    exit 1
  fi
done

COMMIT_SHA="$(tr -d '[:space:]' < "$DISCLOSURE_DIR/lo-core-commit.txt")"
mkdir -p "$OUT_DIR"
STAGING_DIR="$(mktemp -d)"
trap 'rm -rf "$STAGING_DIR"' EXIT

PKG_ROOT="$STAGING_DIR/evora-office-mplv2-disclosure"
mkdir -p "$PKG_ROOT/mplv2-disclosure" "$PKG_ROOT/workflows" "$PKG_ROOT/scripts"

cp "$DISCLOSURE_DIR/autogen.input" "$PKG_ROOT/mplv2-disclosure/"
cp "$DISCLOSURE_DIR/lo-core-commit.txt" "$PKG_ROOT/mplv2-disclosure/"
cp "$DISCLOSURE_DIR/lok_patches.diff" "$PKG_ROOT/mplv2-disclosure/"
cp "$LOCK_FILE" "$PKG_ROOT/"
cp "$WORKFLOW_FILE" "$PKG_ROOT/workflows/"
cp "$ROOT_DIR/scripts/verify_16kb_elf.py" "$PKG_ROOT/scripts/"
cp "$ROOT_DIR/scripts/fetch_lok_prebuilt.sh" "$PKG_ROOT/scripts/"

cat > "$PKG_ROOT/NOTICE_MPLV2_LGPLV3.txt" <<EOF
Evora Office (com.evora.technologies.office) v1.0.0 (versionCode 100)
LibreOfficeKit Android Core (MPLv2 / LGPLv3) Corresponding Source & Build Recipe Disclosure

1. Upstream Source Repository:
   https://github.com/LibreOffice/core.git
   Pinned Commit SHA: $COMMIT_SHA (branch target: libreoffice-25-8)

2. Build Recipe & Patches Included:
   - mplv2-disclosure/autogen.input (LibreOfficeAndroidAarch64 NDK r28 API 26 configuration)
   - mplv2-disclosure/lok_patches.diff (16 KB ELF page-size alignment flags in android/Bootstrap/Makefile.shared)
   - workflows/lok-android-16kb.yml (Reproducible Ubuntu 24.04 + NDK 28.2.13676358 cross-compile workflow)
   - lok-prebuilt.sha256 (SHA-256 lockfile for all 14 ARM64 .so binaries)
   - scripts/verify_16kb_elf.py & scripts/fetch_lok_prebuilt.sh

3. Proprietary Boundary:
   Evora Office invokes LibreOfficeKit solely across the documented JNI boundary
   (org.libreoffice.kit.* and com.evora.technologies.office.engine.office.*).
   Under MPL-2.0 Section 3.3 (Larger Work), proprietary Kotlin/Jetpack Compose application
   modules remain closed-source while all LibreOffice Core modifications and build recipes
   are disclosed openly at:
   https://github.com/Evora-Technologies-Global/E05-EvoraOffice-DOCX_PDF_XLSX
EOF

tar -czf "$ARCHIVE_PATH" -C "$STAGING_DIR" "evora-office-mplv2-disclosure"
if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$ARCHIVE_PATH" > "$ARCHIVE_PATH.sha256"
else
  shasum -a 256 "$ARCHIVE_PATH" > "$ARCHIVE_PATH.sha256"
fi

echo "Created MPLv2 disclosure archive: $ARCHIVE_PATH"
cat "$ARCHIVE_PATH.sha256"
