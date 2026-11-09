![Logotipo de Kiosky](logo.png)

# Te damos la bienvenida a Kiosky

Kiosky muestra una sola página web a pantalla completa y la mantiene ahí: sin
pestañas, sin barra de direcciones, y la pantalla sigue encendida. Está pensado
para tabletas de pared y paneles de control, como Home Assistant o Grafana.

Ves esta página porque todavía no se ha configurado ninguna dirección.

> Los botones y ajustes de Kiosky están en inglés. Esta ayuda los nombra tal como
> aparecen en pantalla, por ejemplo **Settings**.

## Configura tu página

1. **Desliza hacia abajo desde el borde superior** de la pantalla, donde apunta la
   flecha. Deslizar hacia arriba desde el borde inferior también funciona. Las
   barras del sistema y la barra de herramientas de Kiosky aparecen arriba.
2. Toca el cuadro de dirección y escribe la dirección de tu página, por ejemplo
   `https://www.example.com`.
3. Toca **Go** (o pulsa Intro).

Kiosky recuerda esa dirección y la abre cada vez que se inicia la app. Si omites
`https://`, Kiosky lo añade por ti.

> Solo funcionan las direcciones `https://`. Las páginas con `http://` simple se
> bloquean y no se cargan.

## La barra de herramientas

Desliza desde el borde superior o inferior en cualquier momento para recuperar la
barra de herramientas. Empieza el gesto justo en el borde del cristal: un gesto que
empieza más hacia dentro va a la página web.

| Botón | Qué hace |
| --- | --- |
| **Go** | Abre la dirección del cuadro y la guarda como tu página |
| **Reload** | Recarga la página actual |
| **Help** | Muestra esta página encima de la tuya. **Close** (o Atrás) vuelve a tu página, que sigue cargada debajo |
| **Settings** | Horario nocturno, atenuación, volver a la página de inicio, cámara y micrófono, y el PIN de ajustes (consulta *Ajustes*) |
| **Update** | Busca una versión más reciente de Kiosky |
| **Start on boot** | Activa o desactiva que Kiosky se abra solo al encender |
| **Enroll** | Entrega esta tableta a un servidor de gestión Shepherd (consulta más abajo) |
| **Lockdown** | Bloquea la tableta en Kiosky, para que los menús y botones del sistema no permitan salir |
| **Remove device owner** | Deshace la configuración como propietario del dispositivo (solo se muestra con Lockdown desactivado) |
| **✕** | Oculta la barra de herramientas |

La barra de herramientas se oculta sola tras 15 segundos, salvo que estés
escribiendo en ella.

## Ajustes

Toca **Settings** en la barra de herramientas. Todo es opcional; deja una hora
vacía para desactivar esa parte.

### Pantalla apagada por la noche

Configura **Off at** y **On again at** (formato de 24 horas `HH:MM`, por ejemplo
`23:00` y `06:30`). Las horas pueden pasar de la medianoche. Entre ellas, la
pantalla se pone en negro.

- **A touch wakes it**: el primer toque solo despierta la pantalla y no pulsa nada
  en la página. Se queda encendida **Stays awake for** minutos y luego se apaga de
  nuevo.
- **Someone close by wakes it**: usa el sensor de proximidad de la tableta (una
  mano cerca del borde superior) o el sensor de luz (una luz de la habitación
  encendida). No todas las tabletas lo tienen.
- Si ninguno está marcado, Kiosky deja que la pantalla se apague de verdad y la
  vuelve a encender a la hora *On again*. **Cinco toques rápidos** en cualquier
  parte la despiertan igualmente, para que siempre puedas volver a entrar.

Cuando termina el horario de apagado, Kiosky recarga la página para que esté al
día.

### Atenuación

**Dim from** y **Until** bajan el brillo durante parte del día, hasta el
porcentaje **Dimmed brightness**. **Also dim after this many minutes untouched**
atenúa la pantalla cuando nadie la ha usado durante un rato. El primer toque la
ilumina de nuevo sin pulsar nada.

### Página

- **Back to the start page after this many minutes untouched**: si alguien entra
  en otra página y se va, Kiosky vuelve solo a tu página.
