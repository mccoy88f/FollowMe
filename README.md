# FollowMe

App Android + backend per usare un telefono come videocamera di sicurezza
remota: avvio/stop di registrazione audio/video comandato da un altro
dispositivo, con upload delle registrazioni su server e possibilita' di
rivederle.

## Struttura del progetto

- `backend/` — API Node.js/Fastify + PostgreSQL + WebSocket (fase 1, vedi
  `backend/README.md` per dettagli su endpoint e sviluppo locale)
- `docker-compose.yml`, `nginx/` — deploy su `fm.tabloza.live` (vedi
  `nginx/README.md` per la procedura completa, incluso certificato TLS)
- App Android (fase 2/3, non ancora presente in questa repo)

## Avvio rapido (sviluppo locale, senza Docker)

```bash
cd backend
cp .env.example .env   # imposta POSTGRES_HOST=localhost e le altre variabili
npm install
npm run migrate
npm run dev
```

## Deploy in produzione

Vedi `nginx/README.md`.
