#!/usr/bin/env bash
# Runs the unit tests (test/) for the parts of Kiosky that don't need Android:
# Policy, Options, Pin, Json, Markdown and the help translations.
# Needs: JDK 11+ (javac, java), curl. JUnit and org.json are fetched from Maven
# Central on first run and checked against their SHA-256.
set -euo pipefail

P="$(cd "$(dirname "$0")" && pwd)"
T="$P/.tools/test"
B="$P/build/test"
mkdir -p "$T"

fetch() { # url sha256
  local f="$T/$(basename "$1")"
  if [ ! -f "$f" ]; then
    curl -fsSL -o "$f.part" "$1"
    mv "$f.part" "$f"
  fi
  echo "$2  $f" | sha256sum -c --quiet - || { rm -f "$f"; echo "Checksum mismatch for $1" >&2; exit 1; }
}
fetch https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar \
  8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3
fetch https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar \
  66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9
# Android has its own org.json with the same API; this is the reference version
fetch https://repo1.maven.org/maven2/org/json/json/20231013/json-20231013.jar \
  0f18192df289114e17aa1a0d0a7f8372cc9f5c7e4f7e39adcf8906fe714fa7d3
LIBS="$T/junit-4.13.2.jar:$T/hamcrest-core-1.3.jar"
JSON="$T/json-20231013.jar"

rm -rf "$B" && mkdir -p "$B/stubs" "$B/app" "$B/fakes" "$B/tests"

# The app compiles against the Android stubs, as in build.sh. At run time the real
# org.json and the working test fakes (test/fakes) come before the stubs.
javac -nowarn -Xlint:-options --release 8 -d "$B/stubs" $(find "$P/stubs" -name '*.java')
javac -nowarn -Xlint:-options --release 8 -cp "$B/stubs" -d "$B/app" $(find "$P/src" -name '*.java')
javac -nowarn -Xlint:-options --release 8 -cp "$B/stubs" -d "$B/fakes" $(find "$P/test/fakes" -name '*.java')
javac -nowarn -Xlint:-options --release 8 -cp "$JSON:$LIBS:$B/app:$B/stubs" -d "$B/tests" $(find "$P/test/kiosky" -name '*.java')

CLASSES=$(cd "$B/tests" && find . -name '*Test.class' | sed 's|^\./||; s|\.class$||; s|/|.|g' | sort)
cd "$P" # HelpTest reads app/assets
java -cp "$B/fakes:$B/tests:$B/app:$JSON:$LIBS:$B/stubs" org.junit.runner.JUnitCore $CLASSES
