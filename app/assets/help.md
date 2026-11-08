![Kiosky logo](logo.png)

# Welcome to Kiosky

Kiosky shows one web page full-screen and keeps it there: no browser tabs, no
address bar, and the screen stays on. It's made for wall-mounted tablets and
dashboards, such as Home Assistant or Grafana.

You're seeing this page because no address has been set yet.

## Set your page

1. **Swipe down from the top edge** of the screen, where the arrow is pointing.
   Swiping up from the bottom edge works too. The system bars and the Kiosky
   toolbar appear at the top.
2. Tap the address box and type your page's address, for example
   `https://www.example.com`.
3. Tap **Go** (or press Enter).

Kiosky remembers that address and opens it every time the app starts. If you
leave out `https://`, Kiosky adds it for you.

> Only `https://` addresses work. Plain `http://` pages are blocked and won't load.

## The toolbar

Swipe in from the top or bottom edge at any time to bring the toolbar back. Start
the swipe right at the edge of the glass: a swipe that starts further in goes to
the web page instead.

| Button | What it does |
| --- | --- |
| **Go** | Opens the address in the box and saves it as your page |
| **Reload** | Reloads the current page |
| **Help** | Shows this page (your saved address isn't changed) |
| **Update** | Checks for a newer version of Kiosky |
| **✕** | Hides the toolbar |

The toolbar hides itself after 15 seconds unless you're typing in it.

## Updates

Kiosky checks for a new version a minute after it starts, then every 12 hours.
When one is available, a bar appears along the bottom of the screen:

1. Tap **Install**. Kiosky downloads the update.
2. The first time, Android asks you to let Kiosky install apps. Turn on
   **Allow from this source**, go back, and tap **Install** again.
3. Confirm on Android's install screen. Kiosky closes, updates, and normally
   reopens on its own. If it doesn't, tap the Kiosky icon.

Tap **Later** to hide the bar until Kiosky next restarts. Your saved address,
logins and settings are kept through updates.

Updates come from the project's releases on GitHub. Android only installs an
update signed with the same key as the copy you already have, so a tampered file
is refused.

## Good to know

- **Logins are kept.** Cookies and site storage are saved, so you stay signed
  in after restarting the app or the tablet.
- **Lost connection?** If the page can't be reached (Wi-Fi waking up, server
  restarting), Kiosky tries again every 10 seconds on its own.
- **Links stay inside Kiosky.** Nothing opens in another app or browser.
- **Screen stays on** while Kiosky is open. To dim it at night, use the
  tablet's own schedule or your dashboard's settings.
- **Sound and video** can play without a tap first, which suits cameras and
  doorbells on a dashboard.
- **Back button:** hides the toolbar if it's showing, otherwise goes back a
  page. On the first page, it closes Kiosky.
- **Back to this page:** open the toolbar and tap **Help**. To make this page
  the start page again, clear the address box and tap **Go**.

## Tips for a wall-mounted tablet

- **Keep people in Kiosky:** turn on Android's *App pinning* (often under
  Settings › Security) and pin Kiosky.
- **Stop Android closing it:** set Kiosky's battery use to *Unrestricted* in
  Settings › Apps › Kiosky › Battery.
- **Leave it charging.** The screen stays on, so it uses more power than usual.

## If something's wrong

- **Blank or "page not available" screen:** check the address in the toolbar
  and that it starts with `https://`. Kiosky keeps retrying, so a page that comes
  back online loads by itself.
- **Signed out every time:** make sure your site isn't set to clear its sign-in
  when the browser closes.
- **The toolbar won't appear:** swipe from the very edge of the screen, slowly.

## About

Kiosky {{version}} · Free software under the GNU General Public License v3.0.
Source code and releases: [github.com/MichelleFindlay/kiosky](https://github.com/MichelleFindlay/kiosky)

### Credits

Kiosky is built without the Android SDK, using these open-source projects:

- [Apktool](https://github.com/iBotPeaches/Apktool) by Connor Tumbleson and
  contributors (Apache License 2.0) packs the app's manifest and resources into
  an APK.
- [dex2jar](https://github.com/pxb1988/dex2jar) by Bob Pan and contributors
  (Apache License 2.0) turns Java code into Android's dex format.
- [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer) by Patrick
  Favre-Bulle (Apache License 2.0) aligns and signs the APK, using `zipalign`
  and `apksig` from the Android Open Source Project.
- [OpenJDK](https://openjdk.org) (GPL v2 with the Classpath Exception) compiles
  the Java code.

Pages are displayed by Android System WebView, which is built from the
[Chromium](https://www.chromium.org) project (BSD licence).
