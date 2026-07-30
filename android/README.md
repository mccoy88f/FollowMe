# FollowMe Android app

App Kotlin + Jetpack Compose, un solo progetto per entrambi i ruoli
scelti al primo avvio (schermata di selezione ruolo):

- **Controllo remoto** (fase 2): login/registrazione, gestione
  dispositivi (pairing), avvio/stop registrazione, lista e riproduzione
  delle registrazioni.
- **Videocamera remota** (fase 3): associazione tramite codice, servizio
  in foreground che riceve comandi start/stop dal server, registra a
  segmenti con CameraX (video/audio+video) o MediaRecorder (solo audio),
  e carica ogni segmento appena finito.

## Apertura del progetto

Apri la cartella `android/` in Android Studio (Iguana o successivo
consigliato). Il progetto usa Kotlin 2.0.21 / AGP 8.5.2 / Gradle 8.7 con
il compose compiler plugin di Kotlin 2.0 (nessuna configurazione
`composeOptions` manuale necessaria).

**Nota**: in questo ambiente di sviluppo (sandbox Claude Code) non è
stato possibile scaricare l'Android SDK né risolvere le dipendenze
Gradle, perché passano da `dl.google.com`, bloccato dalla policy di rete
del sandbox — il codice non è stato quindi compilato/testato qui. Va
aperto e verificato in un ambiente con accesso normale a internet
(Android Studio in locale, o CI).

## Struttura

- `data/remote` — Retrofit API (`AuthApi`, `DeviceApi`, `RecordingApi`,
  `DeviceRecordingApi` per il ruolo videocamera), DTO, interceptor per
  token (utente o dispositivo, in base al path `api/device/*`) e URL
  server dinamico, refresh automatico del token utente
- `data/local` — DataStore per token/URL server/ruolo scelto/sessione
  dispositivo, cache in memoria per gli interceptor (che non possono
  usare le API sospese di DataStore)
- `data/socket` — `RealtimeClient` (ruolo utente/controllo) e
  `DeviceSocketClient` (ruolo videocamera) per gli eventi realtime
- `data/repository` — `AuthRepository`, `DeviceRepository`,
  `RecordingRepository` (ruolo controllo); `CameraSessionRepository`,
  `DeviceRecordingRepository` (ruolo videocamera)
- `camera/` — `RecordingEngine` (interfaccia), `AudioRecordingEngine`
  (MediaRecorder, solo audio), `VideoRecordingEngine` (CameraX Recorder,
  video/audio+video), `CameraForegroundService`, notifiche
- `ui/` — schermate Compose per entrambi i ruoli
- `navigation/FollowMeNavHost.kt` — grafo di navigazione, sceglie la
  destinazione iniziale in base al ruolo salvato
- `AppContainer.kt` — dependency injection manuale (niente Hilt)

## Configurazione server

Il campo "Indirizzo server" (login o associazione videocamera) permette
di puntare l'app a qualunque backend; il valore di default è
`https://fm.tabloza.live` (vedi `BuildConfig.DEFAULT_SERVER_URL`). In
debug build, il traffico HTTP in chiaro è permesso solo verso
`10.0.2.2`/`localhost` (emulatore) tramite
`network_security_config_debug.xml` - in release solo HTTPS.

## Ruolo videocamera: scelte e limiti noti

- **Registrazione a segmenti**: video e audio+video vengono registrati a
  segmenti di durata fissa (30s, `CameraForegroundService.SEGMENT_DURATION_MS`),
  ognuno caricato come registrazione indipendente non appena finito
  (near-real-time). L'audio-only invece usa MediaRecorder con AAC ADTS,
  formato a frame che si presterebbe anche a un flusso continuo, ma per
  semplicità/uniformità segue la stessa logica a segmenti.
- **Notifica sempre visibile**: per scelta di prodotto (vedi le note
  etiche discusse in fase 1) e per requisito tecnico Android (foreground
  service camera/microfono), la notifica di stato non è mai nascosta.
- **Nessun wake via FCM**: se il sistema termina il servizio in
  background (Doze/battery optimization aggressivo di alcuni OEM), il
  dispositivo non si riattiva automaticamente - `CameraHomeScreen` offre
  un bottone per richiedere l'esenzione dal risparmio energetico, che
  copre la maggior parte dei casi pratici su un telefono dedicato. Il
  wake-up FCM è predisposto lato backend come stub
  (`backend/src/services/fcm.service.ts`) ma richiede un progetto
  Firebase reale per essere completato.
- **Nessuna anteprima live**: `CameraHomeScreen` non mostra un preview
  della fotocamera per evitare di dover coordinare l'uso della fotocamera
  tra l'Activity e il foreground service (due `LifecycleOwner` diversi
  che si contenderebbero lo stesso `ProcessCameraProvider`). Miglioria
  possibile in futuro, con gestione esplicita di quale componente "possiede"
  la fotocamera in un dato momento.
