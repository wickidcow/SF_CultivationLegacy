#!/usr/bin/env bash
# Never update or replace an existing Cultivation tag/release, even on an authorized dispatch.
set -euo pipefail
fail() { echo "::error::$*" >&2; exit 1; }
: "$VERSION" "$OUTPUT_NAME" "$JAR" "$GITHUB_SHA" "$EXPECTED_SOURCE_SHA" "$GITHUB_REPOSITORY" "$GITHUB_EVENT_NAME" "$GITHUB_REF" "$GITHUB_REF_NAME" "$RELEASE_CONFIRMED" "$CONFIRM_VERSION"
[[ "$GITHUB_REPOSITORY" == "wickidcow/SF_CultivationLegacy" ]] || fail "Unexpected repository identity"
[[ "$GITHUB_EVENT_NAME" == "workflow_dispatch" ]] || fail "Only explicit workflow dispatch can publish"
[[ "$GITHUB_REF" == "refs/heads/main" && "$GITHUB_REF_NAME" == "main" ]] || fail "Only current main source can publish"
[[ "$RELEASE_CONFIRMED" == "true" ]] || fail "Explicit publication confirmation is mandatory"
[[ "$VERSION" =~ ^[0-9]+(\.[0-9]+)*$ ]] || fail "Invalid version"
[[ "$OUTPUT_NAME" == "SF_Cultivation$VERSION.jar" ]] || fail "Release artifact identity mismatch"
[[ "$CONFIRM_VERSION" == "$VERSION" ]] || fail "Confirmation must match the exact release version"
[[ "$GITHUB_SHA" =~ ^[0-9a-f]{40}$ && "$EXPECTED_SOURCE_SHA" == "$GITHUB_SHA" ]] || fail "Reviewed exact source SHA mismatch"
[[ "$JAR" == "dist/$OUTPUT_NAME" && -s "$JAR" ]] || fail "Canonical raw JAR is missing"
[[ -s "docs/releases/$VERSION.md" ]] || fail "Missing reviewed release notes"
unzip -tqq "$JAR" || fail "Raw JAR ZIP integrity check failed"
PLUGIN_VERSION="$(unzip -p "$JAR" plugin.yml | awk -F': *' '/^version:/ {gsub(/["\047]/,"",$2); print $2; exit}')"
[[ "$PLUGIN_VERSION" == "$VERSION" ]] || fail "Packaged plugin.yml version mismatch"

# Fail closed if main advanced during this long running build.
MAIN_SHA="$(gh api "repos/$GITHUB_REPOSITORY/git/ref/heads/main" --jq .object.sha)" || fail "Cannot resolve current main"
[[ "$MAIN_SHA" == "$GITHUB_SHA" ]] || fail "Main moved after the tested source; refusing stale publication"
TAG="v$VERSION"
if gh release view "$TAG" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
  fail "Existing GitHub release $TAG is immutable"
fi

# Git exit 0 = existing tag, 2 = missing tag, any other result is an unknown
# lookup failure. In particular, network failures MUST NOT permit publication.
tag_status=0
git ls-remote --exit-code --tags "https://github.com/$GITHUB_REPOSITORY.git" "refs/tags/$TAG" >/dev/null || tag_status=$?
case "$tag_status" in
  2) ;;
  0) fail "Existing Git tag $TAG is immutable" ;;
  *) fail "Cannot verify tag absence (git status $tag_status)" ;;
esac

sha256sum "$JAR"
gh release create "$TAG" "$JAR" --repo "$GITHUB_REPOSITORY" --target "$GITHUB_SHA" \
  --title "Cultivation $VERSION" --notes-file "docs/releases/$VERSION.md"
