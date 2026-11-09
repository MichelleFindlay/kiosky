![Kiosky-Logo](logo.png)

# Willkommen bei Kiosky

Kiosky zeigt eine einzige Webseite im Vollbild und hält sie dort: keine
Browser-Tabs, keine Adresszeile, und der Bildschirm bleibt an. Es ist für
Wandtablets und Dashboards gemacht, etwa Home Assistant oder Grafana.

Du siehst diese Seite, weil noch keine Adresse eingestellt ist.

> Kioskys eigene Schaltflächen und Einstellungen sind englisch beschriftet. Diese
> Hilfe nennt sie so, wie sie auf dem Bildschirm stehen, zum Beispiel **Settings**.

## Deine Seite einstellen

1. **Wische vom oberen Bildschirmrand nach unten**, dort wo der Pfeil hinzeigt.
   Vom unteren Rand nach oben geht auch. Die Systemleisten und die
   Kiosky-Werkzeugleiste erscheinen oben.
2. Tippe auf das Adressfeld und gib die Adresse deiner Seite ein, zum Beispiel
   `https://www.example.com`.
3. Tippe auf **Go** (oder drücke Enter).

Kiosky merkt sich die Adresse und öffnet sie bei jedem Start. Wenn du
`https://` weglässt, ergänzt Kiosky es.

> Nur `https://`-Adressen funktionieren. Einfache `http://`-Seiten werden
> blockiert und laden nicht.

## Die Werkzeugleiste

Wische jederzeit vom oberen oder unteren Rand herein, um die Werkzeugleiste
zurückzuholen. Beginne die Wischbewegung direkt am Rand des Glases: Eine
Bewegung, die weiter innen beginnt, geht stattdessen an die Webseite.

| Schaltfläche | Was sie tut |
| --- | --- |
| **Go** | Öffnet die Adresse im Feld und speichert sie als deine Seite |
| **Reload** | Lädt die aktuelle Seite neu |
| **Help** | Zeigt diese Seite über deiner. **Close** (oder Zurück) bringt dich zu deiner Seite zurück, die darunter geladen bleibt |
| **Settings** | Nachtplan, Dimmen, Rückkehr zur Startseite, Kamera und Mikrofon und die Einstellungs-PIN (siehe *Einstellungen*) |
| **Update** | Sucht nach einer neueren Kiosky-Version |
| **Start on boot** | Schaltet das automatische Öffnen von Kiosky beim Hochfahren ein oder aus |
| **Enroll** | Übergibt dieses Tablet an einen Shepherd-Verwaltungsserver (siehe unten) |
| **Lockdown** | Sperrt das Tablet auf Kiosky, sodass Systemmenüs und -tasten es nicht verlassen können |
| **Remove device owner** | Macht die Einrichtung als Geräteinhaber rückgängig (nur sichtbar, wenn Lockdown aus ist) |
| **✕** | Blendet die Werkzeugleiste aus |

Die Werkzeugleiste blendet sich nach 15 Sekunden selbst aus, außer du tippst
gerade darin.

## Einstellungen

Tippe in der Werkzeugleiste auf **Settings**. Alles hier ist optional; lass eine
Uhrzeit leer, um den jeweiligen Teil auszuschalten.

### Bildschirm nachts aus

Stelle **Off at** und **On again at** ein (24 Stunden, `HH:MM`, zum Beispiel
`23:00` und `06:30`). Die Zeiten dürfen über Mitternacht gehen. Dazwischen wird
der Bildschirm schwarz.

- **A touch wakes it**: Die erste Berührung weckt nur den Bildschirm und drückt
  nichts auf der Seite. Er bleibt **Stays awake for** Minuten an und wird dann
  wieder dunkel.
- **Someone close by wakes it**: nutzt den Näherungssensor des Tablets (eine Hand
  nahe der Oberkante) oder den Lichtsensor (ein eingeschaltetes Raumlicht). Nicht
  jedes Tablet hat einen.
- Ist keines von beiden angehakt, lässt Kiosky den Bildschirm richtig ausgehen und
  schaltet ihn zur *On again*-Zeit wieder ein. **Fünf schnelle Tipper** irgendwo
  wecken ihn trotzdem, damit du immer wieder hineinkommst.

Wenn die Aus-Zeit endet, lädt Kiosky die Seite neu, damit sie aktuell ist.

### Dimmen

**Dim from** und **Until** senken die Helligkeit für einen Teil des Tages auf den
Prozentwert **Dimmed brightness**. **Also dim after this many minutes untouched**
dimmt den Bildschirm, sobald ihn eine Weile niemand benutzt hat. Die erste
Berührung macht ihn wieder hell, ohne etwas zu drücken.

### Seite

