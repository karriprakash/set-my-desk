# SetMyDesk

**setmydesk.com** — a free, open-source engine with two jobs:

1. **Ideas → shares.** Anyone, especially women restarting or building a career, can send an idea by
   **WhatsApp, phone call, voice note, the app or the website**. The engine checks whether the idea already
   exists. If it does, the idea *enhances* that project; if not, it *founds* a new one. Either way, the
   contributor gets a fair, transparent share. No money is charged or paid — ever.
2. **AR desk.** Tell it your desk size and what's on it. It computes an exact, ergonomic layout and places
   it on your real desk in augmented reality.

Web and native (Android + iOS) apps both run on the same engine.

```
set-my-desk/
├── engine/   Java 21 + Spring Boot — the engine, REST API, WhatsApp/phone webhooks, and the website (WebXR AR)
└── mobile/   Expo (React Native) — native Android/iOS app with ARCore/ARKit via ViroReact; also builds for web
```

## The engine: 12 cylinders

Every idea, from any channel, fires through the same 12 cylinders in order. Each does exactly one job,
and every stage is traced, so each decision can be explained.

| # | Cylinder | Job |
|---|----------|-----|
| 1 | intake | Normalise text; refuse empty submissions kindly |
| 2 | transcription | Voice note → text (any OpenAI-compatible speech-to-text), or park it for a volunteer |
| 3 | redaction | Strip phone numbers and emails, because ideas are public |
| 4 | quality gate | Too short? Ask for who / what problem / what solution |
| 5 | duplicate | Same person, same words → earns once |
| 6 | novelty | Compare with every project (TF-IDF cosine + vocabulary containment; works in Telugu, Hindi, …) |
| 7 | merit | Score detail, problem, solution, audience, structure |
| 8 | routing | Overlap ≥ 30% → enhancement of that project; otherwise a new project |
| 9 | title | Name a new project from its first sentence |
| 10 | shares | `points = 1000 × merit × novelty` |
| 11 | persist | Write idea, project and an **append-only** ledger row |
| 12 | reply | Tell the contributor what happened and what she earned |

### Fairness rules (published live at `GET /api/rules`)

- **Merit** = `0.2 + 0.8 × (0.30·detail + 0.25·problem + 0.20·solution + 0.15·audience + 0.10·structure)`.
  It scores the idea, never the person.
- **Novelty** = `1.0` for a new project, `1 − overlap` for an enhancement, so adding something genuinely new earns more.
- **No time input.** The same idea earns the same points on day one or year five: *not more early, not less later*.
- **Share %** = your points ÷ all points in the project. Points never shrink; percentages rebalance as the project grows.

## The AR desk solver

`POST /api/desk/layout` with desk size, handedness and items → exact positions (cm from desk centre).
Deterministic: same input, same layout. Rules include monitor centred at the back (two monitors centred as a group),
keyboard 10 cm from the front edge, mouse on your dominant side, lamp on the other side, and no overlaps or
overhangs. You can **pin** any item to an exact spot and the rest arranges around it. It reports an
ergonomic score and warnings (for example, the monitor being too close to your eyes).

- **Website:** 3D preview everywhere; **View in AR** uses WebXR hit-testing (Chrome on Android).
- **App:** ARCore (Android) / ARKit (iOS) plane detection; tap your desk to drop the layout on it.

## Deploy (Render, free)

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/karriprakash/set-my-desk)

`render.yaml` creates the web service (Docker, built from `engine/`) and a free Postgres database, wires
them together, and generates the webhook token. The website is served by the same service.

1. Click the button, sign in to Render with GitHub, give it access to this repo, and press **Deploy Blueprint**.
2. Wait for the first build (about 5–8 minutes). The app is then live at `https://setmydesk.onrender.com`
   (Render shows the exact URL; if the name is taken it adds a suffix).
3. **Custom domain.** `setmydesk.com` and `www` serve the landing page from Vercel; the engine app is
   `app.setmydesk.com` (already in the blueprint). At your domain registrar add a `CNAME` record named `app`
   pointing to `setmydesk.onrender.com` (use the exact value Render shows under *Settings → Custom Domains*).
   Render issues the HTTPS certificate automatically once DNS resolves.
4. Optional: in *Environment*, fill in the speech-to-text and Twilio variables, then point Twilio's WhatsApp and
   voice webhooks at `https://app.setmydesk.com/channels/...?token=<SETMYDESK_WEBHOOK_TOKEN>` (copy the token from the dashboard).

Free-tier limits: the service sleeps after 15 minutes without traffic (the next visit takes about 30 s to wake),
the free database expires after 30 days unless upgraded, and voice-note files live on temporary disk. Upgrade
the web service and database (or attach a disk) before real users rely on it.

## Run it

### Engine + website

```bash
cd engine
mvn spring-boot:run          # http://localhost:8080
mvn test                     # 22 tests: pipeline, fairness, webhooks, desk geometry
```

Production uses Postgres. Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
or use the `engine/Dockerfile`.

| Variable | Purpose |
|----------|---------|
| `SETMYDESK_WEBHOOK_TOKEN` | Shared secret for WhatsApp/phone webhooks (**change it**) |
| `SETMYDESK_TRANSCRIPTION_URL` / `_API_KEY` / `_MODEL` | Optional speech-to-text (OpenAI-compatible `/audio/transcriptions`). Without it, voice notes wait for volunteers |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_FROM`, `TWILIO_SMS_FROM` | Follow-up messages after voice notes and calls |
| `SETMYDESK_CORS_ORIGINS` | Extra origins allowed to call the API |

### WhatsApp and phone calls (Twilio)

- WhatsApp "when a message comes in" → `POST https://<host>/channels/whatsapp?token=<SETMYDESK_WEBHOOK_TOKEN>`
- Voice "a call comes in" → `POST https://<host>/channels/phone/incoming?token=<SETMYDESK_WEBHOOK_TOKEN>`

Callers speak after the beep; the recording runs through the engine and they get an SMS with their shares.

### Volunteer transcription desk

Voice notes that couldn't be transcribed automatically wait in a queue:

- `GET /api/ideas/pending-transcription` (header `X-Volunteer-Token`)
- `GET /api/ideas/{id}/audio` to listen
- `POST /api/ideas/{id}/transcript` with `{"text": "..."}`; the idea then fires through the cylinders and earns shares

### Mobile app (Android, iOS, web)

```bash
cd mobile
npm install
EXPO_PUBLIC_API_URL=http://<your-computer-ip>:8080 npx expo run:android   # or run:ios
npm run typecheck && npm test
```

AR uses native code (ViroReact), so use a **development build** (`expo run:*` or EAS Build); Expo Go can't run it.

## API

| Method | Path | |
|--------|------|-|
| POST | `/api/ideas` | `{name, phone?, text, channel?}` typed idea |
| POST | `/api/ideas/voice` | multipart `audio` (+ `name`, `phone`, `text`) |
| GET | `/api/ideas/{id}` | idea status |
| GET | `/api/projects` | all projects, by points |
| GET | `/api/projects/{id}` | cap table + ideas |
| GET | `/api/rules` | the full rulebook and firing order |
| GET | `/api/desk/catalog` | desk items with real sizes |
| POST | `/api/desk/layout` | `{deskWidthCm, deskDepthCm, handedness, items:[{type, x?, z?, rotated?}]}` |

Phone numbers are stored privately and never returned by the API.

## License

MIT © Karri Surya Venkata Prakash Rao
