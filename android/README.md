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
- **Funziona con schermo spento/app chiusa**: il servizio acquisisce un
  `PowerManager.PARTIAL_WAKE_LOCK` (rinnovato a ogni heartbeat, ~25s) per
  evitare che la CPU sospenda mentre lo schermo è spento — senza, sia la
  registrazione che la connessione WebSocket per ricevere i comandi si
  fermerebbero in standby. `onTaskRemoved` non viene usato per fermare il
  servizio: chiudere/rimuovere l'app dalle app recenti non deve interrompere
  la videocamera, dato che il servizio è in foreground e indipendente
  dall'Activity. Resta un limite reale: sui produttori con gestori
  batteria molto aggressivi (es. MIUI, alcuni Samsung/Huawei) l'esenzione
  standard Android dal risparmio energetico (bottone in `CameraHomeScreen`)
  potrebbe non bastare — servirebbero impostazioni specifiche del
  produttore (es. "autostart"/"blocca in recenti") che non sono
  automatizzabili con API Android standard.
- **Nessun wake via FCM**: se comunque il sistema termina il processo (es.
  pressione di memoria estrema), il servizio si riavvia da solo
  (`START_STICKY`) ma non c'è un modo per il server di forzare il
  risveglio da remoto se il riavvio automatico non scatta. Il wake-up FCM
  è predisposto lato backend come stub (`backend/src/services/fcm.service.ts`)
  ma richiede un progetto Firebase reale per essere completato.
- **Nessuna anteprima live**: `CameraHomeScreen` non mostra un preview
  della fotocamera per evitare di dover coordinare l'uso della fotocamera
  tra l'Activity e il foreground service (due `LifecycleOwner` diversi
  che si contenderebbero lo stesso `ProcessCameraProvider`). Miglioria
  possibile in futuro, con gestione esplicita di quale componente "possiede"
  la fotocamera in un dato momento.

## Stato registrazione: come viene mostrato

Lo stato "sta registrando adesso?" non è solo un evento WebSocket
effimero: il backend tiene traccia dell'ultimo stato noto per ogni
dispositivo (`ws/registry.ts`, azzerato alla disconnessione) ed è incluso
come campo `recording` in `GET /api/devices`. Sia `DeviceListScreen` sia
`DeviceDetailScreen` interrogano questo stato all'apertura/aggiornamento
(non solo in ascolto di eventi push), quindi mostrano uno stato corretto
anche se non era rimasto nulla in ascolto quando la registrazione è
iniziata.
