# Sentinel weather explanation repair — 5 November 2026

## Findings

The run shown at 00:39 EAT on 5 November made eight successful Wonder requests. The gateway was working. The old failure handler discarded the underlying exception, model name and progress, so the precise historical exception cannot be recovered from that record. Reaching the eight-round ceiling is consistent with the logs, but is not conclusively established.

Code inspection found independent defects worth fixing:

- Weather offered `update_plan` and `record_note` alongside evidence tools, with no transition requiring a final answer. Bookkeeping could consume the same eight rounds needed to retrieve and explain evidence.
- A revised answer was not passed through the verifier again. Unsupported citations could survive a repair attempt.
- Concurrent identical tool calls could start duplicate queries. The parallel-call budget was checked inside each asynchronous callback and could be exceeded by a batch.
- Permanently unavailable weather sources were remembered only within one search, allowing another search in the same conversation turn to retry them.
- Stored failures omitted the model, progress and useful error category. The interface displayed a normal green limitations notice even for failed runs.

## Changes

- `weather-explanation.ts` centralizes the production weather harness. It offers only evidence tools and requests the final structured answer as soon as the required checks return. Unavailable sources are valid evidence of a data gap, not permission to fabricate a forecast.
- The shared harness reserves its last two rounds for completion and repair, rechecks repaired answers, and rejects unsupported citations that remain after repair. Weather keeps the original 1,600-token output limit.
- Tool calls share in-flight promises for identical validated inputs. A complete parallel batch must fit the tool budget before any call starts.
- Access/configuration failures are cached across searches within one weather conversation turn.
- Failed runs retain the selected model, completed checks, phase and a safe failure code. Server logs include the run reference and category without raw exception messages or provider responses.
- The portal distinguishes unfinished analysis, unavailable connections, timeouts and provider declines. Failed records no longer show the green weather-finding notice. Confirmed failed-run retries receive a fresh request key; uncertain interrupted requests retain their key.

## Verification

- 26 backend agent, transport and harness tests passed.
- 7 local gateway security/error-handling tests passed.
- 9 browser recovery tests passed, including the failed assessment display and analysis-limit retry. The final progress labels were also checked in the browser.
- Backend compilation and test type checks passed; the portal production build passed.
- Regression scenarios cover missing source access, successful synthetic evidence, malformed output repair, repeated fabricated citations, bookkeeping loops, simultaneous duplicate queries, batch budgets, safe failure diagnostics and explicit provider refusals.

## Separate provider blocker found during live testing

Live synthetic weather requests received HTTP 400 with `content_policy_violation` from Meta. Direct requests from the same computer received the same classification, so this was not a tunnel failure. Repeating the exact test at the original output limit did not resolve it. The gateway now preserves that safe error classification, and the application reports `AGENT_REQUEST_DECLINED` without automatically retrying it or exposing raw provider content.

Example Meta request IDs from 5 November, approximately 01:03 EAT:

- `5279d8b9-9694-4fdc-af6c-2be1c9da6a7a`
- `30a8e2f8-0fe2-4eb8-9b29-94b9af81a7c2`

The test question was: “How do the last three days of forecast rainfall compare with the coming week, and what should we check on the ground?” The tools returned synthetic data or explicit missing-access results; no real weather query, mission creation or allocation was performed. This benign-request rejection needs Meta's investigation. Passing fixture tests does not establish that Meta will accept the live request.

Existing failed assessments remain historical failures. New requests use the repaired workflow and retain better diagnostics.

## Deployment

Both staging releases completed successfully on 5 November 2026:

- API: `e478b31d-8b7f-44a7-845b-729c66de6de6`
- Admin portal: `edac132c-b356-426a-a456-d634279ecf66`

The local gateway was reloaded with refusal classification support while preserving its tunnel URL. Its gateway, tunnel and keep-awake processes remain running. No new database migration was needed.
