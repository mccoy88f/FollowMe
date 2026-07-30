# Deploy su fm.tabloza.live

Presuppone che il DNS di `fm.tabloza.live` punti gia' all'IP del server e
che le porte 80/443 siano raggiungibili da internet.

## 1. Configurare l'ambiente

```bash
cp backend/.env.example backend/.env
# modifica backend/.env: password Postgres, JWT secrets (openssl rand -hex 64), ecc.
```

## 2. Avviare con solo HTTP (per ottenere il certificato)

Il file `nginx/conf.d/followme.conf` gia' incluso serve l'API in chiaro su
HTTP e la location per la ACME challenge di certbot.

```bash
docker compose up -d postgres backend nginx
```

Verifica che risponda: `curl http://fm.tabloza.live/health`

## 3. Ottenere il certificato Let's Encrypt

```bash
docker compose run --rm certbot certonly --webroot -w /var/www/certbot \
  -d fm.tabloza.live --email TUAEMAIL@example.com --agree-tos --no-eff-email
```

## 4. Abilitare HTTPS

Modifica `nginx/conf.d/followme.conf`:
- decommenta il blocco `server { listen 443 ssl; ... }` in fondo al file
- nel blocco `listen 80`, sostituisci la location `/` con un redirect:
  `location / { return 301 https://$host$request_uri; }`

Poi ricarica nginx:

```bash
docker compose exec nginx nginx -s reload
```

## 5. Avviare anche il servizio di rinnovo automatico del certificato

```bash
docker compose up -d certbot
```

Rinnova automaticamente ogni ~12h (no-op se non ancora vicino a scadenza).

## Note

- Le registrazioni sono salvate nel volume Docker `recordings`, montato in
  `/data/recordings` nel container backend.
- Il database Postgres vive nel volume `pgdata`.
- Per il backup, `docker compose exec postgres pg_dump -U followme followme > backup.sql`
  e copia periodicamente il volume `recordings`.
