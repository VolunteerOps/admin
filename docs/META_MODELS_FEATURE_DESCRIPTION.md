# Meta models in Sentinel and ResQOps — feature description

This document reproduces the original feature description, then records the
additional Meta-powered capabilities implemented since it was written, and
finally lists extended specialised capabilities that can be built on the same
foundation. Capabilities in the last section are proposals, not shipped
features, and are labelled as such.

## Original description

I used Meta products both to build Sentinel and to power its planning capabilities inside ResQOps.

During development, I used Muse Wonder 1.0 through Muse Code in the integrated terminal in VS Code to help build out Sentinel. This brought the coding assistant into the same workspace as my project files and terminal workflow. The feature spans the coordinator interface, backend services, weather-data processing and resource planning, so the implementation required those parts to work together around the same operational needs.

Within the application, Muse Spark 1.3, accessed through Meta Model API using `muse-spark-1.3`, powers the resource-planning assistant.

The assistant uses controlled backend tools to check mission requirements, volunteer qualifications, availability, equipment stock and existing commitments. It uses those results to produce structured recommendations that explain the suggested allocation, identify shortages and highlight decisions that require a coordinator’s attention.

For instance, recommending volunteers for a mission involves checking whether they have the required skills and are available. Equipment recommendations need to account for location and existing reservations. The assistant brings those checks into one planning workflow, while the backend enforces permissions and allocation constraints. A coordinator reviews the proposal before approved changes take effect.

Muse Wonder 1.0 also supports Sentinel’s weather-analysis workflow through Meta Model API. It coordinates queries and analytical tools working with the WeatherNext 3 data in BigQuery.

The workflow retrieves forecasts for a selected region and time window, calculates relevant weather indicators, and examines conditions against regional thresholds and available hazard information. These indicators can include rainfall accumulation, sustained high temperatures and periods of strong winds.

BigQuery and the analytical services perform the numerical calculations. Wonder works through the returned evidence, identifies information needed for further analysis and explains what the findings mean for preparedness. Assessments retain their source information, forecast timestamps, assumptions and missing inputs so coordinators can inspect their basis.

This connects weather analysis to resource planning. If an assessment identifies a period of concern, the coordinator can move into reviewing staffing, equipment and preparation needs within ResQOps.

For flood preparedness, the workflow considers forecast rainfall alongside available local evidence. Where the information supports an adverse-weather assessment but is insufficient for a reliable local flood-impact estimate, Sentinel makes that limitation explicit.

Meta’s products therefore have distinct roles in the project: Muse Wonder 1.0 supported development through Muse Code and supports the weather-analysis workflow, while Muse Spark 1.3 powers resource recommendations and coordinator planning.

## Additional implemented capabilities

The capabilities below are implemented in the backend and the coordinator
interfaces, and were not covered in the description above.

### Conversational weather analysis (Muse Wonder 1.0)

- **Natural-language weather questions.** Coordinators can ask about a period
  in their own words instead of only filling in a forecast form. Wonder
  selects the analysis window, and the backend retrieves evidence for the
  previous 31 days and the next 15 days, in spans of up to 14 days split into
  bounded queries.
- **Two retrieval paths, honestly labelled.** The same WeatherNext 3 forecasts
  can be retrieved through BigQuery and through the GCS Zarr store. Both are
  access paths to the same model, so the assistant states that agreement
  between them is not independent confirmation.
- **Archived forecasts are not observations.** For past periods the workflow
  retrieves archived forecasts — past predictions, not measured weather — and
  says so. Seasonal or ENSO outlooks are not connected, and the assistant
  explains that gap instead of extrapolating from short forecasts.
- **Daily preparedness briefing.** A briefing mode automatically covers the
  previous three days and the next seven, explicitly acknowledging gaps in
  coverage rather than presenting a seamless picture.
- **Station-level temperature detail.** Wonder can call a separate station
  tool that reads 0.05° station-head temperature and dew-point statistics
  from BigQuery alongside the 0.1° forecast grid, and it states what that
  table does not contain (no rain or wind).
- **Citation enforcement.** The backend validates that every source the
  assistant cites was actually retrieved in that turn. A response citing a
  source that was not retrieved is rejected, and the assessment records
  whether coverage was partial, complete or absent.

### Guided mission drafting (Muse Spark 1.3)

- **Conversation-first mission creation.** Spark helps a coordinator draft a
  mission one question at a time — title, brief, timing, capacity, skills and
  meeting point — retaining extra detail in notes and accepting corrections
  to earlier answers.
