![Logo de Kiosky](logo.png)

# Bienvenue dans Kiosky

Kiosky affiche une seule page web en plein écran et l'y maintient : pas d'onglets,
pas de barre d'adresse, et l'écran reste allumé. Il est conçu pour les tablettes
murales et les tableaux de bord, comme Home Assistant ou Grafana.

Vous voyez cette page parce qu'aucune adresse n'a encore été définie.

> Les boutons et réglages de Kiosky sont en anglais. Cette aide les nomme tels
> qu'ils apparaissent à l'écran, par exemple **Settings**.

## Choisir votre page

1. **Balayez vers le bas depuis le bord supérieur** de l'écran, là où pointe la
   flèche. Balayer vers le haut depuis le bord inférieur fonctionne aussi. Les
   barres système et la barre d'outils de Kiosky apparaissent en haut.
2. Touchez le champ d'adresse et saisissez l'adresse de votre page, par exemple
   `https://www.example.com`.
3. Touchez **Go** (ou appuyez sur Entrée).

Kiosky retient cette adresse et l'ouvre à chaque démarrage. Si vous omettez
`https://`, Kiosky l'ajoute pour vous.

> Seules les adresses `https://` fonctionnent. Les pages en simple `http://` sont
> bloquées et ne se chargent pas.

## La barre d'outils

Balayez depuis le bord supérieur ou inférieur à tout moment pour faire revenir la
barre d'outils. Commencez le geste tout au bord de la vitre : un geste qui
commence plus à l'intérieur va à la page web.

| Bouton | Ce qu'il fait |
| --- | --- |
| **Go** | Ouvre l'adresse du champ et l'enregistre comme votre page |
| **Reload** | Recharge la page actuelle |
| **Help** | Affiche cette page par-dessus la vôtre. **Close** (ou Retour) ramène à votre page, qui reste chargée en dessous |
| **Settings** | Programme de nuit, atténuation, retour à la page d'accueil, caméra et micro, et code PIN des réglages (voir *Réglages*) |
| **Update** | Cherche une version plus récente de Kiosky |
| **Start on boot** | Active ou désactive l'ouverture automatique de Kiosky au démarrage |
| **Enroll** | Confie cette tablette à un serveur de gestion Shepherd (voir plus bas) |
| **Lockdown** | Verrouille la tablette sur Kiosky, pour que les menus et boutons système ne permettent pas d'en sortir |
| **Remove device owner** | Annule la configuration en propriétaire de l'appareil (visible seulement si Lockdown est désactivé) |
| **✕** | Masque la barre d'outils |

La barre d'outils se masque d'elle-même après 15 secondes, sauf si vous êtes en
train d'y écrire.

## Réglages

Touchez **Settings** dans la barre d'outils. Tout y est facultatif ; laissez une
heure vide pour désactiver la partie correspondante.

### Écran éteint la nuit

Réglez **Off at** et **On again at** (format 24 heures `HH:MM`, par exemple
`23:00` et `06:30`). Les heures peuvent passer minuit. Entre les deux, l'écran
devient noir.

- **A touch wakes it** : le premier contact réveille seulement l'écran et
  n'appuie sur rien dans la page. Il reste allumé **Stays awake for** minutes,
  puis s'éteint à nouveau.
- **Someone close by wakes it** : utilise le capteur de proximité de la tablette
  (une main près du bord supérieur) ou le capteur de luminosité (une lumière
  allumée dans la pièce). Toutes les tablettes n'en ont pas.
- Si aucun des deux n'est coché, Kiosky laisse l'écran s'éteindre vraiment et le
  rallume à l'heure *On again*. **Cinq touchers rapides** n'importe où le
  réveillent quand même, pour que vous puissiez toujours y revenir.

À la fin de la période d'extinction, Kiosky recharge la page pour qu'elle soit à
jour.

### Atténuation

**Dim from** et **Until** baissent la luminosité pendant une partie de la journée,
au pourcentage **Dimmed brightness**. **Also dim after this many minutes
untouched** atténue l'écran dès que personne ne l'a utilisé depuis un moment. Le
premier contact le rallume sans appuyer sur rien.

### Page

- **Back to the start page after this many minutes untouched** : si quelqu'un va
  sur une autre page et s'en va, Kiosky revient tout seul à votre page.
