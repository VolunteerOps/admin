# Agent harness plan — Meta-powered assistants

Final version of the harness plan, including chat-history accounting for all
planners (added at execution). This plan turns every Meta-powered assistant
from a single bounded tool loop into a proper agent harness — plan-first
work, visible step transcripts, checkpoints, resumable/branchable runs,
verification, and human gates — modelled on what Claude Code/Codex provide
for software work, mapped onto coordinator work.

## Assistant inventory

| Assistant | Model | Current loop |
|---|---|---|
| Resource planner | Spark | 4–5 read tools, validated draft proposal, approve/edit/reject |
| Weather analysis (form) | Wonder | BigQuery indicators + hazards (+station), stored assessment |
| Weather conversation | Wonder | Window search (1/turn), daily brief, 16-message history |
| Mission planner chat | Spark | Draft fields, place lookup, schema-checked draft, never creates |
| Ask Sentinel Q&A | Spark | Single forecast tool, human-review flag |
| 14 specialised capabilities | Mixed | Single-shot tool loops, stored drafts |
| Shared runner | — | `meta-agent.ts`: max 5 rounds, 8 tool calls, required-tool gate, strict JSON output. No plan, transcript, memory, checkpoints, repair, or verification |

## Approach — harness mapping, finetuned per assistant

| Coding-harness feature | Platform equivalent (finetuned) |
|---|---|
| Plan mode + TodoWrite | Built-in `update_plan` tool: model records steps with statuses before evidence tools; plan stored and shown live; PLAN_FIRST mode gates execution tools behind coordinator plan approval |
| Step/tool transcript | Persisted step log per run: tool, argument summary, duration, ok/error, result digest — rendered as a timeline, not just tool names |
| Permission modes | Tool classes READ / COMPUTE / DRAFT-WRITE; reads auto-run, drafts are the only write, approvals unchanged |
| Subagents / parallel fan-out | Multi-region runs spawn one evidence sub-run per region; weather conversation search budget raised 1→3 with the budget shown |
| Compaction / memory | `record_note` tool for working notes; follow-up runs inherit a stored context summary instead of raw history; coordinator-pinned region brief acts as project instructions |
| Checkpoints / rewind | Mission-draft checkpoints each turn with restore; any stored run can be branched via follow-up (parent-linked child run) |
| Verification loop | One self-check pass before a draft is stored (proposal and flood first): verifier confirms allocations/claims against tool evidence; strict-schema failures get one repair round feeding validation errors back instead of failing |
| Background runs | Long analyses keep the existing RUNNING record pattern, add cooperative cancel and status polling in the UI |
| Templates / slash commands | Saved per-assistant prompt templates (quick briefs) in both frontends |
| Hooks | Post-run hook: weather assessments auto-offer gap-worklist creation (already deterministic) from the run detail |

Per-assistant finetuning: mission planner gets field-checklist progress +
draft checkpoints/restore; weather chat gets plan + multi-search budget;
resource planner gets PLAN_FIRST + plan-vs-execution transcript; Ask
Sentinel gains hazards/operations tools under the same harness; all 14
specialised capabilities inherit transcripts, notes and follow-up branching
automatically through the shared runner.

## Chat-history accounting (all planners)

Every planner's conversation state is explicitly accounted for:

- **Mission planner chat** — the client sends message history (max 20) and
  the current draft each turn; the backend answer returns field progress
  (completed/missing) and a draft checkpoint per turn, so any earlier draft
  state can be restored and re-submitted for the same schema validation.
- **Weather conversation** — history (max 16) is sent each turn and passed
  to Wonder with the question; stored conversation runs keep the request
  history in their payload, and follow-ups extend that history rather than
  starting a new thread.
- **Ask Sentinel** — single-question flow; its runs join the same registry
  so a follow-up branches from the stored question and answer.
- **Form-based planners (resource, weather, specialised)** — these are
  run-based, not chat-based: their "history" is the stored run list per
  region, each run keeping its request, harness transcript, plan and notes.
  The Agent runs registry unifies them: list, detail, and follow-up
  (branch) with the parent run's question, summary and notes carried into
  the child run's context, linked by `parent_run_id`.
- **Retention** — run payloads (including histories) follow the existing
  stored-record retention; no chat content is sent to Meta beyond the
  bounded history windows above.

## Steps

1. **Harness core** — extend `meta-agent.ts`: `update_plan`/`record_note`
   built-ins, step transcript with timings, configurable round/tool
   budgets, one schema-repair round, optional verifier pass; return
   `harness {plan, notes, steps, rounds}`.
2. **Persistence + modes** — embed harness data in stored payloads for
   planning, weather, conversations and specialised runs; add
   `mode: PLAN_FIRST | EXECUTE` to planning/weather create contracts
   (default EXECUTE; PLAN_FIRST stores the plan and pauses for approval
   before allocation-level output).
3. **Run registry** — `GET /agent/runs`, `GET /agent/runs/:id`,
   `POST /agent/runs/:id/follow-up` (branch with context summary),
   `POST /agent/runs/:id/cancel`; OpenAPI regenerated.
4. **Mission planner harness** — backend answer gains completed/missing
   field progress and a draft checkpoint per turn; restore is a
   client-held checkpoint re-submit validated by the same schema check.
5. **Frontend web** — Agent runs page (Sentinel area) with run timeline,
   plan checklist, notes, follow-up/branch and cancel; steps timeline +
   mode toggle in the existing Planning and Specialised drawers;
   MissionChat checklist + checkpoint restore; WeatherChat budget
   indicator; template quick-actions.
6. **Frontend Kotlin** — same steps timeline, plan checklist and mode
   chips in PlanningWorkspace and SpecialisedWorkspace; Agent runs list
   under Sentinel.
7. **Docs** — extend `META_MODELS_FEATURE_DESCRIPTION.md` with the harness
   layer per assistant.

## Validation Plan

- Backend: harness unit tests with a scripted fake Meta fetcher (plan tool
  enforced, transcript recorded, repair round recovers a malformed reply,
  verifier rejects an unsupported claim); full `npm test`, source/test
  typechecks, `contracts:check`.
- Web: Playwright test for the Agent runs page (fixture run with steps →
  follow-up creates a child run) plus existing journeys;
  `npm run typecheck`.
- Kotlin: `:shared:compileKotlinJvm`.
- Highest-risk check: the PLAN_FIRST gate — a plan-mode run must store no
  allocation until approved; covered by a backend test, not just UI state.

## Risks / Open Questions

- Provider limits: transcript/repair/verifier add Meta calls per run;
  mitigated by existing daily usage metering and making the verifier
  opt-in per assistant (default on for resource plan and flood only).
- Non-goals: no model-selected writes, no streaming transport, no change
  to approval/MFA rules; async worker execution of long runs stays as the
  current synchronous RUNNING-record pattern with cancel, rather than a
  new job queue.