- **Real place lookup with confirmation.** When a location is given, Spark
  searches real place matches (Mapbox) and asks the coordinator to confirm
  the intended match before coordinates are used. A city centre is not
  treated as a verified meeting site.
- **Validated drafts, never auto-created missions.** The returned draft is
  checked against the actual mission form constraints. The interface shows
  whether the draft is ready and lists field-level issues. Spark never
  creates or dispatches a mission; the coordinator reviews the draft and
  explicitly creates the mission.

### Scoped evidence questions (Muse Spark 1.3)

- **Ask Sentinel over current operations.** A separate evidence question
  flow lets Spark read the latest stored forecast assessment and the scoped
  operational context for the region, then explain preparation options with
  source IDs, source times, gaps and uncertainty.
- **Human review is explicit.** Responses are flagged as requiring human
  review and record that no actions were executed.

### Threshold-explicit weather evidence (Muse Wonder 1.0 → Muse Spark 1.3)

- **Regional thresholds travel with the evidence.** Weather assessments now
  include the region’s configured heat, wind and hourly-rain thresholds, not
  just a policy version number. Rainfall, heat and wind indicators are
  compared against those thresholds in the analysis Wonder receives.
- **Heavy-rain duration is an indicator.** Each grid point reports the hours
  with mean rainfall at or above the regional hourly rain threshold,
  alongside accumulated rainfall, longest sustained heat and longest
  sustained strong winds.
- **Peaks flow into resource planning.** When an assessment is linked to a
  resource proposal, Spark receives the peak indicator values and thresholds
  as well as the narrative, so staffing and equipment preparation can be
  weighed against the measured concern — while weather evidence never
  authorises travel or deployment.

### Proposal lifecycle beyond generation (Muse Spark 1.3)

- **Edit, reject and recompute.** Coordinators can revise a proposal’s
  allocations (revalidated against eligible candidates, capacity and stock),
  reject it with a recorded reason, or recompute it from current data.
- **Expiring drafts and revalidation at approval.** Proposals expire after 30
  minutes. Approval rechecks the mission, stock versions and any linked
  weather assessment; if any of them changed, the proposal must be
  recomputed. Approval requires recent MFA and commits all assignments and
  reservations in one transaction, with an audit record.

### Model governance and resilience (both models)

- **Evidence before answers.** The Meta agent runner requires the named
  evidence tools to be called before a structured answer is accepted, uses a
  per-request tool allowlist with a bounded call budget, and treats all user
  and tool content as untrusted data.
- **Strict structured output.** Both models answer through strict JSON
  schemas validated by the backend; no model-selected tool can execute an
  allocation, run SQL, or change a record.
- **Bounded provider recovery.** If the provider intermittently reports a
  configured model as unavailable, the backend retries the same model and
  request a bounded number of times. It never silently switches model or
  mode, and credential failures are reported without exposing provider
  detail.
- **Scoped, metered access.** Assistant use is coordinator-only, gated by
  the organisation’s assistant capability, and metered against daily attempt
  limits. Requests are idempotent, so retries do not duplicate proposals,
  approvals or charges.

## Agent harness layer (all assistants)

Every Meta-powered assistant now runs inside a shared agent harness, in the
sense of a coding-agent harness rather than a single prompt-and-tools call:

- **Plan-first work.** The runner provides an `update_plan` tool. Assistants
  record ordered plan steps with statuses before and during evidence
  gathering; planning and weather runs can be started in PLAN_FIRST mode,
  where allocations from that run cannot be approved — a coordinator
  creates an execution run instead.
- **Step transcripts.** Each run stores a tool-step timeline (tool,
  duration, success or failure), its plan, its working notes
  (`record_note`) and its round count, alongside the structured result.
  The web and Kotlin interfaces render the plan checklist, notes and steps
  in every run detail.
- **Repair and verification.** A reply that fails its strict schema gets
  one guided repair round instead of failing the run. Resource proposals
  are verified against the validated snapshot, and flood syntheses are
  verified against the flood-evidence rules, before a draft is stored; a
  failed verification sends the draft back for one revision.
- **Chat-history accounting.** Mission-planner turns carry bounded message
  history plus the current draft, and each answer returns field progress
  and a draft checkpoint that can be restored. Weather conversations carry
  bounded history into Wonder and store it on the run. Form-based planners
  keep their history as stored runs per region.
