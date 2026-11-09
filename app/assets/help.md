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
| **Help** | Shows this page over yours. **Close** (or Back) returns to your page, which stays loaded underneath |
| **Settings** | Night schedule, dimming, going back to the start page, camera and microphone, and the settings PIN (see *Settings*) |
| **Update** | Checks for a newer version of Kiosky |
| **Start on boot** | Turns opening Kiosky automatically at start-up on or off |
| **Enroll** | Hands this tablet to a Shepherd management server (see below) |
| **Lockdown** | Locks the tablet to Kiosky, so the system menus and buttons can't be used to leave |
| **Remove device owner** | Undoes device owner setup (only shown when lockdown is off) |
| **✕** | Hides the toolbar |

The toolbar hides itself after 15 seconds unless you're typing in it.

## Settings

Tap **Settings** in the toolbar. Everything here is optional; leave a time empty to
turn that part off.

### Screen off at night

Set **Off at** and **On again at** (24-hour `HH:MM`, for example `23:00` and
`06:30`). The times can wrap past midnight. In between, the screen goes black.

- **A touch wakes it**: the first touch only wakes the screen and doesn't press
  anything on the page. It stays on for **Stays awake for** minutes, then goes dark
  again.
- **Someone close by wakes it**: uses the tablet's proximity sensor (a hand near the
  top edge) or light sensor (a room light switched on). Not every tablet has one.
- If neither is ticked, Kiosky lets the screen switch off properly and turns it back
  on at the *On again* time. **Five quick taps** anywhere still wake it, so you
  can always get back in.

When the off hours end, Kiosky reloads the page so it's fresh.

### Dimming

**Dim from** and **Until** lower the brightness for part of the day, to the
**Dimmed brightness** percentage. **Also dim after this many minutes untouched**
dims the screen whenever nobody has used it for a while. The first touch brightens
it again without pressing anything.

### Page

- **Back to the start page after this many minutes untouched**: if someone taps
  into another page and walks away, Kiosky goes back to your page by itself.
- **Reload a frozen page after this many minutes**: Kiosky checks every few
  seconds that the page still responds. If it stops for this long, Kiosky reloads
  it, and if that doesn't help, starts it again from scratch. On by default (2
  minutes).
- **Pages may use the camera / microphone**: see *Camera and microphone*.

### Playlist

List more pages (one `https://` address per line) to rotate through after your
start page, each shown for **Show each page for** seconds. A touch pauses the
rotation for a while, so someone using a page isn't interrupted.

### Screensaver

After **Show a clock after this many minutes untouched**, a large clock and date
drift slowly around a black screen (so nothing burns in). Give a page address to
show that instead, such as a photo frame. The first touch closes it without
pressing anything.

### Sound

