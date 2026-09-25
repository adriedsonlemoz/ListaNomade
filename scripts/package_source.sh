#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
VERSION="$(grep '^APP_VERSION_NAME=' gradle.properties | cut -d= -f2)"
OUT="${1:-$ROOT/dist}"
mkdir -p "$OUT"
NAME="Lista-Nomade-v${VERSION}-source.zip"
rm -f "$OUT/$NAME"
zip -qr "$OUT/$NAME" . \
  -x '.git/*' '.gradle/*' '**/build/*' 'build/*' 'dist/*' '*.apk' '*.aab' '*.jks' '*.keystore' 'local.properties'
echo "$OUT/$NAME"
