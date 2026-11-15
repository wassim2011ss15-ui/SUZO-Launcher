#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$HOME/SUZO-Launcher"

SDK_ROOT="${ANDROID_SDK_ROOT:-$PWD/android-sdk}"
PLATFORM_ZIP="$PWD/sdk-check/platform.zip"
TOOLS_ZIP="$PWD/build-tools.zip"

mkdir -p "$SDK_ROOT/platforms/android-36" "$SDK_ROOT/build-tools/36.0.0" build

# Reuse the SDK archives already downloaded for this project when needed.
if ! find "$SDK_ROOT" -type f -name android.jar -print -quit 2>/dev/null | grep -q .; then
    if [ -f "$PLATFORM_ZIP" ]; then
        echo "Extracting Android platform..."
        unzip -oq "$PLATFORM_ZIP" -d "$SDK_ROOT/platforms/android-36"
    fi
fi

if ! find "$SDK_ROOT" -type f -name apksigner -print -quit 2>/dev/null | grep -q .; then
    if [ -f "$TOOLS_ZIP" ]; then
        echo "Extracting Android build tools..."
        unzip -oq "$TOOLS_ZIP" -d "$SDK_ROOT/build-tools/36.0.0"
    fi
fi

ANDROID_JAR="$(find "$SDK_ROOT" -type f -name android.jar -print -quit)"
D8_JAR="$(find "$SDK_ROOT" -type f -name d8.jar -print -quit)"
APKSIGNER="$(find "$SDK_ROOT" -type f -name apksigner -print -quit)"
AAPT2="$(find "$SDK_ROOT" -type f -name aapt2 -print -quit)"

for tool in ANDROID_JAR D8_JAR APKSIGNER AAPT2; do
    if [ -z "${!tool}" ]; then
        echo "ERROR: $tool not found under $SDK_ROOT"
        echo "Check sdk-check/platform.zip and build-tools.zip, then run this script again."
        exit 1
    fi
done

if [ -f "build/signing.env" ]; then
    # This file is local-only and must never be committed.
    source "build/signing.env"
fi
: "${SUZO_STORE_PASS:?Signing password missing in local build/signing.env}"
: "${SUZO_KEY_PASS:?Key password missing in local build/signing.env}"

echo "=== SUZO BUILD START ==="
rm -rf build/classes build/dex build/compiled-res.zip
mkdir -p build/classes build/dex

find src/main/java -name "*.java" > build/sources.txt
javac -source 8 -target 8 -classpath "$ANDROID_JAR" -d build/classes @build/sources.txt

java -cp "$(dirname "$D8_JAR")" com.android.tools.r8.D8 \
    --lib "$ANDROID_JAR" --min-api 23 --output build/dex \
    $(find build/classes -name "*.class")

if [ -d res ]; then
    "$AAPT2" compile --dir res -o build/compiled-res.zip
fi

LINK_ARGS=()
if [ -f build/compiled-res.zip ]; then
    LINK_ARGS+=(-R build/compiled-res.zip)
fi

rm -f build/SUZO-unsigned.apk build/SUZO-Launcher.apk
"$AAPT2" link -o build/SUZO-unsigned.apk -I "$ANDROID_JAR" \
    --manifest AndroidManifest.xml --min-sdk-version 23 --target-sdk-version 36 \
    "${LINK_ARGS[@]}"

(cd build/dex && zip -q -j ../SUZO-unsigned.apk classes.dex)

"$APKSIGNER" sign --ks build/suzo.keystore \
    --ks-key-alias suzo --ks-pass "pass:$SUZO_STORE_PASS" \
    --key-pass "pass:$SUZO_KEY_PASS" \
    --out build/SUZO-Launcher.apk build/SUZO-unsigned.apk

"$APKSIGNER" verify build/SUZO-Launcher.apk
mkdir -p /sdcard/Download
cp build/SUZO-Launcher.apk /sdcard/Download/SUZO-Launcher.apk

if [ -x "$HOME/rish" ]; then
    "$HOME/rish" -c "cp /sdcard/Download/SUZO-Launcher.apk /data/local/tmp/SUZO-Launcher.apk && pm install -r /data/local/tmp/SUZO-Launcher.apk" || {
        echo "APK built. Automatic install failed; install it from Downloads."
    }
else
    echo "APK built. Install SUZO-Launcher.apk from Downloads."
fi

echo "=== SUZO BUILD COMPLETE ==="
