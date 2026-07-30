# FollowMe Android app (fase 2: modalità Controllo)

App Kotlin + Jetpack Compose. In questa fase implementa solo il ruolo di
**controllo remoto**: login/registrazione, gestione dispositivi (pairing),
avvio/stop registrazione, lista e riproduzione delle registrazioni. Il
ruolo "videocamera" (foreground service con CameraX/AudioRecord) è
previsto per la fase 3, nello stesso progetto.

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

- `data/remote` — Retrofit API (`AuthApi`, `DeviceApi`, `RecordingApi`),
  DTO, interceptor per token e URL server dinamico, refresh automatico
- `data/local` — DataStore per token/URL server, cache in memoria per gli
  interceptor (che non possono usare le API sospese di DataStore)
- `data/socket` — client Socket.IO per gli eventi realtime (presenza
  dispositivi, stato registrazione)
- `data/repository` — `AuthRepository`, `DeviceRepository`,
  `RecordingRepository`
- `ui/` — schermate Compose (login, registrazione, lista dispositivi,
  pairing, dettaglio dispositivo, lista registrazioni, player)
- `navigation/FollowMeNavHost.kt` — grafo di navigazione
- `AppContainer.kt` — dependency injection manuale (niente Hilt)

## Configurazione server

Il campo "Indirizzo server" nella schermata di login permette di puntare
l'app a qualunque backend (utile per test locali); il valore di default è
`https://fm.tabloza.live` (vedi `BuildConfig.DEFAULT_SERVER_URL`). In
debug build, il traffico HTTP in chiaro è permesso solo verso
`10.0.2.2`/`localhost` (emulatore) tramite
`network_security_config_debug.xml` - in release solo HTTPS.