- **Reload a frozen page after this many minutes**: Kiosky comprueba cada pocos
  segundos que la página sigue respondiendo. Si deja de hacerlo durante ese
  tiempo, Kiosky la recarga y, si eso no ayuda, la inicia de nuevo desde cero.
  Activado por defecto (2 minutos).
- **Pages may use the camera / microphone**: consulta *Cámara y micrófono*.
- **Shared tablet**: para una tableta que usa el público, como un registro de
  visitantes. Cuando Kiosky vuelve a la página de inicio tras los minutos de
  arriba, también cierra todas las sesiones: cookies, datos de sitios guardados,
  la caché y el historial. Solo lo hace si alguien tocó la tableta desde la última
  vez. No lo marques para un panel en el que inicias sesión una sola vez, o también
  cerrará tu sesión.

### Toque

- **Pinch to zoom**: desactivado por defecto, para que un toque accidental con dos
  dedos no deje el panel ampliado. Actívalo si la gente necesita ampliar la
  página.
- **Long press**: desmárcalo para que mantener pulsado no haga nada: ni selección
  de texto con su menú Copiar/Compartir/Buscar (que puede llevar a otras apps), ni
  vistas previas de enlaces. Los cuadros de texto también pierden su menú Pegar.
- **Selecting the page's text**: desmárcalo para que un doble toque o una
  pulsación larga no puedan resaltar el texto de la página. Los cuadros de texto
  siguen funcionando.

### Lectores

- **Read NFC tags**: mientras Kiosky está en primer plano, una etiqueta acercada a
  la tableta se lee y se envía a la página (consulta *Controlar la tableta desde un
  panel*) y a Home Assistant. Además impide que las etiquetas abran otras apps. El
  NFC tiene que estar activado en los ajustes de Android.
- **Lectores de códigos de barras**: los lectores USB y Bluetooth escriben como un
  teclado muy rápido. Kiosky lo detecta y envía también cada lectura a la página y
  a Home Assistant; la lectura sigue escribiéndose en un cuadro de texto activo
  como antes.

### Playlist

Indica más páginas (una dirección `https://` por línea) para ir rotando después de
tu página de inicio, cada una durante **Show each page for** segundos. Un toque
pausa la rotación un rato, para no interrumpir a quien esté usando una página.

### Salvapantallas

Después de **Show a clock after this many minutes untouched**, un reloj grande y
la fecha se desplazan lentamente por una pantalla negra (para que nada se queme).
Indica la dirección de una página para mostrarla en su lugar, como un marco de
fotos. El primer toque lo cierra sin pulsar nada.

### Sonido

**Volume** fija el volumen multimedia (déjalo vacío para usar el de la tableta).
**Quiet from** y **Until** lo bajan, o lo silencian con `0`, durante parte del
día. Kiosky solo cambia el volumen cuando cambia el nivel deseado, así que se
puede seguir subiendo o bajando a mano hasta el siguiente cambio.

### Tableta

- **Restart every day at**: un reinicio diario (por ejemplo a las `04:00`) elimina
  las fugas de memoria lentas de los paneles que funcionan mucho tiempo. Como
  propietario del dispositivo (Android 7+) es un reinicio completo; si no, Kiosky
  se reinicia a sí mismo.
- **Pages may upload files**: permite que los botones de "elegir archivo" abran el
  selector de archivos de Android. Desactívalo en tabletas públicas que no lo
  necesiten.
- **Forget saved sign-ins and trusted certificates**: consulta *Inicios de sesión y
  certificados*.
- **Move the page a pixel or two every few minutes**: desplaza todo lentamente en
  un círculo diminuto (2 píxeles como mucho), para que un panel mostrado todo el
  día no se queme en pantallas LCD y OLED baratas. Nadie lo nota.
- **While reconnecting, show the last page greyed out**: consulta *Conviene
  saber*.

### Home Assistant (MQTT)

Consulta *Home Assistant y MQTT* más abajo.

### PIN de ajustes

