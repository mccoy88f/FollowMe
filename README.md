# FollowMe

App Android + backend per usare un telefono come videocamera di sicurezza
remota: avvio/stop di registrazione audio/video comandato da un altro
dispositivo, con upload delle registrazioni su server e possibilita' di
rivederle.

## Struttura del progetto

- `backend/` — API Node.js/Fastify + PostgreSQL + WebSocket (fase 1, vedi
  `backend/README.md` per dettagli su endpoint e sviluppo locale)
- `docker-compose.yml` — deploy su `fm.tabloza.live` tramite Coolify (vedi
  sotto)
- `android/` — app Android Kotlin/Compose, ruolo di controllo remoto
  (fase 2, vedi `android/README.md`); il ruolo videocamera è previsto
  per la fase 3, nello stesso progetto

## Avvio rapido (sviluppo locale, senza Docker)

```bash
cd backend
cp .env.example .env   # imposta POSTGRES_HOST=localhost e le altre variabili
npm install
npm run migrate
npm run dev
```

## Deploy in produzione (Coolify)

Il `docker-compose.yml` e' pensato per essere deployato come risorsa
"Docker Compose" in Coolify, che fornisce gia' il proprio reverse proxy
(Traefik) con generazione/rinnovo automatico del certificato Let's
Encrypt: **non serve un nginx/certbot separato**, anzi entrerebbe in
conflitto con il proxy di Coolify sulle porte 80/443.

1. Copia `backend/.env.example` in `backend/.env` e imposta i secret
   (password Postgres, `JWT_*_SECRET` generati con `openssl rand -hex 64`)
2. In Coolify, crea una nuova risorsa "Docker Compose" puntando a questo
   repository/branch
3. Imposta il dominio `fm.tabloza.live` per il servizio `backend` dalla UI
   di Coolify (o direttamente nella variabile `SERVICE_FQDN_BACKEND_3000`
   nel `docker-compose.yml`): Coolify/Traefik si occupano da soli di
   ottenere e rinnovare il certificato TLS al deploy
4. Deploy. Le migrazioni Postgres partono automaticamente all'avvio del
   container backend (vedi `backend/docker-entrypoint.sh`)

Le registrazioni sono salvate nel volume Docker `recordings` (montato in
`/data/recordings` nel container backend), il database nel volume
`pgdata`.