- **Back to the start page after this many minutes untouched**: Wenn jemand auf
  eine andere Seite tippt und weggeht, kehrt Kiosky von selbst zu deiner Seite
  zurück.
- **Reload a frozen page after this many minutes**: Kiosky prüft alle paar
  Sekunden, ob die Seite noch reagiert. Hört sie so lange auf, lädt Kiosky sie neu
  und startet sie, falls das nicht hilft, ganz von vorn. Standardmäßig an (2
  Minuten).
- **Pages may use the camera / microphone**: siehe *Kamera und Mikrofon*.
- **Shared tablet**: für ein Tablet, das die Öffentlichkeit benutzt, etwa eine
  Besucheranmeldung. Wenn Kiosky nach den oben eingestellten Minuten zur
  Startseite zurückkehrt, meldet es sich auch überall ab: Cookies, gespeicherte
  Website-Daten, Cache und Verlauf. Das passiert nur, wenn seit dem letzten Mal
  jemand das Tablet berührt hat. Hake es nicht für ein Dashboard an, bei dem du
  dich einmal anmeldest, sonst wirst auch du abgemeldet.

### Berührung

- **Pinch to zoom**: standardmäßig aus, damit eine versehentliche
  Zwei-Finger-Berührung das Dashboard nicht vergrößert zurücklässt. Schalte es
  ein, wenn Leute die Seite vergrößern müssen.
- **Long press**: Hake es ab, damit langes Drücken nichts tut: keine Textauswahl
  mit ihrem Menü Kopieren/Teilen/Suchen (das zu anderen Apps führen kann) und keine
  Linkvorschau. Textfelder verlieren dann auch ihr Einfügen-Menü.
- **Selecting the page's text**: Hake es ab, damit Doppeltippen oder langes
  Drücken den Text der Seite nicht markieren kann. Textfelder funktionieren
  weiterhin.

### Scanner

- **Read NFC tags**: Solange Kiosky im Vordergrund ist, wird ein ans Tablet
  gehaltener Tag gelesen und an die Seite (siehe *Das Tablet von einem Dashboard
  aus steuern*) und an Home Assistant geschickt. Außerdem können Tags dann keine
  anderen Apps öffnen. NFC muss in den Android-Einstellungen eingeschaltet sein.
- **Barcode-Scanner**: USB- und Bluetooth-Scanner tippen wie eine sehr schnelle
  Tastatur. Kiosky erkennt das und schickt jeden Scan zusätzlich an die Seite und
  an Home Assistant; der Scan landet wie bisher auch in einem aktiven Textfeld.

### Playlist

Liste weitere Seiten auf (eine `https://`-Adresse pro Zeile), die nach deiner
Startseite der Reihe nach gezeigt werden, jede für **Show each page for**
Sekunden. Eine Berührung hält den Wechsel eine Weile an, damit niemand beim
Benutzen einer Seite unterbrochen wird.

### Bildschirmschoner

Nach **Show a clock after this many minutes untouched** wandern eine große Uhr
und das Datum langsam über einen schwarzen Bildschirm (damit sich nichts
einbrennt). Gib eine Seitenadresse an, um stattdessen diese zu zeigen, etwa einen
Bilderrahmen. Die erste Berührung schließt ihn, ohne etwas zu drücken.

### Ton

**Volume** legt die Medienlautstärke fest (leer lassen, um die des Tablets zu
behalten). **Quiet from** und **Until** senken sie für einen Teil des Tages oder
schalten sie bei `0` stumm. Kiosky ändert die Lautstärke nur, wenn sich der
gewünschte Wert ändert, sodass man sie bis zur nächsten Änderung noch von Hand
lauter oder leiser stellen kann.

### Tablet

- **Restart every day at**: ein täglicher Neustart (zum Beispiel um `04:00`)
  beseitigt schleichende Speicherlecks lang laufender Dashboards. Als
  Geräteinhaber (Android 7+) ist es ein vollständiger Neustart des Tablets, sonst
  startet sich Kiosky selbst neu.
- **Pages may upload files**: lässt Schaltflächen zur Dateiauswahl die
  Android-Dateiauswahl öffnen. Schalte es auf öffentlichen Tablets aus, die es
  nicht brauchen.
- **Forget saved sign-ins and trusted certificates**: siehe *Anmeldungen und
  Zertifikate*.
- **Move the page a pixel or two every few minutes**: verschiebt alles langsam auf
  einem winzigen Kreis (höchstens 2 Pixel), damit sich ein ganztägig gezeigtes
  Dashboard nicht in günstigere LCD- und OLED-Bildschirme einbrennt. Niemand
  bemerkt es.
- **While reconnecting, show the last page greyed out**: siehe *Gut zu wissen*.

