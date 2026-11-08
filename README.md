<p align="center"><img src="app/assets/logo.png" alt="Kiosky logo" width="128" height="128"></p>

# kiosky

A full-screen Android web kiosk for wall-mounted tablets. Usage instructions live in [app/assets/help.md](app/assets/help.md); the app shows that page on first launch, before an address has been set.

## Building

Run `./build.sh` (needs JDK 11+, curl and unzip). It writes a signed `kiosky.apk`. The build doesn't contact GitHub releases.

## Versions and updates

The version is set in two places that must match: `app/AndroidManifest.xml` and `app/apktool.yml`. `versionName` is `MAJOR.MINOR.PATCH`. `versionCode` must always go up; use `MAJOR*10000 + MINOR*100 + PATCH` (1.0.0 → 10000, 1.2.3 → 10203).

The app checks `https://api.github.com/repos/MichelleFindlay/kiosky/releases/latest` a minute after launch and every 12 hours, and from the toolbar's **Update** button. To publish an update:

1. Bump the version in both files and run `./build.sh`.
2. Create a GitHub release tagged `vMAJOR.MINOR.PATCH` (for example `v1.0.1`) and attach `kiosky.apk`.

Drafts and pre-releases are ignored. Updates must be signed with `kiosky.keystore`, or Android refuses them.

## Shepherd management

Kiosky can be managed by a [Kiosky Shepherd](https://github.com/MichelleFindlay/kiosky-shepherd) server, following that repo's `docs/PROTOCOL.md` (version 1). The client lives in `src/kiosky/`:

| File | Role |
| --- | --- |
| `Shepherd.java` | Enrollment, the check-in loop with backoff, command dispatch and result delivery |
| `Policy.java` | The server's policy with protocol defaults, and `allowed_hosts` matching |
| `ManagedState.java` | The `managed` preferences: token, cached policy, queued events, finished commands |
| `Pin.java` | Technician PIN check (PBKDF2-HMAC-SHA256, implemented on `HmacSHA256` so it works below Android 8) |
| `DeviceInfo.java` | Enrollment device facts and the check-in status report |
| `MainActivity.java` | Enforces the policy: toolbar modes, PIN unlock, navigation blocking, commands, server-chosen updates |

Enrollment links (`kiosky://enroll?server=…&token=…`) open Kiosky directly. Managed mode ends only when the server answers `410 device_retired`.

## Credits

Built without the Android SDK, using:

- [Apktool](https://github.com/iBotPeaches/Apktool) (Apache 2.0): packs the manifest and resources into an APK
- [dex2jar](https://github.com/pxb1988/dex2jar) (Apache 2.0): converts Java bytecode to dex
- [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer) (Apache 2.0): zipalign and APK signing, using AOSP's `zipalign` and `apksig`
- [OpenJDK](https://openjdk.org) (GPL v2 with Classpath Exception): Java compiler

Kiosky is licensed under the GNU GPL v3.0; see [LICENSE](LICENSE).
