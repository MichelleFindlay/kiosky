<p align="center"><img src="app/assets/logo.png" alt="Kiosky logo" width="128" height="128"></p>

# kiosky

A full-screen Android web kiosk for wall-mounted tablets. Usage instructions live in [app/assets/help.md](app/assets/help.md); the app shows that page on first launch, before an address has been set.

## Building

Run `./build.sh` (needs JDK 11+, curl, unzip and the signing keys; see Signing). It writes a signed `kiosky.apk`. The build doesn't contact GitHub releases.

## Signing

The signing keys are **not** in this repo. `build.sh` reads them from the `KIOSKY_*`
environment variables, or from `~/.kiosky/release.env` (private to you, `chmod 600`):

| Variable | What it is |
| --- | --- |
| `KIOSKY_KEYSTORE`, `KIOSKY_KEY_ALIAS`, `KIOSKY_KEYSTORE_PASS` | The release key |
| `KIOSKY_LEGACY_KEYSTORE`, `KIOSKY_LEGACY_ALIAS`, `KIOSKY_LEGACY_PASS` | The original key |

Back up `~/.kiosky` somewhere safe, such as a password manager. Without the release
key, installed tablets can never be updated again.

**Why there are two keys.** Up to 1.6.0, the original key was committed to this public
repo, so anyone could sign an APK that tablets would accept as an update. From 1.7.0,
APKs are signed with key rotation ([`tools/Sign.java`](tools/Sign.java)):

- **Android 9 and later** check the v3 signature: the new release key, plus
  [`signing/kiosky.lineage`](signing/kiosky.lineage), in which the original key
  vouches for it (created once by [`tools/RotateKey.java`](tools/RotateKey.java)).
  Once a tablet has 1.7.0, it only accepts updates signed with the release key; the
  original key gets no rollback rights.
- **Android 5.1 to 8.1** can't rotate keys. They check the v1/v2 signatures, still
  made with the original key, so they keep updating but stay exposed to the leaked
  key. Reinstall Kiosky on those tablets (or upgrade Android) to be rid of it.

Release every update with the same lineage. To check which key each Android
version sees: `java -cp .tools/signer.jar tools/CheckSigning.java kiosky.apk`.

On tablets set up with adb, turn **USB debugging** off afterwards. Physical access
with debugging on is the realistic way a leaked key gets abused.

## Versions and updates

The version is set in two places that must match: `app/AndroidManifest.xml` and `app/apktool.yml`. `versionName` is `MAJOR.MINOR.PATCH`. `versionCode` must always go up; use `MAJOR*10000 + MINOR*100 + PATCH` (1.0.0 → 10000, 1.2.3 → 10203).

The app checks `https://api.github.com/repos/MichelleFindlay/kiosky/releases/latest` a minute after launch and every 12 hours, and from the toolbar's **Update** button. To publish an update:

1. Bump the version in both files and run `./build.sh`.
2. Create a GitHub release tagged `vMAJOR.MINOR.PATCH` (for example `v1.0.1`) and attach `kiosky.apk`.

Drafts and pre-releases are ignored. Updates must be signed with the release keys (see Signing), or Android refuses them.

## Shepherd management

