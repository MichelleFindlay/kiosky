#!/usr/bin/env bash
# Builds kiosky.apk without the Android SDK.
# Needs: JDK 11+ (javac, jar), curl, unzip. Tools are fetched from GitHub on first run.
set -euo pipefail

P="$(cd "$(dirname "$0")" && pwd)"
T="$P/.tools"

# Signing keys live outside the repo (see "Signing" in README.md). Set the KIOSKY_*
# variables yourself, or keep them in ~/.kiosky/release.env.
ENV_FILE="${KIOSKY_SIGNING_ENV:-$HOME/.kiosky/release.env}"
if [ -z "${KIOSKY_KEYSTORE:-}" ] && [ -f "$ENV_FILE" ]; then
  set -a; . "$ENV_FILE"; set +a
fi
LINEAGE="$P/signing/kiosky.lineage"
for v in KIOSKY_KEYSTORE KIOSKY_KEY_ALIAS KIOSKY_KEYSTORE_PASS KIOSKY_LEGACY_KEYSTORE KIOSKY_LEGACY_ALIAS KIOSKY_LEGACY_PASS; do
  if [ -z "${!v:-}" ]; then
    echo "$v isn't set. Put the signing keys in $ENV_FILE (see README.md, Signing)." >&2
    exit 1
  fi
done
for f in "$KIOSKY_KEYSTORE" "$KIOSKY_LEGACY_KEYSTORE" "$LINEAGE"; do
  [ -f "$f" ] || { echo "Missing $f. Never generate a new key here: installed tablets only accept updates from the same keys." >&2; exit 1; }
done

mkdir -p "$T"
[ -f "$T/apktool.jar" ] || curl -fsSL -o "$T/apktool.jar" \
  https://github.com/iBotPeaches/Apktool/releases/download/v2.10.0/apktool_2.10.0.jar
[ -f "$T/signer.jar" ] || curl -fsSL -o "$T/signer.jar" \
  https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar
if [ ! -d "$T/d2j" ]; then
  curl -fsSL -o "$T/d2j.zip" https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip
  unzip -q "$T/d2j.zip" -d "$T/d2j"
fi
D2J="$(ls -d "$T"/d2j/dex-tools-*)"

rm -rf "$P/build" && mkdir -p "$P/build/stubcls" "$P/build/cls"

# Compile against hand-written API stubs (only used at compile time, never packaged)
javac -nowarn -Xlint:-options --release 8 -d "$P/build/stubcls" $(find "$P/stubs" -name '*.java')
javac -Xlint:-options --release 8 -cp "$P/build/stubcls" -d "$P/build/cls" $(find "$P/src" -name '*.java')
(cd "$P/build/cls" && jar cf ../app.jar .)

# Java bytecode -> classes.dex
sh "$D2J/d2j-jar2dex.sh" -f -o "$P/app/classes.dex" "$P/build/app.jar"

# Manifest + resources + dex -> unsigned APK
java -jar "$T/apktool.jar" b -f "$P/app" -o "$P/build/unsigned.apk"

# 1. zipalign (uber-apk-signer also signs, with the legacy key; step 2 replaces that)
java -jar "$T/signer.jar" -a "$P/build/unsigned.apk" -o "$P/build/aligned" \
  --ks "$KIOSKY_LEGACY_KEYSTORE" --ksAlias "$KIOSKY_LEGACY_ALIAS" \
  --ksPass "$KIOSKY_LEGACY_PASS" --ksKeyPass "$KIOSKY_LEGACY_PASS" >/dev/null

# 2. Signatures: v1/v2 with the legacy key (Android 5.1–8.1 only know that one), v3 with
#    the release key plus the proof of rotation, from Android 9 (see tools/Sign.java)
mkdir -p "$P/build/out"
java -cp "$T/signer.jar" "$P/tools/Sign.java" "$(ls "$P"/build/aligned/*.apk)" "$P/build/out/kiosky.apk" "$LINEAGE"

# 3. Check alignment and signatures
java -jar "$T/signer.jar" -a "$P/build/out/kiosky.apk" --onlyVerify | grep -E "verified|ERROR|FAIL" || true

cp "$P"/build/out/*.apk "$P/kiosky.apk"
rm -f "$P/app/classes.dex"
echo "Built $P/kiosky.apk"
