# Deployment

TrustVault runs as one web service on Render, with its database on Aiven (hosted MySQL 8). The single service serves both the website and the API, so there is one address and no cross-site calls.

Live address: https://trustvault-cuaz.onrender.com

## Architecture

- **Render (web service, Docker):** the root `Dockerfile` builds the website with Node, copies its output into the backend's static folder, then builds the Spring Boot backend. The backend serves the website's pages and the `/api` routes.
- **Aiven (MySQL 8):** holds the `trustvault` database. The service is in the Asia Pacific region.
- **Brevo:** sends share notifications through its HTTPS API. Render blocks outbound SMTP ports, so SMTP cannot be used on the hosted service.

## 1. Database (Aiven)

1. Create a MySQL service on Aiven. Note the host, port, user and password.
2. Connect with the MySQL client, using TLS:
mysql -h <host> -P <port> -u avnadmin -p --ssl-mode=REQUIRED defaultdb

3. Create the schema from `database/schema.sql`, then add the columns added after the first version:
   - `documents.sha256 CHAR(64) NULL`
   - `grants.expires_at`, `grants.download_limit`, `grants.downloads_used`
   - `users.display_name`, `users.phone`
   - `password_resets` table, including `delivered_at`
4. Run `database/setup-accounts.sql` to create the application account, which can insert and read `audit_log` only.
5. Check with `SHOW TABLES;` that all five tables exist: `audit_log`, `documents`, `grants`, `password_resets`, `users`.

## 2. Accounts

Accounts are created through sign-up, which always makes an OWNER. Administrator and viewer roles are set with an `UPDATE` on `users.role`. Choose a new password for each account. Do not reuse local demo passwords on the live site.

## 3. Web service (Render)

1. On Render, click **New → Web Service**, and select the `trustvault` repository.
2. Set **Language** to **Docker**.
3. Leave **Root Directory** empty.
4. Under **Advanced**, set **Dockerfile Path** to `./Dockerfile`.
5. Choose the free instance type.
6. Add the environment variables below, then click **Save, rebuild, and deploy**.

### Environment variables

Set these as values in Render. Never put them in a file in the repository.

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | JDBC address of the Aiven database, with `sslMode=REQUIRED` |
| `DB_USER` | Application database user |
| `DB_PASSWORD` | Application database password |
| `STORAGE_ROOT` | Directory for uploaded files, for example `/var/trustvault/files` |
| `JWT_SECRET` | Token signing key, at least 48 random bytes, Base64 encoded |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD` | Brevo SMTP settings, kept for local testing |
| `MAIL_FROM` | Sender address, which must be verified in Brevo under **Senders** |
| `MAIL_API_KEY` | Brevo API key used for share notifications on the hosted service |

Generate `JWT_SECRET` in PowerShell:

```powershell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

## 4. Verify the deployment

1. Open the live address. The welcome page should load.
2. Open `/signup` directly. It should load, not show "Not Found".
3. Open `/api/documents` without signing in. It should return 401, which confirms the API is protected.
4. Sign in with an owner account and upload a file.
5. Share it with a second account. The recipient should receive the notice email.
6. Sign in as the administrator and open **Activity**. The upload, share and notification should be listed.

Check the audit trail for the share notification:

```sql
SELECT action, outcome, detail, created_at FROM audit_log
WHERE action IN ('NOTIFY_SHARE', 'GRANT') ORDER BY id DESC LIMIT 5;
```

`NOTIFY_SHARE` should show `ALLOWED`.

## 5. Redeploying

Push to the `main` branch, and Render deploys automatically. To deploy the latest commit manually, use **Manual Deploy → Deploy latest commit**.

## Known limits of the hosted service

- **Uploaded files are not durable.** They live on the server's disk, which the free tier clears on restart. The database records remain, so a file can be listed but missing. Re-upload and re-share before a demonstration.
- **Cold starts.** The free service sleeps when idle, and the first request can take up to a minute.
- **Development-only setting removed.** The local certificate workaround for mail is not in the deployed code.

## Before submission

1. Change the administrator password, since it was typed into a chat during development.
2. Give assessors the demo passwords directly, not through the repository.
3. Delete the old `trustvault-backend` and `trustvault-frontend` services on Render, so only one deployment remains.
4. Record the hosted-storage limit in `THREATS.md`, if it is not already there.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| "Whitelabel Error Page, status=401" at the root | Security rules blocking public pages. Fixed by the rule that permits everything outside `/api/**`. |
| "Not Found" on a page address such as `/signup` | The page route is not listed in `SpaController`. Add it there. |
| "Server cannot be reached" in the browser | The backend is asleep or the network blocks the site. Wait, or try another network. |
| Share works but `NOTIFY_SHARE` shows DENIED | Check the Render logs for `Email could not be sent`. A connect error on port 587 means SMTP is blocked, so use `MAIL_API_KEY`. |
| `Failed to connect to host, port 587` in logs | Outbound SMTP is blocked on Render. Use the Brevo API. |
| Sign-in returns 401 for a known account | Wrong password, or the account is inactive. Passwords cannot be recovered, so reset them through the reset flow or create a new account. |