Toca **Set a PIN…** al final de los ajustes. A partir de entonces, deslizar desde
un borde (o los cinco toques en la esquina) pide el PIN antes de abrir la barra de
herramientas, el botón Atrás ya no cierra Kiosky y los enlaces de inscripción
también piden el PIN. Usa **PIN…** para cambiarlo o quitarlo.

> Si se olvida el PIN, la única salida es Ajustes › Aplicaciones › Kiosky ›
> Almacenamiento › **Borrar almacenamiento** (o `adb shell pm clear uk.lp0.ha`).
> Eso también borra la dirección y los inicios de sesión.

## Cámara y micrófono

Los timbres con vídeo, los interfonos y el audio bidireccional (por ejemplo en Home
Assistant o Frigate) necesitan la cámara o el micrófono. Cuando una página los
pide:

- El sitio de tu propia página los obtiene enseguida. La primera vez, Android te
  pide que permitas a Kiosky usar la cámara o el micrófono. Como propietario del
  dispositivo, Kiosky se lo concede a sí mismo.
- Cualquier otro sitio pregunta una vez (**Allow** o **Block**), y un sí se
  recuerda.
- Desmarca **Pages may use the camera** o **microphone** en los ajustes para
  rechazarlos en todas partes.

Solo las páginas `https://` pueden usarlos.

### Controlar la tableta desde un panel

Las páginas pueden usar `window.kiosky` en JavaScript, por ejemplo desde una
automatización de un sensor de movimiento en un panel de Home Assistant:

| Llamada | Qué hace |
| --- | --- |
| `kiosky.wake()` | Despierta la pantalla (cualquier página) |
| `kiosky.isScreenOff()` | `true` mientras la pantalla está apagada |
| `kiosky.version()` | La versión de Kiosky |
| `kiosky.reload()` | Recarga la página |
| `kiosky.setBrightness(30)` | Fija el brillo en porcentaje; `0` vuelve al de la tableta |
| `kiosky.screenOff()` | Pone la pantalla en negro hasta el siguiente toque o `wake()` |
| `kiosky.deviceName()` | El nombre de la tableta en Shepherd |
| `kiosky.speak("Hola")` | Lee el texto en voz alta |
| `kiosky.battery()` | Texto JSON: `{"level": 80, "charging": true, "temperature": 31.5}` |
| `kiosky.setUrl("https://…")` | Abre una página hasta que Kiosky vuelva a la página de inicio |
| `kiosky.watchMotion(true)` | Envía eventos `kiosky:motion` (abajo) hasta que cambie la página; `false` sin sensor |
| `kiosky.nfcAvailable()` | `true` mientras se leen etiquetas NFC |

Todo lo que está debajo de `version()` solo funciona para el sitio de tu propia
página (en una tableta gestionada: el sitio de la página de inicio y los sitios
permitidos), y devuelve `false` (o un nombre vacío) en cualquier otro lugar.

Kiosky también envía eventos a tu propia página, para que pueda reaccionar sin
preguntar una y otra vez:

```
window.addEventListener('kiosky:motion', (e) => console.log(e.detail.source)); // "proximity" o "light"
window.addEventListener('kiosky:nfc', (e) => console.log(e.detail.id, e.detail.records));
window.addEventListener('kiosky:barcode', (e) => console.log(e.detail.code));
```

Una etiqueta NFC da su `id` (hexadecimal), sus `techs` y sus `records`: cada uno con
`text`, `uri`, o `mime` y `data` (base64). Las lecturas también despiertan la
pantalla.

## Home Assistant y MQTT

Kiosky puede conectarse a un broker MQTT (como el complemento Mosquitto de Home
Assistant), para que tu servidor doméstico vea y controle la tableta. En
**Settings › Home Assistant (MQTT)**, escribe la dirección del broker, y el usuario
y la contraseña si los necesita. La línea sobre los campos indica si Kiosky está
conectado.

Con **Add it to Home Assistant automatically** marcado, la tableta aparece en Home
Assistant como un dispositivo (Ajustes › Dispositivos › MQTT) con:

