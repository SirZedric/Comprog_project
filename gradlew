#!/bin/sh
# First-run launcher: fetch and verify the official Gradle wrapper JAR.
set -eu
QUIZ_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
QUIZ_WRAPPER="$QUIZ_ROOT/gradle/wrapper/gradle-wrapper.jar"
QUIZ_WRAPPER_SHA=2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046

quiz_sha256() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | awk '{print $1}'
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$1" | awk '{print $1}'
    else
        echo "Install sha256sum or shasum to verify Gradle." >&2
        return 1
    fi
}

if [ ! -f "$QUIZ_WRAPPER" ]; then
    QUIZ_TEMP="${QUIZ_WRAPPER}.tmp.$$"
    trap 'rm -f "$QUIZ_TEMP"' EXIT HUP INT TERM
    QUIZ_URL=https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar
    echo "Downloading the official Gradle wrapper (first run only)..."
    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --retry 3 --connect-timeout 30 --max-time 180 --output "$QUIZ_TEMP" "$QUIZ_URL"
    elif command -v wget >/dev/null 2>&1; then
        wget --timeout=180 --tries=3 --output-document="$QUIZ_TEMP" "$QUIZ_URL"
    else
        echo "Install curl or wget, or use the included GitHub Actions build." >&2
        exit 1
    fi
    if [ "$(quiz_sha256 "$QUIZ_TEMP")" != "$QUIZ_WRAPPER_SHA" ]; then
        echo "Gradle wrapper checksum failed; the downloaded file will not run." >&2
        exit 1
    fi
    mv "$QUIZ_TEMP" "$QUIZ_WRAPPER"
    trap - EXIT HUP INT TERM
fi

if [ "$(quiz_sha256 "$QUIZ_WRAPPER")" != "$QUIZ_WRAPPER_SHA" ]; then
    echo "Gradle wrapper checksum failed. Remove the JAR and retry." >&2
    exit 1
fi

if [ -n "${JAVA_HOME:-}" ]; then
    QUIZ_JAVA="$JAVA_HOME/bin/java"
else
    QUIZ_JAVA=java
fi
exec "$QUIZ_JAVA" -Dorg.gradle.appname=gradlew -classpath "$QUIZ_WRAPPER" org.gradle.wrapper.GradleWrapperMain "$@"