- **Agent runs registry.** All run kinds share one registry in the web
  interface (Agent runs): list, detail with chat history and harness
  transcript, cooperative cancel of a running run, and follow-up branching
  — a child run linked to its parent that inherits the earlier question,
  summary and notes as context.
- **Budgets.** Round and tool-call budgets are explicit per run and
  metered by the existing daily assistant usage limits; the weather
  conversation's search budget is three broad searches per turn, shown in
  its evidence.

## Extended specialised capabilities

The capabilities below have since been implemented in the backend and both
coordinator interfaces (Specialised planning), on the same Meta-model
foundation — controlled tools, strict structured output, evidence citation
and coordinator approval. They are listed here as originally proposed, with
implementation notes where the shipped behaviour is narrower than the idea.

### Flood and water hazards

- **Gauge-linked flood synthesis.** Connect river-gauge, rainfall-gauge and
  catchment data as new controlled tools. Wonder would combine forecast
  rainfall with observed levels and catchment response, and Spark would turn
  an evidence-backed assessment into preparation proposals. A local
  flood-impact estimate would only be offered once a validated impact model
  exists; until then the current explicit limitation remains.
- **Rising-water watch briefs.** Scheduled Wonder briefings comparing the
  latest gauge trend with forecast rainfall, flagging when a river crosses
  coordinator-set watch levels, with the evidence retained per briefing.

### Wider hazard coverage

- **Heat-health planning.** Combine sustained-heat indicators with
  coordinator-supplied vulnerability information (elderly populations,
  outdoor work, shelter capacity) so Spark can propose shade, water and
  rest-cycle staffing — presented as preparation guidance, not medical
  advice.
- **Wind and access assessment.** Pair strong-wind durations with road,
  bridge and ferry status feeds so Spark can flag assignments whose access
  assumptions a coordinator must re-check before approval.
- **Multi-region comparison.** Wonder compares indicator peaks and evidence
  quality across an organisation’s regions so coordinators can see where
  preparation attention is most needed, with per-region source citations.

### Planning depth

- **What-if scenario planning.** A coordinator changes one assumption —
  capacity, a skill requirement, a stock level, an assessment window — and
  Spark recomputes the proposal against the same validated snapshot rules,
  showing a diff against the current draft rather than a new unverifiable
  plan.
- **Fatigue-aware scheduling.** Give Spark shift-history and rest-period
  tools so recommendations account for recent workload and rest, with the
  backend enforcing maximum shift and minimum rest constraints the same way
  it enforces stock and capacity today.
- **Supply-shortage options.** When stock cannot meet a plan, Spark proposes
  splits across storage locations or substitute items from the catalogue,
  each still validated against recorded stock before a coordinator approves.
- **Credential-expiry foresight.** Spark reviews upcoming credential
  expiries against the mission pipeline and highlights roles that will
  become unstaffable, so coordinators can schedule renewals before a
  shortage occurs.

### Communication and reporting

- **Audience-specific briefings.** Wonder and Spark draft the same evidence
  into different forms — a coordinator briefing, a short volunteer briefing,
  and a public-information summary — each retaining source citations and the
  same limitations, each approved by a coordinator before use.
- **Multilingual briefings.** Approved briefings translated for the languages
  a region’s volunteers and communities use, with the coordinator reviewing
  the translated text before distribution.
- **After-action summaries.** After a mission closes, Spark assembles the
  record — proposals, approvals, assignments, weather evidence cited — into
  a draft after-action summary for coordinator review, supporting learning
  without rewriting operational history.

### Evidence quality

- **Forecast-versus-outcome reviews.** Where observations later exist,
  Wonder compares what earlier assessments concluded with what was observed,
  and reports calibration gaps back to coordinators. This reviews past
  forecasts; it does not retrospectively turn them into observations.
- **Evidence-gap worklists.** From an assessment’s missing inputs, Spark
  drafts a checklist of what to obtain — a gauge reading, a road-status
  confirmation, a stock count — assigned and tracked like other coordinator
  tasks.

Every extension above keeps the existing boundaries: numerical work stays
in BigQuery and the analytical services, models work through controlled
backend tools and return structured results, and no model output takes
effect without coordinator review and approval.

Meta’s products therefore have distinct roles in the project: Muse Wonder 1.0
supported development through Muse Code and supports the weather-analysis
workflow, while Muse Spark 1.3 powers resource recommendations and
coordinator planning.