**Volume** sets the media volume (leave it empty to use the tablet's own).
**Quiet from** and **Until** lower it, or mute it at `0`, for part of the day.
Kiosky only changes the volume when the wanted level changes, so someone can
still turn it up or down by hand until the next change.

### Tablet

- **Restart every day at**: a fresh start each day (for example `04:00`) clears
  slow memory leaks in long-running dashboards. As device owner (Android 7+) it's
  a full reboot; otherwise Kiosky restarts itself.
- **Pages may upload files**: lets "choose file" buttons open Android's file
  picker. Turn it off on public tablets that don't need it.
- **Forget saved sign-ins and trusted certificates**: see *Sign-ins and
  certificates*.

### Settings PIN

Tap **Set a PIN…** at the bottom of Settings. After that, swiping in from an edge
(or the five corner taps) asks for the PIN before the toolbar opens, the Back button
no longer closes Kiosky, and enrollment links need the PIN too. Use **PIN…** to
change or remove it.

> If the PIN is forgotten, the only way back is Settings › Apps › Kiosky › Storage ›
> **Clear storage** (or `adb shell pm clear uk.lp0.ha`). That also forgets the
> address and sign-ins.

## Camera and microphone

Video doorbells, intercoms and two-way audio (for example in Home Assistant or
Frigate) need the camera or microphone. When a page asks:

- Your own page's site gets them straight away. The first time, Android asks you to
  allow Kiosky to use the camera or microphone. As device owner, Kiosky grants that
  itself.
- Any other site asks once (**Allow** or **Block**), and a yes is remembered.
- Untick **Pages may use the camera** or **microphone** in Settings to refuse them
  everywhere.

Only `https://` pages can use them.

### Controlling the tablet from a dashboard

Pages can use `window.kiosky` in JavaScript, for example from a motion-sensor
automation on a Home Assistant dashboard:

| Call | What it does |
| --- | --- |
| `kiosky.wake()` | Wakes the screen (any page) |
| `kiosky.isScreenOff()` | `true` while the screen is off |
| `kiosky.version()` | Kiosky's version |
| `kiosky.reload()` | Reloads the page |
| `kiosky.setBrightness(30)` | Sets the brightness in percent; `0` goes back to the tablet's own |
| `kiosky.screenOff()` | Blacks the screen out until the next touch or `wake()` |
| `kiosky.deviceName()` | The tablet's name in Shepherd |

Everything below `version()` only works for your own page's site (on a managed
tablet: the start page's site and the allowed sites), and returns `false` (or an
empty name) anywhere else.

## Sign-ins and certificates

**Username and password prompts.** Some pages (behind a proxy, or older devices)
ask for a username and password in a browser box. Kiosky asks once and, with
**Remember on this tablet** ticked, signs in by itself after that. The details
are stored on the tablet unencrypted, so use an account that can only view the
dashboard.

**Self-signed certificates.** A server on your own network (Home Assistant, a NAS)
often has a certificate Android doesn't trust. Kiosky shows its SHA-256
fingerprint and asks whether to trust it; a yes is remembered for that site only.
If you didn't expect the question for a site, choose **Don't trust**.

To clear both, tick **Forget saved sign-ins and trusted certificates** in Settings.
On a managed tablet, both come from the server instead (*HTTP sign-in* and
*Trusted certificates*); an untrusted certificate shows its fingerprint on screen
and in Shepherd, ready to copy.

## Battery care

Tablets left on charge all the time wear their batteries out and can make them
swell. Kiosky reports the battery's temperature and health to Shepherd, which can
alert you when it runs hot.

**Limit charging to protect the battery** (in Settings) turns on the tablet's own
charging limit. Android has no general way for apps to do this, so it's
experimental and only works on Samsung tablets that have *Protect battery*
(charging stops at about 85%). It needs a one-time command from a computer with
adb:

`adb shell pm grant uk.lp0.ha android.permission.WRITE_SECURE_SETTINGS`

Without it, Kiosky can't change the setting; you can still turn *Protect battery*
on by hand in Settings › Battery. Kiosky only turns the limit off again if it
was the one that turned it on.

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
2. Unless Kiosky is the device owner, a checklist opens. Tap each **✗** item to
   switch it on (see below), then go back to Kiosky.
3. To unlock, swipe in from an edge to open Kiosky's menu, tap **Lockdown: On**,
   then **Turn off lockdown**. If a swipe shows nothing, **tap the top-left corner
   five times quickly** to open the menu instead.

### How strong is it?

Android only lets the tablet's *device owner* (or an MDM) lock it completely. That
works on every Android version and the button reads **Lockdown: On** (or **On
(MDM)**). Otherwise it reads **Lockdown: On (partial)**, and Kiosky combines
everything Android allows. Each checklist item closes another gap:

| Checklist item | What it does | Android versions |
| --- | --- | --- |
| **Accessibility helper** | Brings Kiosky straight back if another app, the Home screen or Recents opens. Closes the notification shade on Android 12+ | All |
| **Display over other apps** | Blocks the notification shade (up to 7.1). Lets Kiosky come back when left (10+) | 6.0 and later |
| **Kiosky is the Home app** | The Home button returns to Kiosky | All |
| **Screen pinning** | Blocks Home and Recents. Turn on *Ask for PIN before unpinning* so it can't simply be undone | All |