### Home Assistant (MQTT)

Siehe *Home Assistant und MQTT* weiter unten.

### Einstellungs-PIN

Tippe unten in den Einstellungen auf **Set a PIN…**. Danach fragt das
Hereinwischen vom Rand (oder die fünf Tipper in die Ecke) nach der PIN, bevor sich
die Werkzeugleiste öffnet, die Zurück-Taste schließt Kiosky nicht mehr, und auch
Registrierungslinks brauchen die PIN. Mit **PIN…** änderst oder entfernst du sie.

> Ist die PIN vergessen, hilft nur Einstellungen › Apps › Kiosky › Speicher ›
> **Speicher löschen** (oder `adb shell pm clear uk.lp0.ha`). Dabei gehen auch die
> Adresse und die Anmeldungen verloren.

## Kamera und Mikrofon

Video-Türklingeln, Gegensprechanlagen und Zwei-Wege-Audio (zum Beispiel in Home
Assistant oder Frigate) brauchen Kamera oder Mikrofon. Wenn eine Seite fragt:

- Die Website deiner eigenen Seite bekommt sie sofort. Beim ersten Mal fragt
  Android, ob Kiosky Kamera oder Mikrofon benutzen darf. Als Geräteinhaber erteilt
  Kiosky das selbst.
- Jede andere Website fragt einmal (**Allow** oder **Block**), und ein Ja wird
  gespeichert.
- Hake **Pages may use the camera** oder **microphone** in den Einstellungen ab,
  um sie überall abzulehnen.

Nur `https://`-Seiten können sie benutzen.

### Das Tablet von einem Dashboard aus steuern

Seiten können in JavaScript `window.kiosky` benutzen, zum Beispiel aus einer
Bewegungsmelder-Automation auf einem Home-Assistant-Dashboard:

| Aufruf | Was er tut |
| --- | --- |
| `kiosky.wake()` | Weckt den Bildschirm (jede Seite) |
| `kiosky.isScreenOff()` | `true`, solange der Bildschirm aus ist |
| `kiosky.version()` | Kioskys Version |
| `kiosky.reload()` | Lädt die Seite neu |
| `kiosky.setBrightness(30)` | Setzt die Helligkeit in Prozent; `0` kehrt zur Helligkeit des Tablets zurück |
| `kiosky.screenOff()` | Schwärzt den Bildschirm bis zur nächsten Berührung oder `wake()` |
| `kiosky.deviceName()` | Der Name des Tablets in Shepherd |
| `kiosky.speak("Hallo")` | Liest den Text vor |
| `kiosky.battery()` | JSON-Text: `{"level": 80, "charging": true, "temperature": 31.5}` |
| `kiosky.setUrl("https://…")` | Öffnet eine Seite, bis Kiosky zur Startseite zurückkehrt |
| `kiosky.watchMotion(true)` | Schickt `kiosky:motion`-Ereignisse (siehe unten), bis die Seite wechselt; `false` ohne Sensor |
| `kiosky.nfcAvailable()` | `true`, solange NFC-Tags gelesen werden |

Alles unterhalb von `version()` funktioniert nur für die Website deiner eigenen
Seite (auf einem verwalteten Tablet: die Website der Startseite und die erlaubten
Websites) und gibt überall sonst `false` (oder einen leeren Namen) zurück.

Kiosky schickt deiner eigenen Seite außerdem Ereignisse, damit sie reagieren
kann, ohne ständig nachzufragen:

```
window.addEventListener('kiosky:motion', (e) => console.log(e.detail.source)); // "proximity" oder "light"
window.addEventListener('kiosky:nfc', (e) => console.log(e.detail.id, e.detail.records));
window.addEventListener('kiosky:barcode', (e) => console.log(e.detail.code));
```

Ein NFC-Tag liefert seine `id` (hex), `techs` und `records`: jeweils mit `text`,
`uri` oder `mime` und `data` (Base64). Scans wecken auch den Bildschirm.

## Home Assistant und MQTT

Kiosky kann sich mit einem MQTT-Broker verbinden (etwa dem Mosquitto-Add-on in
Home Assistant), damit dein Heimserver das Tablet sehen und steuern kann. Gib unter
**Settings › Home Assistant (MQTT)** die Adresse des Brokers ein und, falls nötig,
Benutzername und Passwort. Die Zeile über den Feldern zeigt, ob Kiosky verbunden
ist.

Mit angehaktem **Add it to Home Assistant automatically** erscheint das Tablet in
Home Assistant als Gerät (Einstellungen › Geräte › MQTT) mit:

