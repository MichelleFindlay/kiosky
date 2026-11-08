#!/usr/bin/env bash
# Builds ha-lp0.apk without the Android SDK.
# Needs: JDK 11+ (javac, jar), curl, unzip. Tools are fetched from GitHub on first run.
set -euo pipefail

P="$(cd "$(dirname "$0")" && pwd)"
T="$P/.tools"
KS="$P/ha-lp0.keystore"
KS_ALIAS="ha"
KS_PASS="halp0kiosk"

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

# Signing key (created once; keep it, updates must use the same key)
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$KS_PASS" -keypass "$KS_PASS" -dname "CN=ha.lp0.uk"
fi

# zipalign + v1/v2/v3 signature
java -jar "$T/signer.jar" -a "$P/build/unsigned.apk" -o "$P/build/out" \
  --ks "$KS" --ksAlias "$KS_ALIAS" --ksPass "$KS_PASS" --ksKeyPass "$KS_PASS"

cp "$P"/build/out/*.apk "$P/ha-lp0.apk"
rm -f "$P/app/classes.dex"
echo "Built $P/ha-lp0.apk"