- **Reload a frozen page after this many minutes** : Kiosky vérifie toutes les
  quelques secondes que la page répond encore. Si elle cesse de répondre aussi
  longtemps, Kiosky la recharge et, si cela ne suffit pas, la relance de zéro.
  Activé par défaut (2 minutes).
- **Pages may use the camera / microphone** : voir *Caméra et micro*.
- **Shared tablet** : pour une tablette utilisée par le public, comme un accueil
  des visiteurs. Quand Kiosky revient à la page d'accueil après les minutes
  ci-dessus, il se déconnecte aussi de tout : cookies, données de site
  enregistrées, cache et historique. Il ne le fait que si quelqu'un a touché la
  tablette depuis la dernière fois. Ne le cochez pas pour un tableau de bord où
  vous vous connectez une seule fois, sinon vous serez déconnecté aussi.

### Toucher

- **Pinch to zoom** : désactivé par défaut, pour qu'un contact accidentel à deux
  doigts ne laisse pas le tableau de bord agrandi. Activez-le si les gens ont
  besoin d'agrandir la page.
- **Long press** : décochez-le pour qu'un appui long ne fasse rien : pas de
  sélection de texte avec son menu Copier/Partager/Rechercher (qui peut mener à
  d'autres applis), et pas d'aperçu des liens. Les champs de texte perdent aussi
  leur menu Coller.
- **Selecting the page's text** : décochez-le pour qu'un double toucher ou un
  appui long ne puisse pas surligner le texte de la page. Les champs de texte
  fonctionnent toujours.

### Lecteurs

- **Read NFC tags** : tant que Kiosky est au premier plan, une puce présentée à la
  tablette est lue et envoyée à la page (voir *Piloter la tablette depuis un
  tableau de bord*) et à Home Assistant. Cela empêche aussi les puces d'ouvrir
  d'autres applis. Le NFC doit être activé dans les paramètres Android.
- **Lecteurs de codes-barres** : les lecteurs USB et Bluetooth tapent comme un
  clavier très rapide. Kiosky le détecte et envoie aussi chaque lecture à la page
  et à Home Assistant ; la lecture s'inscrit toujours dans un champ de texte actif
  comme avant.

### Playlist

Indiquez d'autres pages (une adresse `https://` par ligne) à faire défiler après
votre page d'accueil, chacune affichée **Show each page for** secondes. Un contact
met le défilement en pause un moment, pour ne pas interrompre quelqu'un qui
utilise une page.

### Économiseur d'écran

Après **Show a clock after this many minutes untouched**, une grande horloge et la
date dérivent lentement sur un écran noir (pour que rien ne marque l'écran).
Donnez une adresse de page pour l'afficher à la place, comme un cadre photo. Le
premier contact le ferme sans appuyer sur rien.

### Son

**Volume** règle le volume multimédia (laissez vide pour garder celui de la
tablette). **Quiet from** et **Until** le baissent, ou le coupent à `0`, pendant
une partie de la journée. Kiosky ne change le volume que lorsque le niveau voulu
change, donc on peut encore le monter ou le baisser à la main jusqu'au
changement suivant.

### Tablette

- **Restart every day at** : un redémarrage quotidien (par exemple à `04:00`)
  élimine les fuites de mémoire lentes des tableaux de bord qui tournent longtemps.
  En propriétaire de l'appareil (Android 7+), c'est un redémarrage complet ;
  sinon Kiosky se relance lui-même.
- **Pages may upload files** : permet aux boutons « choisir un fichier » d'ouvrir
  le sélecteur de fichiers d'Android. Désactivez-le sur les tablettes publiques
  qui n'en ont pas besoin.
- **Forget saved sign-ins and trusted certificates** : voir *Connexions et
  certificats*.
- **Move the page a pixel or two every few minutes** : déplace tout lentement sur
  un tout petit cercle (2 pixels au plus), pour qu'un tableau de bord affiché toute
  la journée ne marque pas les écrans LCD et OLED bon marché. Personne ne le
  remarque.
- **While reconnecting, show the last page greyed out** : voir *Bon à savoir*.

### Home Assistant (MQTT)

Voir *Home Assistant et MQTT* plus bas.

### Code PIN des réglages

Touchez **Set a PIN…** en bas des réglages. Ensuite, balayer depuis un bord (ou
les cinq touchers dans le coin) demande le code PIN avant d'ouvrir la barre
d'outils, le bouton Retour ne ferme plus Kiosky, et les liens d'inscription
demandent aussi le code. Utilisez **PIN…** pour le changer ou le supprimer.

> Si le code PIN est oublié, la seule solution est Paramètres › Applications ›
> Kiosky › Stockage › **Effacer le stockage** (ou `adb shell pm clear uk.lp0.ha`).
> Cela efface aussi l'adresse et les connexions.

## Caméra et micro

Les sonnettes vidéo, interphones et l'audio bidirectionnel (par exemple dans Home
Assistant ou Frigate) ont besoin de la caméra ou du micro. Quand une page les
demande :

- Le site de votre propre page les obtient tout de suite. La première fois,
  Android vous demande d'autoriser Kiosky à utiliser la caméra ou le micro. En
  propriétaire de l'appareil, Kiosky se l'accorde lui-même.
- Tout autre site demande une fois (**Allow** ou **Block**), et un oui est retenu.
- Décochez **Pages may use the camera** ou **microphone** dans les réglages pour
  les refuser partout.

Seules les pages `https://` peuvent les utiliser.

### Piloter la tablette depuis un tableau de bord

Les pages peuvent utiliser `window.kiosky` en JavaScript, par exemple depuis une
automatisation de détecteur de mouvement sur un tableau de bord Home Assistant :

| Appel | Ce qu'il fait |
| --- | --- |
| `kiosky.wake()` | Réveille l'écran (toute page) |
| `kiosky.isScreenOff()` | `true` tant que l'écran est éteint |
| `kiosky.version()` | La version de Kiosky |
| `kiosky.reload()` | Recharge la page |
| `kiosky.setBrightness(30)` | Règle la luminosité en pourcentage ; `0` revient à celle de la tablette |
| `kiosky.screenOff()` | Noircit l'écran jusqu'au prochain contact ou `wake()` |
| `kiosky.deviceName()` | Le nom de la tablette dans Shepherd |
| `kiosky.speak("Bonjour")` | Lit le texte à voix haute |
| `kiosky.battery()` | Texte JSON : `{"level": 80, "charging": true, "temperature": 31.5}` |
| `kiosky.setUrl("https://…")` | Ouvre une page jusqu'à ce que Kiosky revienne à la page d'accueil |
| `kiosky.watchMotion(true)` | Envoie des événements `kiosky:motion` (voir ci-dessous) jusqu'au changement de page ; `false` sans capteur |
| `kiosky.nfcAvailable()` | `true` tant que les puces NFC sont lues |

Tout ce qui est sous `version()` ne fonctionne que pour le site de votre propre
page (sur une tablette gérée : le site de la page d'accueil et les sites
autorisés), et renvoie `false` (ou un nom vide) partout ailleurs.

Kiosky envoie aussi des événements à votre propre page, pour qu'elle puisse réagir
sans interroger sans cesse :

```
window.addEventListener('kiosky:motion', (e) => console.log(e.detail.source)); // "proximity" ou "light"
window.addEventListener('kiosky:nfc', (e) => console.log(e.detail.id, e.detail.records));
window.addEventListener('kiosky:barcode', (e) => console.log(e.detail.code));
```

Une puce NFC donne son `id` (hexadécimal), ses `techs` et ses `records` : chacun
avec `text`, `uri`, ou `mime` et `data` (base64). Les lectures réveillent aussi
l'écran.

## Home Assistant et MQTT

Kiosky peut se connecter à un broker MQTT (comme le module complémentaire
Mosquitto de Home Assistant), pour que votre serveur domotique voie et pilote la
tablette. Dans **Settings › Home Assistant (MQTT)**, saisissez l'adresse du
broker, et le nom d'utilisateur et le mot de passe s'il en faut. La ligne au-dessus
des champs indique si Kiosky est connecté.

Avec **Add it to Home Assistant automatically** coché, la tablette apparaît dans
Home Assistant comme un appareil (Paramètres › Appareils › MQTT) avec :

| Entité | Ce qu'elle fait |
| --- | --- |
| **Screen** | Interrupteur : éteint noircit l'écran jusqu'à un contact ; allumé le réveille |
| **Brightness** | 1–100 % |
| **Volume** | Volume multimédia, 0–100 % |
| **Page** | Affiche l'adresse à l'écran ; modifiez-la pour ouvrir une autre page |
| **Reload**, **Start page** | Boutons |
| **Announce** | Lit un message à voix haute (`notify.send_message`) |
| **Motion** | Activé quand quelqu'un touche la tablette ou que le capteur de proximité ou de luminosité remarque quelqu'un, désactivé 30 secondes plus tard |
| **Battery**, **Charging**, **Battery temperature**, **Wi-Fi signal**, **Page title** | Capteurs |
| **NFC tag**, **Barcode** | Événements, quand ces lecteurs sont activés : déclenchez des automatisations à partir d'une lecture |

Une page ouverte depuis Home Assistant reste jusqu'à ce que Kiosky revienne à la
page d'accueil (après les minutes réglées dans *Page*, ou avec **Start page**).

**Sans découverte automatique**, utilisez directement les topics. Le topic de base
est `kiosky/` suivi d'une partie de l'identifiant de la tablette (affiché dans les
réglages), ou votre propre **Topic** :

| Topic | Contenu |
| --- | --- |
| `<base>/availability` | `online` ou `offline` (publié par Kiosky) |
| `<base>/state` | JSON : `screen` (`on`, `dim`, `off`), `brightness`, `volume`, `url`, `title`, `battery_level`, `charging`, `battery_temperature`, `wifi_rssi`, `screensaver`, `offline`, `app_version` |
| `<base>/motion` | `ON` ou `OFF` |
| `<base>/cmd/wake` | Facultatif : minutes pendant lesquelles rester allumé |
| `<base>/cmd/screen` | `ON` ou `OFF` |
| `<base>/cmd/screen_off` | N'importe quoi |
| `<base>/cmd/brightness`, `<base>/cmd/volume` | Un pourcentage |
| `<base>/cmd/set_url` | Une adresse `https://` (ou `http://`) |
| `<base>/cmd/go_home`, `<base>/cmd/reload` | N'importe quoi |
| `<base>/cmd/announce` | Du texte, ou `{"text": "…", "volume": 80}` |
| `<base>/cmd/clear_session` | N'importe quoi : se déconnecte de tout, comme pour une tablette partagée |
| `<base>/nfc`, `<base>/barcode` | Lectures (publiées par Kiosky) : JSON avec `event_type: "scanned"` et les détails ci-dessus |

Par exemple, pour réveiller l'écran quand on sonne à la porte :

```
action: mqtt.publish
data:
  topic: kiosky/1a2b3c4d/cmd/wake
  payload: "5"
```

**Sécurité.** Quiconque peut publier sur le broker peut piloter la tablette ;
protégez donc le broker par un mot de passe. **Use TLS** chiffre la connexion
(généralement le port 8883). Pour un broker avec un certificat auto-signé,
saisissez son **empreinte de certificat** SHA-256 ; Kiosky fait alors confiance à
exactement ce certificat.

## Connexions et certificats

**Demandes de nom d'utilisateur et de mot de passe.** Certaines pages (derrière un
proxy, ou sur des appareils anciens) demandent un nom d'utilisateur et un mot de
passe dans une fenêtre du navigateur. Kiosky demande une fois et, avec **Remember
on this tablet** coché, se connecte ensuite tout seul. Les informations sont
stockées sur la tablette sans chiffrement ; utilisez donc un compte qui peut
seulement consulter le tableau de bord.

