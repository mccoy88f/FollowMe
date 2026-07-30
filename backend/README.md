# FollowMe backend

Node.js + Fastify + PostgreSQL + Socket.IO. Fase 1 del progetto FollowMe:
auth, pairing dei dispositivi, comandi realtime, upload/storage locale
delle registrazioni.

## Sviluppo locale

```bash
cp .env.example .env      # imposta POSTGRES_HOST=localhost
npm install
npm run migrate           # richiede un Postgres raggiungibile
npm run dev                # avvia con reload automatico su :3000
```

## Modello

- **users**: account che controllano da remoto uno o piu' dispositivi.
- **devices**: telefoni "camera". Creati da un utente (stato `pending`,
  con un pairing token temporaneo), poi "accoppiati" dall'app installata
  sul telefono camera, che scambia il pairing token con un device token
  di lunga durata.
- **recordings**: una riga per ogni registrazione, con path del file su
  disco locale (`STORAGE_PATH`), tipo (`audio`/`video`/`audio_video`) e
  stato (`recording`/`completed`/`failed`).

## Flusso di autenticazione (app di controllo)

1. `POST /api/auth/register` `{email, password}` -> `{accessToken, refreshToken}`
2. `POST /api/auth/login` `{email, password}` -> `{accessToken, refreshToken}`
3. Le richieste autenticate passano `Authorization: Bearer <accessToken>`
4. Quando l'access token scade: `POST /api/auth/refresh` `{refreshToken}` -> `{accessToken}`
5. `POST /api/auth/logout-all` `{refreshToken}` invalida tutti i refresh token emessi (bump di `refresh_token_version`)

## Flusso di pairing (app camera)

1. Dall'app di controllo, l'utente crea un nuovo dispositivo:
   `POST /api/devices` `{name}` (auth utente) -> `{device, pairingToken, pairingTokenExpiresAt}`
   (il pairing token scade dopo 15 minuti)
2. L'utente inserisce il pairing token nell'app installata sul telefono camera
3. L'app camera chiama `POST /api/devices/pair` `{pairingToken}` (nessuna auth)
   -> `{deviceToken, deviceId, name}`
4. L'app camera usa `deviceToken` come `Authorization: Bearer` per tutte le
   chiamate successive (endpoint `/api/device/...`) e per la connessione WebSocket

## Comandi realtime (WebSocket, Socket.IO)

Connessione: `io(URL, { auth: { role: 'device'|'user', token } })`

- L'app camera si connette con `role: 'device'` e il proprio `deviceToken`.
  Riceve l'evento `command` (`{action: 'start'|'stop', type?}`), ed emette
  `status` (es. `{state: 'recording_started', recordingId}`) e periodicamente
  `heartbeat`.
- L'app di controllo si connette con `role: 'user'` e il proprio `accessToken`.
  Riceve `device_status` (`{deviceId, status: 'online'|'offline'}`) e
  `device_recording_status` (rilancio degli eventi `status` del device).

Se il dispositivo non e' connesso via WebSocket, `POST /api/devices/:id/command`
risponde `202 {delivered:false, reason:'device_offline'}`. Il wake-up via FCM
push per riattivare l'app in background e' predisposto come stub
(`src/services/fcm.service.ts`) ma non ancora collegato a un progetto
Firebase reale: da completare in fase 3 insieme all'app camera.

## API principali (riassunto)

Autenticate come utente (`Authorization: Bearer <accessToken>`):

- `GET /api/devices` — lista dispositivi con stato online/offline
- `POST /api/devices` `{name}` — crea dispositivo + pairing token
- `POST /api/devices/:id/regenerate-pairing-token` — nuovo pairing token
- `DELETE /api/devices/:id`
- `POST /api/devices/:id/command` `{action, type?}` — avvia/ferma registrazione
- `GET /api/recordings?deviceId=&limit=&offset=` — lista registrazioni
- `GET /api/recordings/:id`
- `GET /api/recordings/:id/download` — download manuale del file
- `DELETE /api/recordings/:id`

Autenticate come dispositivo (`Authorization: Bearer <deviceToken>`):

- `POST /api/device/recordings` `{type}` -> `{recordingId}`
- `POST /api/device/recordings/:id/chunk` — body `application/octet-stream`,
  bytes grezzi appesi in ordine al file della registrazione
- `POST /api/device/recordings/:id/complete` `{durationSeconds?}`
- `POST /api/device/recordings/:id/fail`

Nessuna auth: `POST /api/devices/pair`, `GET /health`

## Note per fase 3 (app camera)

- I chunk vengono semplicemente appesi in ordine al file su disco: il
  container/formato del media (fMP4, segmenti indipendenti, ecc.) deve
  essere scelto lato Android in modo che l'append di byte grezzi produca
  comunque un file riproducibile via streaming (es. registrare a segmenti
  e chiamare `chunk` una volta per segmento, oppure usare un muxer che
  supporti scrittura incrementale sicura).
- Il token dispositivo non scade per tempo ma e' revocabile: incrementando
  `device_token_version` (endpoint da aggiungere se serve un "logout" del
  dispositivo) tutti i token gia' emessi diventano invalidi.

## Scalabilita' (nota)

La presenza WebSocket (`src/ws/registry.ts`) e' tenuta in memoria di
processo: funziona per una singola istanza del backend. Per scalare
orizzontalmente servirebbe l'adapter Redis di Socket.IO e spostare la
registry su Redis.
