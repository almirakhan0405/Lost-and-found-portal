# Smart Lost & Found Portal

A Java project (Spring Boot + Swing) where people report lost and found items, search them with a
typo-tolerant search, and get **automatic match suggestions** between lost and found reports.

One command starts everything:

* a **Swing desktop app** (login, report, search, claims, notifications, admin panel)
* a **Spring Boot backend** with an embedded H2 database (data is kept in the `data/` folder)
* a **full web app + REST API** at <http://localhost:8080> - people can register, report items, claim items and get notifications from any browser or phone

## Requirements

* JDK 17 or newer (`java -version`)
* Maven 3.8+ (`mvn -version`) - or open the folder in IntelliJ / Eclipse / VS Code and run `LostFoundApplication`
* Internet on the first build (Maven downloads Spring Boot)

## Run

```bash
./run.sh          # Linux / macOS
run.bat           # Windows
```

or manually:

```bash
mvn clean package -DskipTests
java -jar target/smart-lost-found-portal-1.0.0.jar
```

or straight from Maven: `mvn spring-boot:run`

The login window opens, and <http://localhost:8080> opens the web version of the portal (same accounts, same database).
Run the tests with `mvn test`.

### Demo accounts (created on first start)

| Username   | Password   | Role  |
|------------|------------|-------|
| `admin`    | `admin123` | Admin |
| `student1` | `pass123`  | User  |
| `student2` | `pass123`  | User  |

Demo data already contains a lost/found pair that the portal matches automatically
(the black Dell laptop bag). To start fresh, stop the app and delete the `data/` folder.

## Features

| Area | What it does |
|------|--------------|
| Accounts | Register / log in; passwords stored as salted PBKDF2 hashes; USER and ADMIN roles |
| Report | Report a LOST or FOUND item (name, category, location, date, description) |
| Verification | Finders can add a private question only the real owner can answer |
| Search | Typo-tolerant search ("dell lapto bag" still works) + filters by type, category, status |
| **Smart matching** | Every new report is scored against open reports of the opposite type (0-100%) |
| Notifications | Both people are notified when a likely match is found, or when a claim is made / reviewed |
| Claims | Claim a found item ("it's mine") or answer a lost item ("I found it"); the reporter approves or rejects |
| Status flow | `OPEN` -> `CLAIMED` -> `RETURNED` |
| Admin | Statistics, all users, delete any item, review any claim |
| Web app | Browse (public), sign in, report, claim, approve/reject, notifications - from any browser or phone |

## How the smart matching works

`MatchingService` gives each lost/found pair a score out of 100 and reports pairs at 45% or more:

| Signal | Points | How |
|--------|--------|-----|
| Category | 25 | same category |
| Title | 35 | fuzzy word similarity (Dice coefficient) |
| Description | 15 | shared words in title + description |
| Location | 10 | shared words ("library", "gate", ...) |
| Date | 15 | full points within 2 days, fading to 0 at 21 days |

Words count as equal when they are identical or a small typo apart (Levenshtein edit distance:
1 edit for words of 4+ letters, 2 edits for 8+ letters). Common words ("the", "lost", "found"...) are ignored.
The code is in `service/TextUtil.java` and `service/MatchingService.java` - no external libraries.

## Project structure

```
src/main/java/com/lostfound
  LostFoundApplication.java    starts Spring Boot, then opens the Swing login window
  DataSeeder.java              demo users and items on first start
  model/                       JPA entities: User, Item, Claim, Notification + enums
  repo/                        Spring Data repositories
  service/                     business logic: Auth, Item, Claim, Notification, Matching, TextUtil
  web/                         REST controllers (public + signed-in), session helper, error handler, DTOs
  ui/                          Swing screens (Login, Main frame, one panel per tab)
src/main/resources
  application.properties       port, database, settings
  static/index.html            the web app (plain HTML/JS, no build step)
src/test/java/com/lostfound    unit tests (matching, text utils) and a claim-workflow test
```

Layers: **Swing UI / REST controller -> services -> repositories -> H2 database**.
The UI never touches the database directly; it only calls services.

## REST API

| Endpoint | Returns |
|----------|---------|
| `GET /api/items?q=&type=&category=&status=` | search / filter items |
| `GET /api/items/{id}` | one item |
| `GET /api/items/{id}/matches` | smart matches for an item, with scores and reasons |
| `GET /api/stats` | counts of lost / found / open / claimed / returned |

Signed-in actions (session cookie): `POST /api/auth/login`, `/api/auth/register`, `/api/auth/logout`, `GET /api/auth/me`,
`POST /api/items` (report), `GET /api/my/items`, `POST /api/items/{id}/claim`, `GET /api/claims/incoming`, `GET /api/claims/mine`,
`POST /api/claims/{id}/approve|reject`, `GET /api/notifications`.

Example: `curl "http://localhost:8080/api/items?q=laptop&type=LOST"`

Database viewer (off by default): start with environment variable `H2_CONSOLE=true`, then open `/h2-console` -
JDBC URL `jdbc:h2:file:./data/lostfound`, user `sa`, empty password. Never enable it on a public server.

## Putting it online (so anyone can open it)

The web app is what the public uses; the Swing window is the local/admin desktop client (it shares the same database).

**Quick demo from your laptop** - run the app, then in a second terminal: `ngrok http 8080`
(or `cloudflared tunnel --url http://localhost:8080`). Share the https link it prints.
Same Wi-Fi only: open `http://<your-laptop-IP>:8080` on the phone.

**Permanent hosting (Render / Railway / any Docker host)** - the included `Dockerfile` builds and runs the app.
1. Push this folder to a GitHub repository.
2. Create a new *Web Service* from the repo and choose the Docker runtime.
3. Deploy. The platform sets `PORT` automatically; the app reads it.

Notes: the default H2 file database lives on the server's disk, which on free plans is wiped on each redeploy
(the demo data is re-created automatically). For lasting data, switch to a hosted MySQL/PostgreSQL by setting
`DB_URL` (plus the matching driver in `pom.xml`) or use a plan with a persistent disk.
Change the demo passwords (`admin123`, `pass123`) before sharing a public link.

## Ideas to extend it

* Photo upload for items
* Email notifications (Spring Mail)
* Switch H2 to MySQL (change the datasource lines in `application.properties`, add the MySQL driver)
* Spring Security (CSRF protection, rate-limited logins) for production use
# Lost-and-found-portal