| Entität | Was sie tut |
| --- | --- |
| **Screen** | Schalter: aus schwärzt den Bildschirm bis zur nächsten Berührung; an weckt ihn |
| **Brightness** | 1–100 % |
| **Volume** | Medienlautstärke, 0–100 % |
| **Page** | Zeigt die Adresse auf dem Bildschirm; setze sie, um eine andere Seite zu öffnen |
| **Reload**, **Start page** | Schaltflächen |
| **Announce** | Liest eine Nachricht vor (`notify.send_message`) |
| **Motion** | An, wenn jemand das Tablet berührt oder der Näherungs- oder Lichtsensor jemanden bemerkt, 30 Sekunden später aus |
| **Battery**, **Charging**, **Battery temperature**, **Wi-Fi signal**, **Page title** | Sensoren |
| **NFC tag**, **Barcode** | Ereignisse, wenn diese Scanner an sind: Automationen durch einen Scan auslösen |

Eine von Home Assistant geöffnete Seite bleibt, bis Kiosky zur Startseite
zurückkehrt (nach den unter *Seite* eingestellten Minuten oder mit **Start page**).

**Ohne Discovery** benutzt du die Topics direkt. Das Basis-Topic ist `kiosky/`
gefolgt von einem Teil der Tablet-ID (in den Einstellungen angezeigt) oder dein
eigenes **Topic**:

| Topic | Nutzdaten |
| --- | --- |
| `<base>/availability` | `online` oder `offline` (Kiosky veröffentlicht) |
| `<base>/state` | JSON: `screen` (`on`, `dim`, `off`), `brightness`, `volume`, `url`, `title`, `battery_level`, `charging`, `battery_temperature`, `wifi_rssi`, `screensaver`, `offline`, `app_version` |
| `<base>/motion` | `ON` oder `OFF` |
| `<base>/cmd/wake` | Optional: Minuten, die er wach bleiben soll |
| `<base>/cmd/screen` | `ON` oder `OFF` |
| `<base>/cmd/screen_off` | Beliebig |
| `<base>/cmd/brightness`, `<base>/cmd/volume` | Ein Prozentwert |
| `<base>/cmd/set_url` | Eine `https://`- (oder `http://`-) Adresse |
| `<base>/cmd/go_home`, `<base>/cmd/reload` | Beliebig |
| `<base>/cmd/announce` | Text oder `{"text": "…", "volume": 80}` |
| `<base>/cmd/clear_session` | Beliebig: meldet sich überall ab, wie bei einem geteilten Tablet |
| `<base>/nfc`, `<base>/barcode` | Scans (Kiosky veröffentlicht): JSON mit `event_type: "scanned"` und den Angaben von oben |

Zum Beispiel, um den Bildschirm zu wecken, wenn es an der Tür klingelt:

```
action: mqtt.publish
data:
  topic: kiosky/1a2b3c4d/cmd/wake
  payload: "5"
```

**Sicherheit.** Wer an den Broker veröffentlichen kann, kann das Tablet steuern;
gib dem Broker also ein Passwort. **Use TLS** verschlüsselt die Verbindung (meist
Port 8883). Für einen Broker mit selbst signiertem Zertifikat gib dessen
SHA-256-**Zertifikat-Fingerabdruck** ein; Kiosky vertraut dann genau diesem
Zertifikat.

## Anmeldungen und Zertifikate

**Abfragen nach Benutzername und Passwort.** Manche Seiten (hinter einem Proxy oder
auf älteren Geräten) fragen in einem Browserfenster nach Benutzername und Passwort.
Kiosky fragt einmal und meldet sich mit angehaktem **Remember on this tablet**
danach selbst an. Die Angaben werden unverschlüsselt auf dem Tablet gespeichert,
benutze also ein Konto, das das Dashboard nur ansehen kann.

**Selbst signierte Zertifikate.** Ein Server in deinem eigenen Netz (Home
Assistant, ein NAS) hat oft ein Zertifikat, dem Android nicht vertraut. Kiosky
zeigt seinen SHA-256-Fingerabdruck und fragt, ob es vertrauen soll; ein Ja wird nur
für diese Website gespeichert. Hast du die Frage für eine Website nicht erwartet,
wähle **Don't trust**.

Um beides zu löschen, hake **Forget saved sign-ins and trusted certificates** in
den Einstellungen an. Auf einem verwalteten Tablet kommt beides stattdessen vom
Server (*HTTP sign-in* und *Trusted certificates*); ein nicht vertrauenswürdiges
Zertifikat zeigt seinen Fingerabdruck auf dem Bildschirm und in Shepherd, bereit
zum Kopieren.

## Akkupflege

Tablets, die ständig am Ladegerät hängen, nutzen ihre Akkus ab und können sie
aufblähen lassen. Kiosky meldet Temperatur und Zustand des Akkus an Shepherd, das
dich warnen kann, wenn er heiß wird.

