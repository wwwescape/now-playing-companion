<p align="center">
  <img src="assets/logo.svg" alt="Now Playing Companion logo" width="120" />
</p>

<h1 align="center">Now Playing Companion</h1>

<p align="center">
  Pixel's ambient "Now Playing" song recognition, on your Wear OS watch. No backend, no
  analytics, no microphone access — songs your phone already recognized go straight to your
  wrist over Bluetooth or Wi-Fi.
</p>

<p align="center">
  <a href="https://github.com/wwwescape/now-playing-companion/releases"><img src="https://img.shields.io/github/v/release/wwwescape/now-playing-companion.svg?style=flat-square" alt="GitHub release" /></a>
  <a href="https://github.com/wwwescape/now-playing-companion/commits/master"><img src="https://img.shields.io/github/last-commit/wwwescape/now-playing-companion.svg?style=flat-square" alt="GitHub last commit" /></a>
  <a href="https://github.com/wwwescape/now-playing-companion"><img src="https://img.shields.io/github/languages/code-size/wwwescape/now-playing-companion.svg?color=red&style=flat-square" alt="GitHub code size" /></a>
</p>

## Features

- **Wrist alerts** — a quiet notification when a new song is heard nearby, with Subtle,
  Prominent, Heartbeat, or Mute vibration.
- **Tile** — the current (or last heard) song, with a one-tap **Open on phone** button.
- **Watch face complications** — the song title in Short Text and Long Text slots, pushed only
  when a song arrives (never polled).
- **History** — recently recognized songs on both the watch and the phone; tap to open, swipe
  to remove.
- **Open on phone** — sends a song to YouTube Music, Spotify, or Google Search on the phone.

## How It Works

Now Playing runs on the phone (Android System Intelligence) and posts its result as a local-only
notification, so it never reaches the watch on its own. The phone app reads that notification
with a `NotificationListenerService` and publishes the song as a Wearable Data Layer item. The
watch app picks it up, stores it, and updates the alert, tile, and complications.

## Installation

Download both APKs from the [latest release](https://github.com/wwwescape/now-playing-companion/releases/latest):

- `now-playing-companion-phone-*.apk` — install on your phone
- `now-playing-companion-watch-*.apk` — install on your watch

Then open the phone app and grant notification access. On Android 13+, a sideloaded app first
needs **Settings → Apps → Now Playing Companion → ⋮ → Allow restricted settings** before
notification access can be granted.

Requires:

- A Pixel phone running Android 8.0 (API 26) or newer, with Now Playing turned on
  (**Settings → Sound & vibration → Now Playing**)
- On Pixels with the standalone **Now Playing** app (March 2026 onwards), its song notifications
  turned on: **Now Playing app → Settings → Notifications**. Without them, Pixel shows songs only
  on the lock screen and the companion has nothing to read.
- A Wear OS 3+ watch paired to that phone

## Privacy

Now Playing Companion records no audio and has no microphone access. It reads only Now
Playing's notification on the phone, and the song title and artist travel directly between your
phone and watch over Google Play services' Wearable Data Layer. Nothing is sent to any server.

## Development

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Narwhal or newer)
- JDK 17+ (bundled with Android Studio)
- A Pixel phone with Now Playing turned on, and a Wear OS 3+ watch paired to it

### Build & run

```bash
git clone https://github.com/wwwescape/now-playing-companion.git
cd now-playing-companion
./gradlew assembleDebug
```

Install `mobile` on the phone and `wear` on the watch. Both must share the same signing key
(debug builds do), or the Data Layer won't connect them.

### Test

```bash
./gradlew lint testDebugUnitTest
```

### Release a new version

Bump `appVersionCode` and `appVersionName` in `gradle.properties` (shared by both apps), commit,
then tag `v` + `appVersionName` (the release workflow refuses a mismatched tag):

```bash
git tag v0.0.2
git push origin v0.0.2
```

The tag push builds signed release APKs and AABs for the phone and watch apps and attaches them
to a new GitHub Release (see `.github/workflows/release.yml`). It needs the `KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` repository secrets, which are kept locally
in the gitignored `keystore.properties`. Both apps are signed with the same key, which the
Wearable Data Layer requires.

### Icons

Regenerate the logo, launcher icons, and store art with `python scripts/generate_icons.py`
(needs `pip install resvg-py`).

### Project layout

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
assets/    README assets
```

## License

GPL-3.0 — see [LICENSE](LICENSE).

## Support

If you find Now Playing Companion useful, consider buying me a coffee:

[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="40" />](https://buymeacoffee.com/wwwescape)