| Entidad | Qué hace |
| --- | --- |
| **Screen** | Interruptor: apagado pone la pantalla en negro hasta un toque; encendido la despierta |
| **Brightness** | 1–100 % |
| **Volume** | Volumen multimedia, 0–100 % |
| **Page** | Muestra la dirección en pantalla; cámbiala para abrir otra página |
| **Reload**, **Start page** | Botones |
| **Announce** | Lee un mensaje en voz alta (`notify.send_message`) |
| **Motion** | Activado cuando alguien toca la tableta o el sensor de proximidad o de luz detecta a alguien, desactivado 30 segundos después |
| **Battery**, **Charging**, **Battery temperature**, **Wi-Fi signal**, **Page title** | Sensores |
| **NFC tag**, **Barcode** | Eventos, cuando esos lectores están activados: lanza automatizaciones con una lectura |

Una página abierta desde Home Assistant permanece hasta que Kiosky vuelve a la
página de inicio (tras los minutos fijados en *Página*, o con **Start page**).

**Sin descubrimiento**, usa los topics directamente. El topic base es `kiosky/`
seguido de parte del identificador de la tableta (se muestra en los ajustes), o tu
propio **Topic**:

| Topic | Contenido |
| --- | --- |
| `<base>/availability` | `online` u `offline` (lo publica Kiosky) |
| `<base>/state` | JSON: `screen` (`on`, `dim`, `off`), `brightness`, `volume`, `url`, `title`, `battery_level`, `charging`, `battery_temperature`, `wifi_rssi`, `screensaver`, `offline`, `app_version` |
| `<base>/motion` | `ON` u `OFF` |
| `<base>/cmd/wake` | Opcional: minutos que debe seguir encendida |
| `<base>/cmd/screen` | `ON` u `OFF` |
| `<base>/cmd/screen_off` | Cualquier cosa |
| `<base>/cmd/brightness`, `<base>/cmd/volume` | Un porcentaje |
| `<base>/cmd/set_url` | Una dirección `https://` (o `http://`) |
| `<base>/cmd/go_home`, `<base>/cmd/reload` | Cualquier cosa |
| `<base>/cmd/announce` | Texto, o `{"text": "…", "volume": 80}` |
| `<base>/cmd/clear_session` | Cualquier cosa: cierra todas las sesiones, como en una tableta compartida |
| `<base>/nfc`, `<base>/barcode` | Lecturas (las publica Kiosky): JSON con `event_type: "scanned"` y los detalles de arriba |

Por ejemplo, para despertar la pantalla cuando suena el timbre:

```
action: mqtt.publish
data:
  topic: kiosky/1a2b3c4d/cmd/wake
  payload: "5"
```

**Seguridad.** Cualquiera que pueda publicar en el broker puede controlar la
tableta, así que pon una contraseña al broker. **Use TLS** cifra la conexión
(normalmente en el puerto 8883). Para un broker con un certificado autofirmado,
escribe su **huella del certificado** SHA-256; Kiosky confía entonces exactamente
en ese certificado.

## Inicios de sesión y certificados

**Peticiones de usuario y contraseña.** Algunas páginas (detrás de un proxy, o en
dispositivos antiguos) piden usuario y contraseña en un cuadro del navegador.
Kiosky pregunta una vez y, con **Remember on this tablet** marcado, inicia sesión
solo a partir de entonces. Los datos se guardan en la tableta sin cifrar, así que
usa una cuenta que solo pueda ver el panel.

**Certificados autofirmados.** Un servidor de tu propia red (Home Assistant, un
NAS) suele tener un certificado en el que Android no confía. Kiosky muestra su
huella SHA-256 y pregunta si confiar en él; un sí se recuerda solo para ese sitio.
Si no esperabas la pregunta para un sitio, elige **Don't trust**.

Para borrar ambos, marca **Forget saved sign-ins and trusted certificates** en los
ajustes. En una tableta gestionada, ambos vienen del servidor (*HTTP sign-in* y
*Trusted certificates*); un certificado no fiable muestra su huella en pantalla y
en Shepherd, lista para copiar.

## Cuidado de la batería

