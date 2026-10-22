#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="${ROOT}/.build/libXray"
mkdir -p "${WORK}" "${ROOT}/app/libs"
if ! command -v go >/dev/null 2>&1; then
  echo "Go n'est pas installé. Installez Go conformément à https://go.dev/dl/" >&2; exit 2
fi
if ! command -v python3 >/dev/null 2>&1; then
  echo "Python 3 est requis." >&2; exit 2
fi
if [[ ! -d "${WORK}/.git" ]]; then
  git clone https://github.com/XTLS/libXray.git "${WORK}"
fi
cd "${WORK}"
git fetch --tags
# Use the official upstream build script; it selects/pins the compatible Xray-core module.
python3 build/main.py android
AAR="$(find "${WORK}" -type f -name '*.aar' -print -quit)"
if [[ -z "${AAR}" ]]; then
  echo "Build libXray terminé mais aucun AAR trouvé. Inspectez les artefacts générés dans ${WORK}." >&2
  exit 3
fi
cp "${AAR}" "${ROOT}/app/libs/libXray.aar"
echo "AAR intégré : ${ROOT}/app/libs/libXray.aar"
echo "Relancez ./gradlew assembleDebug depuis la racine du projet."
