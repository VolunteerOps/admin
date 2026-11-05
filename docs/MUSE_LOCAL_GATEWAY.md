# Temporary Muse gateway for hackathon judging

The staging API can send Muse requests through this Windows computer. Users keep using the normal portal on any device. They do not install anything or receive the Meta API key.

**Verified live on 4 November 2026:** staging API deployment `1bdf8c40-f506-449d-8a25-dafd86fa8100` succeeded. Both models completed signed requests from the deployed Virginia API through this computer: Spark completed the mission requirements tool and a validated planning reply; Wonder completed the weather evidence tool and a validated explanation. These checks used synthetic evidence, created no missions, and ran no BigQuery/GCS queries. API readiness and the portal API proxy both returned HTTP 200. Six local gateway tests, nineteen backend agent/transport tests, backend compilation and test type-checking passed. The disposable diagnostic SSH key was revoked after verification.

```
Portal → authenticated Railway API → signed HTTPS request
       → temporary Cloudflare tunnel → local gateway → Meta Model API
```

The backend still checks workspace access, Pro entitlement, usage limits and tool permissions. Tool execution, data access, mission approval and dispatch stay in the backend. The gateway only relays completions for `muse-spark-1.3` and `muse-wonder-1.0` to Meta's fixed endpoint.

## Start, check and stop

Run from the admin repository in PowerShell, as the Windows account used during setup:

```powershell
# Start hidden background processes and update the staging API connection.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/muse-gateway/manage.ps1 -Action Start

# Check the gateway, tunnel and temporary keep-awake helper.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/muse-gateway/manage.ps1 -Action Status

# Stop the local processes. Muse will be unavailable until restarted.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/muse-gateway/manage.ps1 -Action Stop
```

Starting automatically reads the existing Meta secret using the authenticated Railway CLI, creates/reuses a separate gateway secret, checks the public tunnel's authentication, and updates **only the staging API**. Railway redeploys the API after reconnection; allow a few minutes. Re-running Start while both processes are healthy reconnects the same tunnel. A new tunnel gets a new URL, which Start updates automatically.

If the local gateway starts but Railway reconnection fails, confirm `railway whoami` shows the account with Kalama project access, then run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/muse-gateway/manage.ps1 -Action Connect
```

Keep this computer connected to power and the internet. You can lock Windows and close the terminal. The keep-awake helper prevents automatic system sleep while the gateway runs, without changing Windows power settings; it allows the display to turn off. Do not shut down, sign out, manually put the computer to sleep, or close the lid if Windows is configured to sleep on lid close. Restart Start after a reboot.

## Restore direct Meta access

Once Meta accepts Railway's requests, disconnect the gateway, wait for the API deployment to finish, verify chat, then stop the local processes:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/muse-gateway/manage.ps1 -Action Disconnect
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/muse-gateway/manage.ps1 -Action Stop
```

Disconnect clears `META_GATEWAY_URL` and `META_GATEWAY_SECRET` on the staging API. The original Meta key remains available there for direct access. Disconnecting before the provider issue is resolved will bring back the original error.

## Security and operational limits

- The gateway listens only on `127.0.0.1:4187`. No router port forwarding or firewall opening is needed.
- Each request has an HMAC signature over its method, path, timestamp, nonce and complete body. Expired, tampered and replayed requests are rejected. Windows and Railway clocks need to be accurate.
- The Meta key stays in the local gateway. Requests from Railway carry a separate gateway signature, not the Meta key. There are no browser credentials or public `NEXT_PUBLIC_*` secrets.
- Credentials and process state are in `%LOCALAPPDATA%\ResQOps\MuseGateway`, outside both repositories, restricted to this Windows account and SYSTEM. Anyone able to control this Windows account can read its secrets. Do not share the credentials file.
- The public tunnel terminates HTTPS at Cloudflare. Cloudflare carries prompt and response traffic; the HMAC authenticates the backend but does not add end-to-end encryption beyond TLS. Use this temporary path only for the authorized hackathon setup.
- Only the two approved model names and the completion route are accepted. Maximum request body: 512 KiB; response: 256 KiB; output tokens: 4096; simultaneous calls: 3; calls per minute: 40. The gateway cannot query databases or execute model tools.
- Logs contain timestamps, model names, response status and duration, not prompts, replies or credentials. Logs are under the same protected directory. Start overwrites the previous process logs.
- The tunnel has no uptime guarantee and does not support SSE. The backend uses complete JSON model responses while retaining its existing chat progress events and final reply. It does not automatically switch providers or invent responses when the gateway is offline.
- This addresses the Muse connection only. WeatherNext billing, bucket permissions and data availability still govern actual weather analysis.

## Installation and verification

Requires Node.js 24, the Railway CLI authenticated to Kalama, and the official Cloudflare client at `.work/tools/cloudflared.exe`.

Pinned release: [cloudflared 2026.10.7](https://github.com/cloudflare/cloudflared/releases/tag/2026.10.7), Windows amd64. The start script verifies SHA-256:

```
2493dbc168923bb3e59e5e7716f71deced92992eea9acd6e9d74f84ac7751343
```

Download a replacement only from that official release. If upgrading the pinned binary, review and update its checksum in `manage.ps1`.

Local gateway regression tests:

```powershell
node --test scripts/muse-gateway/server.test.mjs
```

Backend transport tests are included in `npm test` under `tests/meta-gateway.test.ts`. These cover signing, configuration validation, keeping the provider key out of relay requests, and retaining controlled tool calls when a streaming chat uses the JSON gateway.

References: [Meta key handling](https://dev.meta.ai/docs/authentication), [Cloudflare Quick Tunnel behavior and limitations](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).
