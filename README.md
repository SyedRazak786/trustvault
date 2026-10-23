# TrustVault

Secure document sharing with a real audit trail. A document is reachable only by a user who was explicitly granted it, and every upload, download, change, deletion, permission change and refused attempt is recorded in a hash-chained audit log.

## Live demo

**Website and API:** https://trustvault-cuaz.onrender.com

The free hosting sleeps when idle, so the first visit can take up to a minute.

### Demo accounts

| Role | Email | Password |
|---|---|---|
| Owner | 23jr1a05i5@gmail.com | Provided separately |
| Viewer | syedrazak773@gmail.com | Provided separately |
| Administrator | admin@trustvault.test | Provided separately |

To try sharing: sign in as the owner, open a document, and share it with the viewer's address as Read. The viewer receives an email notice, and sees the document under **Shared with me**. The administrator can review the activity log and the audit chain, but cannot open document contents.

Passwords are not published in this repository. They are given to assessors separately.

## Stack
- Backend: Java 17, Spring Boot 3.3, Spring Security (JWT), JDBC, MySQL 8
- Frontend: React and TypeScript, built with Vite and served by the backend
- Email: Brevo HTTPS API
- Tests: JUnit 5, Spring MockMvc

## How it is deployed
The root `Dockerfile` builds the website and the backend into one image. The backend serves the website's pages and the API from the same address, so there is one link. The database is hosted on Aiven (MySQL 8).

## Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8
- Node.js 18+ (frontend only)

## Database setup
1. Create the database and run `database/schema.sql` as the admin account.
2. Run `database/setup-accounts.sql` to create the application account. It has INSERT and SELECT on `audit_log` only, with no UPDATE or DELETE.
3. Optionally run `database/seed.sql` to load demo accounts and documents.

## Environment variables
| Variable | Purpose |
|---|---|
| `DB_USER` | Application database user |
| `DB_PASSWORD` | Application database password |
| `SPRING_DATASOURCE_URL` | Database address, when it is not on localhost |
| `STORAGE_ROOT` | Directory for uploaded files, outside the repository |
| `JWT_SECRET` | Signing key for tokens, at least 32 random bytes |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD` | SMTP settings, used for local testing |
| `MAIL_FROM` | Sender address, which must be verified in Brevo |
| `MAIL_API_KEY` | Brevo API key used to send share notifications |

No secret is stored in this repository. All of them are read from the environment.

## Limits of the hosted demo
- Uploaded files are stored on the server's disk. The free hosting clears that disk when the service restarts, so files uploaded earlier can disappear while their records remain. Upload the files you need shortly before a demonstration.
- The free service sleeps when idle, so the first request can be slow.

## Run the tests