**Limit charging to protect the battery** (in den Einstellungen) schaltet die
eigene Ladebegrenzung des Tablets ein. Android bietet Apps dafür keinen
allgemeinen Weg, deshalb ist es experimentell und funktioniert nur auf
Samsung-Tablets mit *Akku schützen* (das Laden stoppt bei etwa 85 %). Es braucht
einmalig einen Befehl von einem Computer mit adb:

`adb shell pm grant uk.lp0.ha android.permission.WRITE_SECURE_SETTINGS`

Ohne ihn kann Kiosky die Einstellung nicht ändern; du kannst *Akku schützen*
trotzdem von Hand unter Einstellungen › Akku einschalten. Kiosky schaltet die
Begrenzung nur dann wieder aus, wenn es sie selbst eingeschaltet hat.

## Updates

Kiosky sucht eine Minute nach dem Start nach einer neuen Version, danach alle 12
Stunden. Ist eine verfügbar, erscheint unten am Bildschirm eine Leiste:

1. Tippe auf **Install**. Kiosky lädt das Update herunter.
2. Beim ersten Mal bittet Android darum, Kiosky das Installieren von Apps zu
   erlauben. Schalte **Dieser Quelle vertrauen** ein, geh zurück und tippe
   erneut auf **Install**.
3. Bestätige auf Androids Installationsbildschirm. Kiosky schließt sich,
   aktualisiert sich und öffnet sich normalerweise von selbst wieder. Ab Android
   10 braucht das dieselbe Berechtigung wie **Start on boot** (unten). Öffnet es
   sich nicht wieder, tippe auf das Kiosky-Symbol.

Tippe auf **Later**, um die Leiste bis zum nächsten Neustart von Kiosky
auszublenden. Gespeicherte Adresse, Anmeldungen und Einstellungen bleiben bei
Updates erhalten.

Updates kommen aus den Veröffentlichungen des Projekts auf GitHub. Android
installiert nur ein Update, das mit demselben Schlüssel signiert ist wie die
bereits installierte Kopie, eine manipulierte Datei wird also abgelehnt.

**Wenn ein Update schiefgeht.** In den ersten 15 Minuten nach dem ersten Start
einer neuen Version achtet Kiosky auf Abstürze. Stürzt es in dieser Zeit zweimal
ab, startet es im *abgesicherten Modus*: MQTT, die Bewegungssensoren und
WLAN-Änderungen sind aus, und es öffnet die letzte Seite nicht wieder, damit es
weiterläuft und das nächste Update empfangen kann. Auf einem verwalteten Tablet
meldet es das auch dem Server. Android lässt eine App nie von selbst zu einer
älteren Version zurückkehren, die Lösung ist also immer eine neuere.

## Automatisch starten

Kiosky kann sich von selbst öffnen, wann immer das Tablet eingeschaltet wird oder
neu startet, sodass ein Wandpanel nach einem Stromausfall ohne Zutun zurückkommt.
Es ist aus, bis du es einschaltest:

1. Öffne die Werkzeugleiste und tippe auf **Start on boot: Off**. Es wechselt auf
   **On**.
2. Ab Android 10 öffnet Kiosky dann einen Einstellungsbildschirm. Schalte dort
   **Über anderen Apps einblenden** für Kiosky ein und geh zurück. Bis dahin
   steht auf der Schaltfläche **Start on boot: Needs permission**.
3. Starte das Tablet zur Probe neu. Kiosky öffnet sich, sobald der Start
   abgeschlossen ist, was eine Minute dauern kann.

Kiosky öffnet sich, nachdem das Tablet zum ersten Mal entsperrt wurde; stelle die
Displaysperre für ein Wandpanel also auf *Keine* oder *Wischen*. Manche Marken
(etwa Xiaomi, Huawei und Oppo) haben außerdem eine eigene *Autostart*-Einstellung,
die für Kiosky eingeschaltet werden muss.

Tippe erneut auf die Schaltfläche, um es auszuschalten.

## Das Tablet sperren

Normalerweise kann jeder die Benachrichtigungsleiste herunterziehen oder auf Home
drücken, um Kiosky zu verlassen. **Lockdown** verhindert das: Kioskys eigenes Menü
wird zum einzigen Ausweg.

1. Öffne die Werkzeugleiste und tippe auf **Lockdown: Off**.
2. Sofern Kiosky nicht der Geräteinhaber ist, öffnet sich eine Checkliste. Tippe
   auf jeden **✗**-Punkt, um ihn einzuschalten (siehe unten), und geh dann zu
   Kiosky zurück.
