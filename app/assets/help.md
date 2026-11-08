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
| **Start on boot** | Turns opening Kiosky automatically at start-up on or off |
| **Enroll** | Hands this tablet to a Shepherd management server (see below) |
| **Lockdown** | Locks the tablet to Kiosky, so the system menus and buttons can't be used to leave |
| **Remove device owner** | Undoes device owner setup (only shown when lockdown is off) |
| **✕** | Hides the toolbar |

The toolbar hides itself after 15 seconds unless you're typing in it.

## Updates

Kiosky checks for a new version a minute after it starts, then every 12 hours.
When one is available, a bar appears along the bottom of the screen:

1. Tap **Install**. Kiosky downloads the update.
2. The first time, Android asks you to let Kiosky install apps. Turn on
   **Allow from this source**, go back, and tap **Install** again.
3. Confirm on Android's install screen. Kiosky closes, updates, and normally
   reopens on its own. On Android 10 and later this needs the same permission as
   **Start on boot** (below). If it doesn't reopen, tap the Kiosky icon.

Tap **Later** to hide the bar until Kiosky next restarts. Your saved address,
logins and settings are kept through updates.

Updates come from the project's releases on GitHub. Android only installs an
update signed with the same key as the copy you already have, so a tampered file
is refused.

## Start automatically

Kiosky can open by itself whenever the tablet is switched on or restarts, so a
wall panel comes back after a power cut without anyone touching it. It's off
until you turn it on:

1. Open the toolbar and tap **Start on boot: Off**. It changes to **On**.
2. On Android 10 and later, Kiosky then opens a settings screen. Turn on
   **Display over other apps** (sometimes called *Appear on top*) for Kiosky,
   then go back. Until you do, the button reads **Start on boot: Needs
   permission**.
3. Restart the tablet to check. Kiosky opens once start-up finishes, which can
   take a minute.

Kiosky opens after the tablet has been unlocked the first time, so for a wall
panel set the screen lock to *None* or *Swipe*. Some brands (such as Xiaomi,
Huawei and Oppo) also have their own *Autostart* setting that has to be turned on
for Kiosky.

Tap the button again to turn it off.

## Lock the tablet down

Normally, anyone can pull down the notification shade or press Home to leave
Kiosky. **Lockdown** stops that: Kiosky's own menu becomes the only way out.

1. Open the toolbar and tap **Lockdown: Off**.
2. Swipe in from an edge to open Kiosky's menu at any time, and tap **Lockdown:
   On** to unlock again. If a swipe shows nothing, **tap the top-left corner five
   times quickly** to open the menu instead.

How strong the lock is depends on whether Kiosky is the tablet's *device owner*.

| | Device owner | Not device owner |
| --- | --- | --- |
| Notification shade | Turned off | Closed again as soon as it opens (see below) |
| Home, Recents | Blocked | Blocked |
| Power menu and lock screen | Blocked | Still work |
| Leaving without Kiosky's menu | Not possible | Hold **Back** and **Recents** to unpin |
| Updates | Install without a prompt | Android asks to confirm |

Without device owner, Kiosky uses Android's *app pinning*. Turn on **Ask for PIN
before unpinning** (Settings › Security › App pinning) so people can't just unpin
it.

### Make Kiosky the device owner

This is done once per tablet, from a computer with
[adb](https://developer.android.com/tools/adb):

1. On the tablet, remove every account in Settings › Accounts (Android refuses
   while any account is signed in). You can add them back afterwards.
2. Turn on **USB debugging** (Settings › About › tap *Build number* seven times,
   then Settings › Developer options).
3. Connect the tablet and run:
   `adb shell dpm set-device-owner uk.lp0.ha/kiosky.AdminReceiver`
4. Open Kiosky and turn on **Lockdown**. The button reads **Lockdown: On**
   instead of **Lockdown: On (pinned)**.

> A device owner can't be uninstalled. To remove Kiosky later, turn Lockdown off
> and tap **Remove device owner** first, or factory-reset the tablet.

On a tablet managed by Shepherd, **Lockdown** and **Remove device owner** only
appear after the technician PIN.

## Managed tablets (Shepherd)

An organisation can run a **Kiosky Shepherd** server to look after many tablets at
once. Once a tablet is enrolled, the server chooses its page and settings, and the
tablet reports back how it's doing. The tablet's own controls are locked.

### Enroll a tablet

You need an enrollment code from your administrator. Any one of these works:

- **Scan the QR code** with the tablet's camera app and open the link, or
- open the **enrollment link** on the tablet, or
- open the toolbar, tap **Enroll**, and type the **server** address and the
  **code** (dashes and capitals don't matter). **Scan QR** uses a scanner app
  if one is installed.

Kiosky asks you to confirm before handing the tablet over. A tablet that's
already managed ignores enrollment links and codes.

### What changes once it's managed

- The server sets the page. If it hasn't chosen one yet, Kiosky shows
  *Waiting for configuration*.
- The toolbar may be hidden completely or show only **Reload** and **Help**,
  depending on what the administrator chose. **Update**, **Start on boot** and
  **Enroll** disappear.
- Only sites the administrator allows can open.
- Updates, start on boot, screen orientation, text size and similar settings
  come from the server.
- The server can reload the page, show a message, take a screenshot, clear
  sign-ins or restart Kiosky.
- Kiosky reports the page it's showing, any loading errors, battery, Wi-Fi
  signal and whether start-on-boot and update permissions are granted.

If the tablet loses contact with the server, it keeps its last settings and
stays locked. It's released only when the administrator removes it in Shepherd.
Kiosky then shows *This tablet is no longer managed* and keeps its current page.

### Technician PIN

If the administrator set a PIN, tap the **top-left corner** of the screen five
times quickly, then enter the PIN. The full toolbar opens until it next hides,
with **Device info** (server, device name, last check-in) and buttons to grant
missing permissions. After five wrong PINs, PIN entry is locked for 5 minutes.

## Good to know

- **Logins are kept.** Cookies and site storage are saved, so you stay signed
  in after restarting the app or the tablet.
- **Lost connection?** If the page can't be reached (Wi-Fi waking up, server
  restarting), Kiosky tries again every 10 seconds on its own.
- **Links stay inside Kiosky.** Nothing opens in another app or browser.
- **Notification shade:** whenever Kiosky is on screen, it stops people pulling
  down the notification shade, even with Lockdown off. As device owner the shade
  is turned off completely. Otherwise Kiosky closes it as soon as it opens, which
  works on most tablets up to Android 11. Android 12 and later restrict this, so
  make Kiosky the device owner there.
- **Screen stays on** while Kiosky is open. To dim it at night, use the
  tablet's own schedule or your dashboard's settings.
- **Sound and video** can play without a tap first, which suits cameras and
  doorbells on a dashboard.
- **Back button:** hides the toolbar if it's showing, otherwise goes back a
  page. On the first page, it closes Kiosky.
- **Back to this page:** open the toolbar and tap **Help**. To make this page
  the start page again, clear the address box and tap **Go**.

## Tips for a wall-mounted tablet

- **Keep people in Kiosky:** turn on **Lockdown** (see *Lock the tablet down*).
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