Las tabletas que pasan todo el tiempo cargando desgastan la batería y pueden
hacer que se hinche. Kiosky informa a Shepherd de la temperatura y el estado de la
batería, y Shepherd puede avisarte cuando se calienta.

**Limit charging to protect the battery** (en los ajustes) activa el límite de
carga propio de la tableta. Android no ofrece a las apps una forma general de
hacerlo, así que es experimental y solo funciona en tabletas Samsung con
*Proteger batería* (la carga se detiene en torno al 85 %). Necesita una orden
única desde un ordenador con adb:

`adb shell pm grant uk.lp0.ha android.permission.WRITE_SECURE_SETTINGS`

Sin ella, Kiosky no puede cambiar el ajuste; aun así puedes activar *Proteger
batería* a mano en Ajustes › Batería. Kiosky solo vuelve a desactivar el límite si
fue él quien lo activó.

## Actualizaciones

Kiosky busca una versión nueva un minuto después de iniciarse y luego cada 12
horas. Cuando hay una disponible, aparece una barra en la parte inferior de la
pantalla:

1. Toca **Install**. Kiosky descarga la actualización.
2. La primera vez, Android te pide que permitas a Kiosky instalar apps. Activa
   **Permitir de esta fuente**, vuelve atrás y toca **Install** otra vez.
3. Confirma en la pantalla de instalación de Android. Kiosky se cierra, se
   actualiza y normalmente se vuelve a abrir solo. Desde Android 10 esto necesita
   el mismo permiso que **Start on boot** (abajo). Si no se vuelve a abrir, toca el
   icono de Kiosky.

Toca **Later** para ocultar la barra hasta el siguiente reinicio de Kiosky. La
dirección guardada, los inicios de sesión y los ajustes se conservan en las
actualizaciones.

Las actualizaciones vienen de las publicaciones del proyecto en GitHub. Android
solo instala una actualización firmada con la misma clave que la copia que ya
tienes, así que un archivo manipulado se rechaza.

**Si una actualización sale mal.** Durante los 15 minutos siguientes al primer
inicio de una versión nueva, Kiosky vigila los cierres inesperados. Si falla dos
veces en ese tiempo, se inicia en *modo seguro*: MQTT, los sensores de movimiento y
los cambios de Wi-Fi se desactivan y no vuelve a abrir la última página, para
seguir funcionando y poder recibir la siguiente actualización. En una tableta
gestionada, también se lo comunica al servidor. Android nunca deja que una app
vuelva sola a una versión anterior, así que la solución siempre es una más nueva.

## Inicio automático

Kiosky puede abrirse solo cada vez que la tableta se enciende o se reinicia, para
que un panel de pared vuelva tras un corte de luz sin que nadie lo toque. Está
desactivado hasta que lo actives:

1. Abre la barra de herramientas y toca **Start on boot: Off**. Cambia a **On**.
2. Desde Android 10, Kiosky abre entonces una pantalla de ajustes. Activa
   **Mostrar sobre otras apps** para Kiosky y vuelve atrás. Hasta que lo hagas, el
   botón dice **Start on boot: Needs permission**.
3. Reinicia la tableta para comprobarlo. Kiosky se abre cuando termina el
   arranque, lo que puede tardar un minuto.

Kiosky se abre después de desbloquear la tableta por primera vez, así que para un
panel de pared pon el bloqueo de pantalla en *Ninguno* o *Deslizar*. Algunas
marcas (como Xiaomi, Huawei y Oppo) tienen además su propio ajuste de *Inicio
automático* que hay que activar para Kiosky.

Toca el botón de nuevo para desactivarlo.

## Bloquear la tableta

Normalmente, cualquiera puede bajar el panel de notificaciones o pulsar Inicio
para salir de Kiosky. **Lockdown** lo impide: el menú de Kiosky pasa a ser la
única salida.

1. Abre la barra de herramientas y toca **Lockdown: Off**.
2. Salvo que Kiosky sea el propietario del dispositivo, se abre una lista de
   comprobación. Toca cada elemento **✗** para activarlo (consulta abajo) y luego
   vuelve a Kiosky.