Kiosky also closes the notification shade by itself whenever it's pulled down,
which works up to Android 11 without the helper.

| | Device owner or MDM | Partial, all items ✓ |
| --- | --- | --- |
| Notification shade | Turned off | Closed straight away (up to 11 by itself, 12+ with the helper) |
| Home, Recents | Blocked | Blocked by pinning, and Kiosky comes back if they're reached |
| Power menu and lock screen | Blocked | Still work |
| Settings | Can't be reached | Kiosky comes back if it opens |
| Updates | Install without a prompt | Android asks to confirm |

> **Android 13 and later:** Android greys out the accessibility switch for apps
> that weren't installed from an app store. Open **App info › ⋮ › Allow restricted
> settings** for Kiosky first (the checklist has a shortcut), then turn the helper on.

### Make Kiosky the device owner

**Easiest, with Kiosky Shepherd:** factory-reset the tablet, tap six times in the
same spot on its first setup screen, and scan the enrollment code's
**Provisioning QR** from the Shepherd console. The tablet joins Wi-Fi, installs
Kiosky as the device owner and enrolls by itself.

Otherwise, it's done once per tablet, from a computer with
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

On a tablet managed by Shepherd, the administrator turns Lockdown on or off with
the **Lock the tablet to Kiosky** setting, and the tablet can't change it. After the
technician PIN, the **Lockdown** button opens the checklist so the missing items
can be switched on, and **Remove device owner** appears while lockdown is off.

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
- The server can reload the page, show a message or read one aloud, take a
  screenshot or watch the screen live, collect diagnostics, clear sign-ins, wake
  the screen, or restart Kiosky (or reboot the tablet, as device owner).
- If Kiosky ever crashes, it tells the server what went wrong when it next starts.
- The night schedule, dimming, playlist, screensaver, sound, daily restart, going
  back to the start page, the frozen-page check, file uploads, sign-ins,
  certificates and camera and microphone access come from the server, so
  **Settings** is hidden.
  Camera and microphone only work for the start page's site and the sites the
  administrator allows by name.
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
  restarting), Kiosky shows a *Reconnecting…* screen with the time and tries
  again every 10 seconds on its own. That includes servers answering with an error
  while they start, like Home Assistant's *502 Bad Gateway*. Tap **Try now** to
  retry straight away.
- **Page crashed?** If the page's renderer crashes or Android stops it to free
  memory, Kiosky starts it again on its own instead of closing.
- **Pop-ups close themselves.** A page's `alert()`, `confirm()` or `prompt()` box
  closes after 30 seconds (as OK for alerts, Cancel for the others), so a stray
  pop-up can't leave the panel stuck.
- **Links stay inside Kiosky.** Nothing opens in another app or browser.
- **Notification shade:** whenever Kiosky is on screen, it stops people pulling
  down the notification shade, even with Lockdown off. As device owner the shade
  is turned off completely. Otherwise Kiosky closes it as soon as it opens: by
  itself up to Android 11, and with the accessibility helper on Android 12 and
  later (see *Lock the tablet down*).
- **Screen stays on** while Kiosky is open. To dim it or switch it off at night,
  use **Settings** (see *Settings*).
- **Sound and video** can play without a tap first, which suits cameras and
  doorbells on a dashboard.
- **Back button:** hides the toolbar or this help if either is showing,
  otherwise goes back a page. On the first page, it closes Kiosky, unless a
  settings PIN is set or Lockdown is on.
- **Back to this page:** open the toolbar and tap **Help**. It opens over your
  page and never replaces it. To make this page the start page instead, clear
  the address box and tap **Go**.

## Tips for a wall-mounted tablet

- **Keep people in Kiosky:** turn on **Lockdown** (see *Lock the tablet down*),
  and set a **settings PIN** so the toolbar can't be used to change things.
- **Save the screen at night:** set a night schedule in **Settings**.
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