3. Zum Entsperren wische vom Rand herein, um Kioskys Menü zu öffnen, tippe auf
   **Lockdown: On** und dann auf **Turn off lockdown**. Zeigt eine Wischbewegung
   nichts, **tippe fünfmal schnell in die linke obere Ecke**, um das Menü
   stattdessen zu öffnen.

### Wie sicher ist das?

Android lässt nur den *Geräteinhaber* des Tablets (oder ein MDM) es vollständig
sperren. Das funktioniert auf jeder Android-Version, und auf der Schaltfläche
steht **Lockdown: On** (oder **On (MDM)**). Andernfalls steht dort **Lockdown: On
(partial)**, und Kiosky kombiniert alles, was Android erlaubt. Jeder Punkt der
Checkliste schließt eine weitere Lücke:

| Punkt der Checkliste | Was er tut | Android-Versionen |
| --- | --- | --- |
| **Accessibility helper** | Holt Kiosky sofort zurück, wenn sich eine andere App, der Startbildschirm oder die Übersicht öffnet. Schließt ab Android 12 die Benachrichtigungsleiste | Alle |
| **Display over other apps** | Blockiert die Benachrichtigungsleiste (bis 7.1). Lässt Kiosky zurückkommen, wenn es verlassen wurde (ab 10) | Ab 6.0 |
| **Kiosky is the Home app** | Die Home-Taste führt zu Kiosky zurück | Alle |
| **Screen pinning** | Blockiert Home und Übersicht. Schalte *Vor dem Loslösen nach PIN fragen* ein, damit es sich nicht einfach rückgängig machen lässt | Alle |

Kiosky schließt die Benachrichtigungsleiste außerdem selbst, sobald sie
heruntergezogen wird; das funktioniert bis Android 11 auch ohne den Helfer.

| | Geräteinhaber oder MDM | Teilweise, alle Punkte ✓ |
| --- | --- | --- |
| Benachrichtigungsleiste | Ausgeschaltet | Sofort geschlossen (bis 11 von selbst, ab 12 mit dem Helfer) |
| Home, Übersicht | Blockiert | Durch Anheften blockiert, und Kiosky kommt zurück, falls sie erreicht werden |
| Ein/Aus-Menü und Sperrbildschirm | Blockiert | Funktionieren weiterhin |
| Einstellungen | Nicht erreichbar | Kiosky kommt zurück, wenn sie sich öffnen |
| Updates | Installieren ohne Nachfrage | Android bittet um Bestätigung |

> **Ab Android 13:** Android graut den Bedienungshilfen-Schalter für Apps aus, die
> nicht aus einem App-Store installiert wurden. Öffne für Kiosky zuerst
> **App-Info › ⋮ › Eingeschränkte Einstellungen zulassen** (die Checkliste hat
> eine Abkürzung dorthin) und schalte dann den Helfer ein.

### Kiosky zum Geräteinhaber machen

**Am einfachsten, mit Kiosky Shepherd:** Setze das Tablet auf Werkseinstellungen
zurück, tippe auf dem ersten Einrichtungsbildschirm sechsmal auf dieselbe Stelle
und scanne den **Provisioning QR** des Registrierungscodes aus der
Shepherd-Konsole. Das Tablet verbindet sich mit dem WLAN, installiert Kiosky als
Geräteinhaber und registriert sich selbst.

