<p align="center">
  <img src="assets/logo.svg" alt="Now Playing Companion logo" width="120" />
</p>

<h1 align="center">Now Playing Companion</h1>

<p align="center">
  Pixel's ambient "Now Playing" song recognition, on your Wear OS watch. No backend, no
  analytics, no microphone access — songs your phone already recognized are sent straight to
  your wrist over Bluetooth or Wi-Fi.
</p>

## Features

- **Wrist alerts** — a quiet notification when a new song is heard nearby, with a choice of
  Subtle, Prominent, Heartbeat, or Mute vibration.
- **Tile** — the current (or last heard) song, with a one-tap "Open on phone" button.
- **Watch face complications** — song title in Short Text and Long Text slots, pushed only when
  a song arrives (never polled).
- **History** — recently recognized songs on both watch and phone; tap to open, swipe to remove.
- **Open on phone** — sends a song to YouTube Music, Spotify, or Google Search on the phone.

## How it works

Now Playing runs on the phone (Android System Intelligence) and posts its result as a
local-only notification, so it never bridges to the watch on its own. The phone app reads that
notification with a `NotificationListenerService` and publishes the song as a Wearable Data
Layer item; the watch app picks it up, stores it, and updates the alert, tile, and
complications.

## Prerequisites

- Android Studio (Narwhal or newer): https://developer.android.com/studio
- JDK 17+ (bundled with Android Studio)
- A Pixel phone with Now Playing turned on (Settings → Sound & vibration → Now Playing)
- A Wear OS 3+ watch paired to that phone

## Build & run

```
git clone https://github.com/wwwescape/now-playing-companion.git
cd now-playing-companion
./gradlew assembleDebug
```

Install `mobile` on the phone and `wear` on the watch. Both must share the same signing key
(debug builds do) or the Data Layer won't connect them.

Sideloaded APKs on Android 13+ need **Settings → Apps → Now Playing Companion → ⋮ → Allow
restricted settings** before notification access can be granted (not needed for `adb install`
or Play Store installs).

## Test

```
./gradlew lint testDebugUnitTest
```

## Release a new version

Bump `appVersionCode` and `appVersionName` in `gradle.properties` (shared by both apps), commit,
then:

```
git tag v0.1.0
git push origin v0.1.0
```

That tag push builds signed release APKs and AABs for the phone and watch apps and attaches them
to an auto-generated GitHub Release. See `.github/workflows/release.yml`; it needs the
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` repository secrets set
(see `keystore.properties`, which is gitignored and holds these locally). Both apps are signed
with the same key, which the Wearable Data Layer requires.

## Project layout

```
mobile/    Phone app: Now Playing notification listener, Data Layer publisher, history UI
  sync/        Notification parsing (NowPlayingParser) and watch sync
wear/      Watch app (Wear Compose Material 3)
  sync/        Data Layer listener, "open on phone" via RemoteActivityHelper
  tile/        Now Playing tile
  complication/ Short/long text complication data source
  alert/       Notification and haptic patterns
scripts/   generate_icons.py — builds the logo, launcher icons, and store art
design/    Play Store icon assets
assets/    README/repo assets
```

## Privacy

Now Playing Companion records no audio and has no microphone access. It reads only Now
Playing's notification on the phone, and the song title and artist travel directly between
your phone and watch over Google Play services' Wearable Data Layer. Nothing is sent to any
server.

## License

GPL-3.0 — see `LICENSE`.

## Support

If you find Now Playing Companion useful, consider buying me a coffee:

[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="40" />](https://buymeacoffee.com/wwwescape)
