#!/data/data/com.termux/files/usr/bin/bash
set -e
cd ~/SUZO-Launcher
BT="android-sdk/build-tools/36.0.0/android-16"
ANDROID="android-sdk/platforms/android-36/android.jar"

if [ -f "build/signing.env" ]; then
    source "build/signing.env"
fi
: "${SUZO_STORE_PASS:?Signing password missing}"
: "${SUZO_KEY_PASS:?Key password missing}"


echo "=== SUZO BUILD START ==="
rm -rf build/classes build/dex
mkdir -p build/classes build/dex

find src/main/java -name "*.java" > build/sources.txt
javac -source 8 -target 8 -classpath "$ANDROID" -d build/classes @build/sources.txt

java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 \
--lib "$ANDROID" --min-api 23 --output build/dex \
$(find build/classes -name "*.class")

rm -f build/SUZO-unsigned.apk build/SUZO-Launcher.apk
aapt2 link -o build/SUZO-unsigned.apk -I "$ANDROID" \
--manifest AndroidManifest.xml --min-sdk-version 23 --target-sdk-version 36

(cd build/dex && zip -q -j ../SUZO-unsigned.apk classes.dex)

"$BT/apksigner" sign --ks build/suzo.keystore \
--ks-key-alias suzo --ks-pass "pass:$SUZO_STORE_PASS" \
--key-pass "pass:$SUZO_KEY_PASS" \
--out build/SUZO-Launcher.apk build/SUZO-unsigned.apk

"$BT/apksigner" verify build/SUZO-Launcher.apk
cp build/SUZO-Launcher.apk /sdcard/Download/SUZO-Launcher.apk

~/rish -c "cp /sdcard/Download/SUZO-Launcher.apk /data/local/tmp/SUZO-Launcher.apk && pm install -r /data/local/tmp/SUZO-Launcher.apk"

echo "=== SUZO BUILD AND INSTALL SUCCESS ==="