Kiosky can be managed by a [Kiosky Shepherd](https://github.com/MichelleFindlay/kiosky-shepherd) server, following that repo's `docs/PROTOCOL.md` (version 1). The client lives in `src/kiosky/`:

| File | Role |
| --- | --- |
| `Shepherd.java` | Enrollment, the check-in loop with backoff, command dispatch and result delivery |
| `Policy.java` | The server's policy with protocol defaults, and `allowed_hosts` matching |
| `Options.java` | Idle, watchdog, camera/microphone and screen-schedule keys, shared with the local **Settings** dialog |
| `ManagedState.java` | The `managed` preferences: token, cached policy, queued events, finished commands |
| `Pin.java` | Technician PIN check (PBKDF2-HMAC-SHA256, implemented on `HmacSHA256` so it works below Android 8) |
| `DeviceInfo.java` | Enrollment device facts and the check-in status report |
| `MainActivity.java` | Enforces the policy: toolbar modes, PIN unlock, navigation blocking, commands, server-chosen updates |

Enrollment links (`kiosky://enroll?server=…&token=…`) open Kiosky directly. Managed mode ends only when the server answers `410 device_retired`.

### Policy keys, events and status added in 1.7.0

Kiosky 1.7.0 implements `screen.off_at`, `screen.on_at`, `screen.wake_on_touch` and `screen.wake_minutes` as in PROTOCOL.md §5.2, plus these keys (missing keys use the defaults shown; older apps ignore them):

| Key | Default | Meaning |
| --- | --- | --- |
| `idle_home_minutes` | `0` | Load `url` after this many minutes without a touch (0: never) |
| `watchdog_minutes` | `2` | Reload a page that doesn't answer a JavaScript heartbeat for this long, then rebuild the WebView (0: never) |
| `media.camera`, `media.microphone` | `true` | Grant pages' camera/microphone requests, only for the `url` host and hosts listed in `allowed_hosts` |
| `screen.wake_on_motion` | `false` | The proximity or light sensor wakes the screen during the off hours, and ends idle dimming |
| `screen.dim_at`, `screen.dim_until` | `""` | Dim between these local times (`HH:MM`) |
| `screen.dim_brightness` | `15` | Dimmed brightness, in percent |
| `screen.idle_dim_minutes` | `0` | Dim after this many minutes without a touch (0: never) |
| `playlist.urls`, `playlist.seconds`, `playlist.pause_on_touch_seconds` | `[]`, `60`, `120` | Rotate through these pages after `url` (PROTOCOL.md §5.2) |
| `screensaver.minutes`, `screensaver.url` | `0`, `""` | Clock (or a page) after this many minutes untouched |
| `audio.volume`, `audio.quiet_at`, `audio.quiet_until`, `audio.quiet_volume` | `-1`, `""`, `""`, `0` | Media volume in percent (-1: leave it) and quiet hours |
| `reboot_at` | `""` | Daily reboot (device owner, Android 7+) or fresh restart, at this local time |
| `file_uploads` | `true` | Let pages open the file picker |
| `http_auth.host`, `http_auth.username`, `http_auth.password` | `""` | Answer HTTP basic-auth prompts from `host` (empty: the `url` host) |
| `trusted_certificates` | `[]` | SHA-256 fingerprints accepted despite TLS errors, on trusted hosts only |
| `battery.protect` | `false` | Turn on the tablet's charging limit (Samsung `protect_battery`, needs WRITE_SECURE_SETTINGS granted over adb) |

New events: `renderer_crashed` (`crashed`, `url`, `looping`), `watchdog_reload`, `watchdog_rebuild`, `idle_home`, `screen_schedule` (`state`: `off`/`on`), `media_denied` (`origin`, `reason`), `js_dialog` (`type`, `message`), `battery_hot` / `battery_cool` (at 45 °C and back under 42 °C, with `temperature`, `level`, `charging`), `battery_protect` (`state`). `page_error` is now also sent for main-frame HTTP errors, with the HTTP status as `code`.

New status keys: `screen_schedule` (`off`/`on`, when a schedule is set), `screen_state` (`normal`/`dim`/`off`), `camera_permission`, `microphone_permission`, `offline_screen`, `battery_temperature` (°C), `battery_health` (`good`, `overheat`, `dead`, `over_voltage`, `failure`, `cold`, `unknown`), `battery_full_hours`, `battery_protect` (`on`, `off`, `no_permission`, `unsupported`), `storage_free_mb`, `storage_total_mb`, `memory_free_mb`, `memory_total_mb`, `memory_low`, `app_memory_mb`.

`restart_app` now starts the page fresh instead of restoring it. Back never closes Kiosky while lockdown is on, whatever `allow_exit` says.

Pages can call `window.kiosky`: `wake()`, `isScreenOff()` and `version()` from any page; `reload()`, `setBrightness(percent)`, `screenOff()` and `deviceName()` only from a trusted page (the `url` host, `allowed_hosts` entries or an admin-sent page; unmanaged, the saved address's host).

New commands: `wake_screen` (`minutes`, default `screen.wake_minutes`), `announce` (`text`, `volume`), `reboot` (device owner). `diagnostics` and `live_view` (already in the protocol) are now implemented, and screenshots show the whole screen. More events: `crash` (at the next start), `certificate_untrusted`, `screensaver`, `rebooting`, `restarting`, `reboot_failed`; more status: `screensaver`, `reboot_supported`, `volume_percent`, `playlist_index`.

**QR provisioning** (`Provisioning.java`, `AdminReceiver.java`): a factory-reset tablet scanning Shepherd's provisioning QR installs Kiosky as device owner and enrolls with the code in its admin extras (PROTOCOL.md §3.3). The signature checksum for the release key is `5D43Qjavxg_OZaPbJfD5a9QWhnhi6drJw8uro0Zl9I8` (Android 9+); Shepherd uses the APK's own checksum when it serves the download, which works on every version.

## Credits

Built without the Android SDK, using:

- [Apktool](https://github.com/iBotPeaches/Apktool) (Apache 2.0): packs the manifest and resources into an APK
- [dex2jar](https://github.com/pxb1988/dex2jar) (Apache 2.0): converts Java bytecode to dex
- [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer) (Apache 2.0): zipalign and APK signing, using AOSP's `zipalign` and `apksig`
- [OpenJDK](https://openjdk.org) (GPL v2 with Classpath Exception): Java compiler

Kiosky is licensed under the GNU GPL v3.0; see [LICENSE](LICENSE).