3. Para desbloquear, desliza desde un borde para abrir el menú de Kiosky, toca
   **Lockdown: On** y luego **Turn off lockdown**. Si un deslizamiento no muestra
   nada, **toca cinco veces rápido la esquina superior izquierda** para abrir el
   menú.

### ¿Hasta qué punto es seguro?

Android solo permite al *propietario del dispositivo* (o a un MDM) bloquear la
tableta por completo. Eso funciona en todas las versiones de Android y el botón
dice **Lockdown: On** (u **On (MDM)**). Si no, dice **Lockdown: On (partial)**, y
Kiosky combina todo lo que Android permite. Cada elemento de la lista cierra otro
hueco:

| Elemento de la lista | Qué hace | Versiones de Android |
| --- | --- | --- |
| **Accessibility helper** | Trae Kiosky de vuelta enseguida si se abre otra app, la pantalla de inicio o Recientes. Cierra el panel de notificaciones desde Android 12 | Todas |
| **Display over other apps** | Bloquea el panel de notificaciones (hasta 7.1). Permite que Kiosky vuelva cuando se ha salido de él (10 y posteriores) | 6.0 y posteriores |
| **Kiosky is the Home app** | El botón Inicio vuelve a Kiosky | Todas |
| **Screen pinning** | Bloquea Inicio y Recientes. Activa *Pedir PIN para desfijar* para que no se pueda deshacer sin más | Todas |

Kiosky también cierra solo el panel de notificaciones en cuanto se baja, lo que
funciona hasta Android 11 sin el asistente.

| | Propietario del dispositivo o MDM | Parcial, todos los elementos ✓ |
| --- | --- | --- |
| Panel de notificaciones | Desactivado | Se cierra enseguida (solo hasta 11, con el asistente desde 12) |
| Inicio, Recientes | Bloqueados | Bloqueados por la fijación, y Kiosky vuelve si se llega a ellos |
| Menú de apagado y pantalla de bloqueo | Bloqueados | Siguen funcionando |
| Ajustes | Inaccesibles | Kiosky vuelve si se abren |
| Actualizaciones | Se instalan sin preguntar | Android pide confirmación |

> **Android 13 y posteriores:** Android atenúa el interruptor de accesibilidad de
> las apps que no se instalaron desde una tienda. Abre primero **Información de la
> app › ⋮ › Permitir ajustes restringidos** para Kiosky (la lista tiene un acceso
> directo) y luego activa el asistente.

### Hacer de Kiosky el propietario del dispositivo

**Lo más fácil, con Kiosky Shepherd:** restablece la tableta de fábrica, toca seis
veces en el mismo punto de la primera pantalla de configuración y escanea el
**Provisioning QR** del código de inscripción desde la consola de Shepherd. La
tableta se conecta al Wi-Fi, instala Kiosky como propietario del dispositivo y se
inscribe sola.

