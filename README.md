# SUZO Launcher

**Feel the Speed.** A lightweight Android launcher project with Liquid Glass, Dark and Classic themes.

## Current features

- Android HOME launcher entry point.
- Three selectable interface themes: Liquid Dark, Liquid Glass and Classic.
- Large live clock and date display.
- Application search and paginated app library.
- Configurable grid columns, icon size, icon shape, icon color and dock count.
- System wallpaper picker with persisted URI permission and downsampled image loading.
- Optional icon-pack integration for compatible packs that expose an `appfilter.xml`.
- In-launcher control center that opens Android's Wi-Fi, Bluetooth, display, sound and battery settings pages.
- Optional SUZO Keyboard input method with a basic QWERTY layout and emoji panel.
- Lightweight touch animations and persistent preferences.

## Important Android limitations

- A normal launcher cannot replace Android's system notification shade or silently toggle protected radios on modern Android. SUZO's control center opens the official settings pages instead.
- The optional keyboard must be enabled by the user in Android Settings, then selected from the input-method picker.
- Icon packs must be installed separately and must contain a compatible `appfilter.xml` to provide mapped icons.
- Widgets, a true multi-home-screen workspace, notification-listener features and deeper device integrations are not yet implemented.

## Build on Termux

The project expects Android SDK platform 36, build-tools 36.0.0, Java, unzip and zip. If SDK archives are already present at `sdk-check/platform.zip` and `build-tools.zip`, `build.sh` attempts to extract them automatically.

Keep these local-only files on your device:
- `build/signing.env`
- `build/suzo.keystore`
- SDK packages and generated APKs

They are ignored by Git. Never upload signing passwords, keystores or personal access tokens.

Run:

```bash
cd ~/SUZO-Launcher
git pull --ff-only
bash build.sh
```

The APK is copied to `/sdcard/Download/SUZO-Launcher.apk` when the build succeeds.

## Project structure

- `MainActivity.java` — launcher home, clock, app library and dock.
- `SettingsActivity.java` — theme and customization settings.
- `ThemeEngine.java` — shared theme colors and panels.
- `QuickPanel.java` — in-launcher quick settings panel.
- `SuzoKeyboardService.java` — optional keyboard and emoji input method.
- `res/xml/suzo_input_method.xml` — Android input-method metadata.