**Certificats auto-signés.** Un serveur sur votre propre réseau (Home Assistant,
un NAS) a souvent un certificat auquel Android ne fait pas confiance. Kiosky
affiche son empreinte SHA-256 et demande s'il faut lui faire confiance ; un oui est
retenu pour ce site uniquement. Si vous ne vous attendiez pas à la question pour un
site, choisissez **Don't trust**.

Pour effacer les deux, cochez **Forget saved sign-ins and trusted certificates**
dans les réglages. Sur une tablette gérée, les deux viennent du serveur
(*HTTP sign-in* et *Trusted certificates*) ; un certificat non reconnu affiche son
empreinte à l'écran et dans Shepherd, prête à copier.

## Entretien de la batterie

Les tablettes laissées en charge en permanence usent leur batterie et peuvent la
faire gonfler. Kiosky signale la température et l'état de la batterie à Shepherd,
qui peut vous alerter quand elle chauffe.

**Limit charging to protect the battery** (dans les réglages) active la limite de
charge de la tablette. Android n'offre aux applis aucun moyen général de le faire,
c'est donc expérimental et ne fonctionne que sur les tablettes Samsung dotées de
*Protéger la batterie* (la charge s'arrête vers 85 %). Il faut une commande
unique depuis un ordinateur avec adb :

`adb shell pm grant uk.lp0.ha android.permission.WRITE_SECURE_SETTINGS`

Sans elle, Kiosky ne peut pas modifier le réglage ; vous pouvez toujours activer
*Protéger la batterie* à la main dans Paramètres › Batterie. Kiosky ne désactive la
limite que si c'est lui qui l'a activée.

## Mises à jour

Kiosky cherche une nouvelle version une minute après son démarrage, puis toutes les
12 heures. Quand une version est disponible, une barre apparaît en bas de
l'écran :

1. Touchez **Install**. Kiosky télécharge la mise à jour.
2. La première fois, Android vous demande d'autoriser Kiosky à installer des
   applis. Activez **Autoriser cette source**, revenez en arrière et touchez de
   nouveau **Install**.
3. Confirmez sur l'écran d'installation d'Android. Kiosky se ferme, se met à jour
   et se rouvre normalement tout seul. À partir d'Android 10, cela demande la même
   autorisation que **Start on boot** (plus bas). S'il ne se rouvre pas, touchez
   l'icône de Kiosky.

Touchez **Later** pour masquer la barre jusqu'au prochain redémarrage de Kiosky.
L'adresse, les connexions et les réglages enregistrés sont conservés lors des mises
à jour.

Les mises à jour viennent des publications du projet sur GitHub. Android
n'installe qu'une mise à jour signée avec la même clé que la copie déjà installée ;
un fichier modifié est donc refusé.

**Si une mise à jour tourne mal.** Pendant les 15 minutes qui suivent le premier
démarrage d'une nouvelle version, Kiosky surveille les plantages. S'il plante deux
fois pendant ce temps, il démarre en *mode sans échec* : MQTT, les capteurs de
mouvement et les changements de Wi-Fi sont désactivés et il ne rouvre pas la
dernière page, pour continuer à tourner et pouvoir recevoir la mise à jour
suivante. Sur une tablette gérée, il le signale aussi au serveur. Android ne laisse
jamais une appli revenir seule à une version plus ancienne ; la solution est donc
toujours une version plus récente.

## Démarrage automatique

Kiosky peut s'ouvrir tout seul chaque fois que la tablette est allumée ou
redémarre, pour qu'un écran mural revienne après une coupure de courant sans que
personne n'y touche. C'est désactivé tant que vous ne l'activez pas :

1. Ouvrez la barre d'outils et touchez **Start on boot: Off**. Il passe à **On**.
2. À partir d'Android 10, Kiosky ouvre alors un écran de paramètres. Activez
   **Afficher par-dessus d'autres applis** pour Kiosky, puis revenez en arrière.
   Tant que ce n'est pas fait, le bouton indique **Start on boot: Needs
   permission**.
3. Redémarrez la tablette pour vérifier. Kiosky s'ouvre une fois le démarrage
   terminé, ce qui peut prendre une minute.

Kiosky s'ouvre une fois la tablette déverrouillée pour la première fois ; pour un
écran mural, réglez donc le verrouillage de l'écran sur *Aucun* ou *Balayer*.
Certaines marques (comme Xiaomi, Huawei et Oppo) ont aussi leur propre réglage
*Démarrage automatique* à activer pour Kiosky.

Touchez de nouveau le bouton pour le désactiver.

## Verrouiller la tablette

Normalement, n'importe qui peut dérouler le volet des notifications ou appuyer sur
Accueil pour quitter Kiosky. **Lockdown** l'empêche : le menu de Kiosky devient la
seule sortie.

1. Ouvrez la barre d'outils et touchez **Lockdown: Off**.
2. Sauf si Kiosky est le propriétaire de l'appareil, une liste de vérification
   s'ouvre. Touchez chaque élément **✗** pour l'activer (voir plus bas), puis
   revenez à Kiosky.
3. Pour déverrouiller, balayez depuis un bord pour ouvrir le menu de Kiosky,
   touchez **Lockdown: On**, puis **Turn off lockdown**. Si un balayage n'affiche
   rien, **touchez cinq fois rapidement le coin supérieur gauche** pour ouvrir le
   menu à la place.

### Quelle est sa solidité ?

Android ne permet qu'au *propriétaire de l'appareil* (ou à un MDM) de verrouiller
complètement la tablette. Cela fonctionne sur toutes les versions d'Android et le
bouton indique **Lockdown: On** (ou **On (MDM)**). Sinon il indique **Lockdown: On
(partial)**, et Kiosky combine tout ce qu'Android permet. Chaque élément de la
liste ferme une brèche de plus :

| Élément de la liste | Ce qu'il fait | Versions d'Android |
| --- | --- | --- |
| **Accessibility helper** | Ramène Kiosky aussitôt si une autre appli, l'écran d'accueil ou les applis récentes s'ouvrent. Ferme le volet des notifications à partir d'Android 12 | Toutes |
| **Display over other apps** | Bloque le volet des notifications (jusqu'à 7.1). Permet à Kiosky de revenir quand on l'a quitté (10 et plus) | 6.0 et plus |
| **Kiosky is the Home app** | Le bouton Accueil ramène à Kiosky | Toutes |
| **Screen pinning** | Bloque Accueil et applis récentes. Activez *Demander le code avant de retirer l'épinglage* pour qu'on ne puisse pas simplement l'annuler | Toutes |

Kiosky ferme aussi tout seul le volet des notifications dès qu'il est déroulé, ce
qui fonctionne jusqu'à Android 11 sans l'assistant.

| | Propriétaire de l'appareil ou MDM | Partiel, tous les éléments ✓ |
| --- | --- | --- |
| Volet des notifications | Désactivé | Fermé aussitôt (seul jusqu'à 11, avec l'assistant à partir de 12) |
| Accueil, applis récentes | Bloqués | Bloqués par l'épinglage, et Kiosky revient si on les atteint |
| Menu Marche/Arrêt et écran de verrouillage | Bloqués | Fonctionnent encore |
| Paramètres | Inaccessibles | Kiosky revient s'ils s'ouvrent |
| Mises à jour | Installées sans confirmation | Android demande de confirmer |

> **Android 13 et plus :** Android grise l'interrupteur d'accessibilité des applis
> qui n'ont pas été installées depuis une boutique. Ouvrez d'abord **Infos sur
> l'appli › ⋮ › Autoriser les paramètres restreints** pour Kiosky (la liste de
> vérification propose un raccourci), puis activez l'assistant.

### Faire de Kiosky le propriétaire de l'appareil

**Le plus simple, avec Kiosky Shepherd :** réinitialisez la tablette, touchez six
fois au même endroit sur le premier écran de configuration et scannez le
**Provisioning QR** du code d'inscription depuis la console Shepherd. La tablette
rejoint le Wi-Fi, installe Kiosky en propriétaire de l'appareil et s'inscrit toute
seule.

Sinon, cela se fait une fois par tablette, depuis un ordinateur avec
[adb](https://developer.android.com/tools/adb) :

1. Sur la tablette, supprimez tous les comptes dans Paramètres › Comptes (Android
   refuse tant qu'un compte est connecté). Vous pourrez les rajouter ensuite.
2. Activez le **Débogage USB** (Paramètres › À propos › touchez sept fois *Numéro
   de build*, puis Paramètres › Options pour les développeurs).
3. Branchez la tablette et lancez :
   `adb shell dpm set-device-owner uk.lp0.ha/kiosky.AdminReceiver`
4. Ouvrez Kiosky et activez **Lockdown**. Le bouton indique **Lockdown: On** au
   lieu de **Lockdown: On (pinned)**.

> Un propriétaire de l'appareil ne peut pas être désinstallé. Pour retirer Kiosky
> plus tard, désactivez Lockdown et touchez d'abord **Remove device owner**, ou
> réinitialisez la tablette.

Sur une tablette gérée par Shepherd, l'administrateur active ou désactive Lockdown
avec le réglage **Lock the tablet to Kiosky**, et la tablette ne peut pas le
modifier. Après le code PIN technicien, le bouton **Lockdown** ouvre la liste de
vérification pour activer les éléments manquants, et **Remove device owner**
apparaît tant que Lockdown est désactivé.

## Tablettes gérées (Shepherd)

Une organisation peut faire tourner un serveur **Kiosky Shepherd** pour s'occuper
de nombreuses tablettes à la fois. Une fois une tablette inscrite, le serveur
choisit sa page et ses réglages, et la tablette lui indique comment elle va. Les
commandes de la tablette elle-même sont verrouillées.

### Inscrire une tablette

Il vous faut un code d'inscription de votre administrateur. N'importe laquelle de
ces méthodes fonctionne :

- **Scannez le code QR** avec l'appli appareil photo de la tablette et ouvrez le
  lien, ou
- ouvrez le **lien d'inscription** sur la tablette, ou
- ouvrez la barre d'outils, touchez **Enroll**, et saisissez l'adresse du
  **serveur** et le **code** (les tirets et majuscules n'ont pas d'importance).
  **Scan QR** utilise une appli de lecture si elle est installée.

Kiosky vous demande de confirmer avant de confier la tablette. Une tablette déjà
gérée ignore les liens et codes d'inscription.

### Ce qui change une fois gérée

- Le serveur définit la page. S'il n'en a pas encore choisi, Kiosky affiche
  *Waiting for configuration*.
- La barre d'outils peut être complètement masquée ou n'afficher que **Reload** et
  **Help**, selon le choix de l'administrateur. **Update**, **Start on boot** et
  **Enroll** disparaissent.
- Seuls les sites autorisés par l'administrateur peuvent s'ouvrir.
- Les mises à jour, le démarrage automatique, l'orientation de l'écran, la taille du
  texte et réglages similaires viennent du serveur.
- Le serveur peut recharger la page, afficher un message ou le lire à voix haute,
  prendre une capture d'écran ou regarder l'écran en direct, collecter des
  diagnostics, effacer les connexions, réveiller l'écran, ou relancer Kiosky (ou
  redémarrer la tablette, en propriétaire de l'appareil).
- Si Kiosky plante un jour, il indique au serveur ce qui s'est passé au démarrage
  suivant.
- Le programme de nuit, l'atténuation, la playlist, l'économiseur d'écran, le son,
  le redémarrage quotidien, le retour à la page d'accueil, la vérification des
  pages figées, l'envoi de fichiers, les connexions, les certificats et l'accès à la
  caméra et au micro viennent du serveur ; **Settings** est donc masqué. La caméra
  et le micro ne fonctionnent que pour le site de la page d'accueil et les sites
  que l'administrateur autorise nommément.
- Kiosky signale la page affichée, les erreurs de chargement, la batterie, le
  signal Wi-Fi et si les autorisations de démarrage automatique et de mise à jour
  sont accordées.
- Le serveur peut enregistrer des réseaux Wi-Fi sur la tablette (en propriétaire
  de l'appareil, à partir d'Android 10), pour qu'un nouveau routeur ou un nouveau
  mot de passe ne demande pas de déplacement. Un mot de passe modifié qui ne se
  connecte pas dans les 10 minutes est remis comme avant.

Si la tablette perd le contact avec le serveur, elle garde ses derniers réglages et
reste verrouillée. Elle n'est libérée que lorsque l'administrateur la retire dans
Shepherd. Kiosky affiche alors *This tablet is no longer managed* et garde sa page
actuelle.

### Code PIN technicien

Si l'administrateur a défini un code PIN, touchez cinq fois rapidement le **coin
supérieur gauche** de l'écran, puis saisissez le code. La barre d'outils complète
s'ouvre jusqu'à ce qu'elle se masque, avec **Device info** (serveur, nom de
l'appareil, dernier contact) et des boutons pour accorder les autorisations
manquantes. Après cinq codes erronés, la saisie est bloquée pendant 5 minutes.

## Bon à savoir

- **Les connexions sont conservées.** Les cookies et le stockage des sites sont
  enregistrés ; vous restez donc connecté après un redémarrage de l'appli ou de la
  tablette (sauf si **Shared tablet** est coché dans les réglages).
- **Connexion perdue ?** Si la page est injoignable (Wi-Fi qui se réveille,
  serveur qui redémarre), Kiosky affiche la dernière bonne image de la page,
  grisée, avec un bandeau *Reconnecting…* (ou un écran *Reconnecting…* complet s'il
  n'a pas encore d'image de ce site), et réessaie tout seul toutes les 10 secondes.
  Cela vaut aussi pour les serveurs qui répondent par une erreur pendant leur
  démarrage, comme le *502 Bad Gateway* de Home Assistant. Touchez **Try now** pour
  réessayer tout de suite.
- **La page a planté ?** Si le moteur de rendu de la page plante ou qu'Android
  l'arrête pour libérer de la mémoire, Kiosky le relance tout seul au lieu de se
  fermer.
- **Les fenêtres surgissantes se ferment seules.** Un `alert()`, `confirm()` ou
  `prompt()` d'une page se ferme après 30 secondes (comme OK pour les alertes,
  Annuler pour les autres), pour qu'une fenêtre égarée ne bloque pas l'écran.
- **Les liens restent dans Kiosky.** Rien ne s'ouvre dans une autre appli ou un
  navigateur.
- **Volet des notifications :** tant que Kiosky est à l'écran, il empêche de
  dérouler le volet des notifications, même sans Lockdown. En propriétaire de
  l'appareil, le volet est complètement désactivé. Sinon Kiosky le ferme dès qu'il
  s'ouvre : tout seul jusqu'à Android 11, et avec l'assistant d'accessibilité à
  partir d'Android 12 (voir *Verrouiller la tablette*).
- **L'écran reste allumé** tant que Kiosky est ouvert. Pour l'atténuer ou
  l'éteindre la nuit, utilisez **Settings** (voir *Réglages*).
- **Le son et la vidéo** peuvent se lancer sans toucher l'écran d'abord, ce qui
  convient aux caméras et sonnettes sur un tableau de bord.
- **Bouton Retour :** masque la barre d'outils ou cette aide si l'une d'elles est
  affichée, sinon revient d'une page. Sur la première page, il ferme Kiosky, sauf
  si un code PIN des réglages est défini ou que Lockdown est activé.
- **Revenir à cette page :** ouvrez la barre d'outils et touchez **Help**. Elle
  s'ouvre par-dessus votre page et ne la remplace jamais. Pour faire de cette page
  la page d'accueil à la place, videz le champ d'adresse et touchez **Go**.

## Conseils pour une tablette murale

- **Garder les gens dans Kiosky :** activez **Lockdown** (voir *Verrouiller la
  tablette*) et définissez un **code PIN des réglages** pour que la barre d'outils
  ne serve pas à modifier quoi que ce soit.
- **Ménager l'écran la nuit :** réglez un programme de nuit dans **Settings**.
- **Empêcher Android de le fermer :** réglez l'utilisation de la batterie de Kiosky
  sur *Non restreinte* dans Paramètres › Applications › Kiosky › Batterie.
- **Laissez-la en charge.** L'écran reste allumé, elle consomme donc plus que
  d'habitude.

## En cas de problème

- **Écran vide ou « page non disponible » :** vérifiez l'adresse dans la barre
  d'outils et qu'elle commence par `https://`. Kiosky continue d'essayer ; une page
  qui revient en ligne se charge donc toute seule.
- **Déconnecté à chaque fois :** vérifiez que votre site n'est pas réglé pour
  effacer la connexion à la fermeture du navigateur.
- **La barre d'outils n'apparaît pas :** balayez lentement depuis l'extrême bord de
  l'écran.

## À propos

Kiosky {{version}} · Logiciel libre sous licence GNU General Public License v3.0.
Code source et publications : [github.com/MichelleFindlay/kiosky](https://github.com/MichelleFindlay/kiosky)

### Remerciements

Kiosky est construit sans le SDK Android, avec ces projets open source :

- [Apktool](https://github.com/iBotPeaches/Apktool) de Connor Tumbleson et ses
  contributeurs (Apache License 2.0) empaquette le manifeste et les ressources de
  l'appli dans un APK.
- [dex2jar](https://github.com/pxb1988/dex2jar) de Bob Pan et ses contributeurs
  (Apache License 2.0) convertit le code Java au format dex d'Android.
- [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer) de Patrick
  Favre-Bulle (Apache License 2.0) aligne et signe l'APK, avec `zipalign` et
  `apksig` du projet Android Open Source.
- [OpenJDK](https://openjdk.org) (GPL v2 avec la Classpath Exception) compile le
  code Java.

Les pages sont affichées par Android System WebView, construit à partir du projet
[Chromium](https://www.chromium.org) (licence BSD).