Si no, se hace una vez por tableta, desde un ordenador con
[adb](https://developer.android.com/tools/adb):

1. En la tableta, elimina todas las cuentas en Ajustes › Cuentas (Android se niega
   mientras haya alguna cuenta iniciada). Puedes volver a añadirlas después.
2. Activa la **Depuración por USB** (Ajustes › Información › toca siete veces
   *Número de compilación*, y luego Ajustes › Opciones para desarrolladores).
3. Conecta la tableta y ejecuta:
   `adb shell dpm set-device-owner uk.lp0.ha/kiosky.AdminReceiver`
4. Abre Kiosky y activa **Lockdown**. El botón dice **Lockdown: On** en lugar de
   **Lockdown: On (pinned)**.

> Un propietario del dispositivo no se puede desinstalar. Para quitar Kiosky más
> adelante, desactiva Lockdown y toca primero **Remove device owner**, o restablece
> la tableta de fábrica.

En una tableta gestionada por Shepherd, el administrador activa o desactiva
Lockdown con el ajuste **Lock the tablet to Kiosky**, y la tableta no puede
cambiarlo. Tras el PIN de técnico, el botón **Lockdown** abre la lista de
comprobación para activar los elementos que falten, y **Remove device owner**
aparece mientras Lockdown está desactivado.

## Tabletas gestionadas (Shepherd)

Una organización puede tener un servidor **Kiosky Shepherd** para cuidar de muchas
tabletas a la vez. Una vez inscrita una tableta, el servidor elige su página y sus
ajustes, y la tableta le informa de cómo está. Los controles propios de la tableta
quedan bloqueados.

### Inscribir una tableta

Necesitas un código de inscripción de tu administrador. Cualquiera de estas formas
funciona:

- **Escanea el código QR** con la app de cámara de la tableta y abre el enlace, o
- abre el **enlace de inscripción** en la tableta, o
- abre la barra de herramientas, toca **Enroll** y escribe la dirección del
  **servidor** y el **código** (los guiones y las mayúsculas no importan).
  **Scan QR** usa una app de escaneo si hay una instalada.

Kiosky te pide confirmación antes de entregar la tableta. Una tableta ya
gestionada ignora los enlaces y códigos de inscripción.

### Qué cambia cuando está gestionada

- El servidor fija la página. Si aún no ha elegido una, Kiosky muestra *Waiting for
  configuration*.
- La barra de herramientas puede estar oculta del todo o mostrar solo **Reload** y
  **Help**, según lo que haya elegido el administrador. **Update**, **Start on
  boot** y **Enroll** desaparecen.
- Solo se pueden abrir los sitios que el administrador permite.
- Las actualizaciones, el inicio automático, la orientación de la pantalla, el
  tamaño del texto y ajustes similares vienen del servidor.
- El servidor puede recargar la página, mostrar un mensaje o leerlo en voz alta,
  hacer una captura de pantalla o ver la pantalla en directo, recopilar
  diagnósticos, borrar los inicios de sesión, despertar la pantalla o reiniciar
  Kiosky (o reiniciar la tableta, como propietario del dispositivo).
- Si Kiosky falla alguna vez, le dice al servidor qué salió mal en el siguiente
  inicio.
- El horario nocturno, la atenuación, la playlist, el salvapantallas, el sonido, el
  reinicio diario, volver a la página de inicio, la comprobación de páginas
  congeladas, la subida de archivos, los inicios de sesión, los certificados y el
  acceso a la cámara y el micrófono vienen del servidor, así que **Settings** está
  oculto. La cámara y el micrófono solo funcionan para el sitio de la página de
  inicio y los sitios que el administrador permite por nombre.
- Kiosky informa de la página que muestra, los errores de carga, la batería, la
  señal Wi-Fi y si están concedidos los permisos de inicio automático y de
  actualización.
- El servidor puede guardar redes Wi-Fi en la tableta (como propietario del
  dispositivo, desde Android 10), para que un router o una contraseña nuevos no
  requieran una visita. Una contraseña cambiada que no conecta en 10 minutos se
  restablece como estaba.

Si la tableta pierde el contacto con el servidor, conserva sus últimos ajustes y
sigue bloqueada. Solo se libera cuando el administrador la quita en Shepherd.
Kiosky muestra entonces *This tablet is no longer managed* y mantiene su página
actual.

### PIN de técnico

Si el administrador ha fijado un PIN, toca cinco veces rápido la **esquina
superior izquierda** de la pantalla y escribe el PIN. La barra de herramientas
completa se abre hasta que vuelve a ocultarse, con **Device info** (servidor,
nombre del dispositivo, último contacto) y botones para conceder los permisos que
falten. Tras cinco PIN incorrectos, la introducción se bloquea durante 5 minutos.

## Conviene saber

- **Los inicios de sesión se conservan.** Las cookies y el almacenamiento de los
  sitios se guardan, así que sigues con la sesión iniciada tras reiniciar la app o
  la tableta (salvo que **Shared tablet** esté marcado en los ajustes).
- **¿Sin conexión?** Si no se puede llegar a la página (el Wi-Fi está
  despertando, el servidor se está reiniciando), Kiosky muestra la última imagen
  buena de la página, en gris, con un aviso *Reconnecting…* (o una pantalla
  *Reconnecting…* completa si aún no tiene imagen de ese sitio), y lo vuelve a
  intentar solo cada 10 segundos. Eso incluye los servidores que responden con un
  error mientras arrancan, como el *502 Bad Gateway* de Home Assistant. Toca **Try
  now** para reintentar al momento.
- **¿Se ha bloqueado la página?** Si el motor de renderizado de la página falla o
  Android lo detiene para liberar memoria, Kiosky lo vuelve a iniciar solo en lugar
  de cerrarse.
- **Las ventanas emergentes se cierran solas.** Un `alert()`, `confirm()` o
  `prompt()` de una página se cierra a los 30 segundos (como Aceptar en los avisos,
  Cancelar en los demás), para que una ventana perdida no deje el panel atascado.
- **Los enlaces se quedan en Kiosky.** Nada se abre en otra app o navegador.
- **Panel de notificaciones:** siempre que Kiosky está en pantalla, impide bajar el
  panel de notificaciones, incluso sin Lockdown. Como propietario del dispositivo,
  el panel se desactiva del todo. Si no, Kiosky lo cierra en cuanto se abre: solo
  hasta Android 11, y con el asistente de accesibilidad desde Android 12 (consulta
  *Bloquear la tableta*).
- **La pantalla sigue encendida** mientras Kiosky está abierto. Para atenuarla o
  apagarla por la noche, usa **Settings** (consulta *Ajustes*).
- **El sonido y el vídeo** pueden reproducirse sin tocar antes, lo que va bien con
  cámaras y timbres en un panel.
- **Botón Atrás:** oculta la barra de herramientas o esta ayuda si alguna está a la
  vista; si no, vuelve una página atrás. En la primera página cierra Kiosky, salvo
  que haya un PIN de ajustes o Lockdown esté activado.
- **Volver a esta página:** abre la barra de herramientas y toca **Help**. Se abre
  encima de tu página y nunca la sustituye. Para que esta página sea la de inicio,
  vacía el cuadro de dirección y toca **Go**.

## Consejos para una tableta de pared

- **Mantener a la gente en Kiosky:** activa **Lockdown** (consulta *Bloquear la
  tableta*) y pon un **PIN de ajustes** para que la barra de herramientas no sirva
  para cambiar nada.
- **Cuidar la pantalla por la noche:** fija un horario nocturno en **Settings**.
- **Evitar que Android lo cierre:** pon el uso de batería de Kiosky en *Sin
  restricciones* en Ajustes › Aplicaciones › Kiosky › Batería.
- **Déjala cargando.** La pantalla sigue encendida, así que gasta más de lo
  habitual.

## Si algo va mal

- **Pantalla en blanco o "página no disponible":** comprueba la dirección en la
  barra de herramientas y que empiece por `https://`. Kiosky sigue intentándolo,
  así que una página que vuelve a estar en línea se carga sola.
- **Se cierra la sesión cada vez:** asegúrate de que tu sitio no está configurado
  para borrar la sesión al cerrar el navegador.
- **La barra de herramientas no aparece:** desliza despacio desde el mismo borde
  de la pantalla.

## Acerca de

Kiosky {{version}} · Software libre bajo la GNU General Public License v3.0.
Código fuente y publicaciones: [github.com/MichelleFindlay/kiosky](https://github.com/MichelleFindlay/kiosky)

### Créditos

Kiosky se construye sin el SDK de Android, con estos proyectos de código abierto:

- [Apktool](https://github.com/iBotPeaches/Apktool) de Connor Tumbleson y
  colaboradores (Apache License 2.0) empaqueta el manifiesto y los recursos de la
  app en un APK.
- [dex2jar](https://github.com/pxb1988/dex2jar) de Bob Pan y colaboradores (Apache
  License 2.0) convierte el código Java al formato dex de Android.
- [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer) de Patrick
  Favre-Bulle (Apache License 2.0) alinea y firma el APK, con `zipalign` y
  `apksig` del Android Open Source Project.
- [OpenJDK](https://openjdk.org) (GPL v2 con la Classpath Exception) compila el
  código Java.

Las páginas se muestran con Android System WebView, que se construye a partir del
proyecto [Chromium](https://www.chromium.org) (licencia BSD).
