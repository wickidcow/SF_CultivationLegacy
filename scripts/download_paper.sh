#!/usr/bin/env bash
# Fetch a stable build for the requested version only; no fallback to another version.
set -euo pipefail
VERSION="${1:?Paper version is required}"
OUTPUT="${2:?Output JAR path is required}"
[[ "$VERSION" =~ ^[0-9]+(\.[0-9]+)+$ ]]
AGENT='CultivationLegacy-CI/1.0 (+https://github.com/wickidcow/SF_CultivationLegacy)'
mkdir -p "$(dirname "$OUTPUT")"
curl --fail --silent --show-error --location --retry 3 --max-time 90 \
  --header "User-Agent: ${AGENT}" \
  "https://fill.papermc.io/v3/projects/paper/versions/${VERSION}/builds" > "${OUTPUT}.metadata.json"
BUILD=$(jq -ec '[.[] | select(.channel == "STABLE")] | sort_by(.id) | last // empty' "${OUTPUT}.metadata.json")
URL=$(jq -er '.downloads["server:default"].url // empty' <<<"$BUILD")
SHA=$(jq -er '.downloads["server:default"].checksums.sha256 // empty' <<<"$BUILD")
[[ "$URL" == https://fill-data.papermc.io/* ]]
[[ "$SHA" =~ ^[0-9a-f]{64}$ ]]
echo "Testing stable Paper ${VERSION}, build $(jq -r '.id' <<<"$BUILD")"
curl --fail --silent --show-error --location --retry 3 --max-time 180 \
  --header "User-Agent: ${AGENT}" --output "$OUTPUT" "$URL"
echo "$SHA  $OUTPUT" | sha256sum -c -
unzip -tqq "$OUTPUT"