Andernfalls geschieht es einmal pro Tablet von einem Computer mit
[adb](https://developer.android.com/tools/adb):

1. Entferne auf dem Tablet alle Konten unter Einstellungen › Konten (Android
   verweigert es, solange ein Konto angemeldet ist). Du kannst sie danach wieder
   hinzufügen.
2. Schalte **USB-Debugging** ein (Einstellungen › Über das Tablet › siebenmal auf
   *Build-Nummer* tippen, dann Einstellungen › Entwickleroptionen).
3. Schließe das Tablet an und führe aus:
   `adb shell dpm set-device-owner uk.lp0.ha/kiosky.AdminReceiver`
4. Öffne Kiosky und schalte **Lockdown** ein. Auf der Schaltfläche steht
   **Lockdown: On** statt **Lockdown: On (pinned)**.

> Ein Geräteinhaber lässt sich nicht deinstallieren. Um Kiosky später zu
> entfernen, schalte Lockdown aus und tippe zuerst auf **Remove device owner**,
> oder setze das Tablet auf Werkseinstellungen zurück.

Auf einem von Shepherd verwalteten Tablet schaltet der Administrator Lockdown mit
der Einstellung **Lock the tablet to Kiosky** ein oder aus, und das Tablet kann es
nicht ändern. Nach der Techniker-PIN öffnet die Schaltfläche **Lockdown** die
Checkliste, damit fehlende Punkte eingeschaltet werden können, und **Remove device
owner** erscheint, solange Lockdown aus ist.

## Verwaltete Tablets (Shepherd)

Eine Organisation kann einen **Kiosky Shepherd**-Server betreiben, um viele
Tablets zugleich zu betreuen. Sobald ein Tablet registriert ist, wählt der Server
seine Seite und Einstellungen, und das Tablet meldet zurück, wie es ihm geht. Die
eigenen Bedienelemente des Tablets sind gesperrt.

### Ein Tablet registrieren

Du brauchst einen Registrierungscode von deinem Administrator. Jeder dieser Wege
funktioniert:

- **Scanne den QR-Code** mit der Kamera-App des Tablets und öffne den Link, oder
- öffne den **Registrierungslink** auf dem Tablet, oder
- öffne die Werkzeugleiste, tippe auf **Enroll** und gib die **Server**-Adresse
  und den **Code** ein (Bindestriche und Großschreibung spielen keine Rolle).
  **Scan QR** benutzt eine Scanner-App, falls eine installiert ist.

Kiosky bittet um Bestätigung, bevor es das Tablet übergibt. Ein bereits
verwaltetes Tablet ignoriert Registrierungslinks und -codes.

### Was sich ändert, wenn es verwaltet wird

- Der Server legt die Seite fest. Hat er noch keine gewählt, zeigt Kiosky
  *Waiting for configuration*.
- Die Werkzeugleiste kann ganz ausgeblendet sein oder nur **Reload** und **Help**
  zeigen, je nachdem, was der Administrator gewählt hat. **Update**, **Start on
  boot** und **Enroll** verschwinden.
- Nur Websites, die der Administrator erlaubt, lassen sich öffnen.
- Updates, Autostart, Bildschirmausrichtung, Textgröße und ähnliche Einstellungen
  kommen vom Server.
- Der Server kann die Seite neu laden, eine Nachricht anzeigen oder vorlesen,
  einen Screenshot machen oder den Bildschirm live ansehen, Diagnosedaten
  sammeln, Anmeldungen löschen, den Bildschirm wecken oder Kiosky neu starten
  (oder als Geräteinhaber das Tablet neu starten).
- Stürzt Kiosky einmal ab, teilt es dem Server beim nächsten Start mit, was
  schiefging.
- Nachtplan, Dimmen, Playlist, Bildschirmschoner, Ton, täglicher Neustart,
  Rückkehr zur Startseite, die Prüfung auf eingefrorene Seiten, Datei-Uploads,
  Anmeldungen, Zertifikate sowie Kamera- und Mikrofonzugriff kommen vom Server,
  deshalb ist **Settings** ausgeblendet. Kamera und Mikrofon funktionieren nur für
  die Website der Startseite und die Websites, die der Administrator namentlich
  erlaubt.
- Kiosky meldet die angezeigte Seite, Ladefehler, Akku, WLAN-Signal und ob die
  Berechtigungen für Autostart und Updates erteilt sind.
- Der Server kann WLAN-Netze auf dem Tablet speichern (als Geräteinhaber, ab
  Android 10), sodass ein neuer Router oder ein neues Passwort keinen Besuch vor
  Ort braucht. Ein geändertes Passwort, das sich nicht innerhalb von 10 Minuten
  verbindet, wird wieder auf den alten Stand gesetzt.

Verliert das Tablet den Kontakt zum Server, behält es seine letzten Einstellungen
und bleibt gesperrt. Freigegeben wird es erst, wenn der Administrator es in
Shepherd entfernt. Kiosky zeigt dann *This tablet is no longer managed* und
behält seine aktuelle Seite.

### Techniker-PIN

Hat der Administrator eine PIN festgelegt, tippe fünfmal schnell in die **linke
obere Ecke** des Bildschirms und gib die PIN ein. Die vollständige Werkzeugleiste
öffnet sich, bis sie sich wieder ausblendet, mit **Device info** (Server,
Gerätename, letzter Kontakt) und Schaltflächen, um fehlende Berechtigungen zu
erteilen. Nach fünf falschen PINs ist die Eingabe 5 Minuten gesperrt.

## Gut zu wissen

- **Anmeldungen bleiben erhalten.** Cookies und Website-Speicher werden
  gesichert, du bleibst also nach einem Neustart der App oder des Tablets
  angemeldet (außer **Shared tablet** ist in den Einstellungen angehakt).
- **Verbindung verloren?** Ist die Seite nicht erreichbar (WLAN wacht gerade auf,
  Server startet neu), zeigt Kiosky das letzte gute Bild der Seite ausgegraut mit
  einem *Reconnecting…*-Banner (oder einen ganzen *Reconnecting…*-Bildschirm, wenn
  es von dieser Website noch kein Bild hat) und versucht es von selbst alle 10
  Sekunden erneut. Das gilt auch für Server, die beim Starten mit einem Fehler
  antworten, wie Home Assistants *502 Bad Gateway*. Tippe auf **Try now**, um es
  sofort erneut zu versuchen.
- **Seite abgestürzt?** Stürzt der Renderer der Seite ab oder beendet Android ihn,
  um Speicher freizugeben, startet Kiosky ihn von selbst neu, statt sich zu
  schließen.
- **Pop-ups schließen sich selbst.** Ein `alert()`, `confirm()` oder `prompt()`
  einer Seite schließt sich nach 30 Sekunden (bei Hinweisen als OK, sonst als
  Abbrechen), damit ein verirrtes Pop-up das Panel nicht hängen lässt.
- **Links bleiben in Kiosky.** Nichts öffnet sich in einer anderen App oder einem
  Browser.
- **Benachrichtigungsleiste:** Solange Kiosky auf dem Bildschirm ist, verhindert
  es das Herunterziehen der Benachrichtigungsleiste, auch ohne Lockdown. Als
  Geräteinhaber ist die Leiste ganz ausgeschaltet. Sonst schließt Kiosky sie,
  sobald sie sich öffnet: bis Android 11 von selbst, ab Android 12 mit dem
  Bedienungshilfen-Helfer (siehe *Das Tablet sperren*).
- **Der Bildschirm bleibt an**, solange Kiosky offen ist. Um ihn zu dimmen oder
  nachts auszuschalten, benutze **Settings** (siehe *Einstellungen*).
- **Ton und Video** können ohne vorheriges Tippen abspielen, was zu Kameras und
  Türklingeln auf einem Dashboard passt.
- **Zurück-Taste:** blendet die Werkzeugleiste oder diese Hilfe aus, falls eine
  davon angezeigt wird, sonst geht sie eine Seite zurück. Auf der ersten Seite
  schließt sie Kiosky, außer eine Einstellungs-PIN ist gesetzt oder Lockdown ist
  an.
- **Zurück zu dieser Seite:** Öffne die Werkzeugleiste und tippe auf **Help**. Sie
  öffnet sich über deiner Seite und ersetzt sie nie. Um stattdessen diese Seite zur
  Startseite zu machen, leere das Adressfeld und tippe auf **Go**.

## Tipps für ein Wandtablet

- **Leute in Kiosky halten:** Schalte **Lockdown** ein (siehe *Das Tablet
  sperren*) und setze eine **Einstellungs-PIN**, damit die Werkzeugleiste nicht
  zum Ändern benutzt werden kann.
- **Den Bildschirm nachts schonen:** Stelle in **Settings** einen Nachtplan ein.
- **Verhindern, dass Android es schließt:** Stelle die Akkunutzung von Kiosky
  unter Einstellungen › Apps › Kiosky › Akku auf *Nicht eingeschränkt*.
- **Lass es am Ladegerät.** Der Bildschirm bleibt an, es braucht also mehr Strom
  als sonst.

## Wenn etwas nicht stimmt

- **Leerer Bildschirm oder „Seite nicht verfügbar“:** Prüfe die Adresse in der
  Werkzeugleiste und dass sie mit `https://` beginnt. Kiosky versucht es weiter,
  eine Seite, die wieder online ist, lädt also von selbst.
- **Jedes Mal abgemeldet:** Achte darauf, dass deine Website die Anmeldung nicht
  beim Schließen des Browsers löscht.
- **Die Werkzeugleiste erscheint nicht:** Wische langsam vom äußersten Rand des
  Bildschirms.

## Über

Kiosky {{version}} · Freie Software unter der GNU General Public License v3.0.
Quellcode und Veröffentlichungen: [github.com/MichelleFindlay/kiosky](https://github.com/MichelleFindlay/kiosky)

### Danksagung

Kiosky wird ohne das Android SDK gebaut, mit diesen Open-Source-Projekten:

- [Apktool](https://github.com/iBotPeaches/Apktool) von Connor Tumbleson und
  Mitwirkenden (Apache License 2.0) packt Manifest und Ressourcen der App in eine
  APK.
- [dex2jar](https://github.com/pxb1988/dex2jar) von Bob Pan und Mitwirkenden
  (Apache License 2.0) wandelt Java-Code in Androids dex-Format um.
- [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer) von Patrick
  Favre-Bulle (Apache License 2.0) richtet die APK aus und signiert sie, mit
  `zipalign` und `apksig` aus dem Android Open Source Project.
- [OpenJDK](https://openjdk.org) (GPL v2 mit der Classpath Exception) kompiliert
  den Java-Code.

Seiten werden von Android System WebView angezeigt, das aus dem
[Chromium](https://www.chromium.org)-Projekt gebaut wird (BSD-Lizenz).
