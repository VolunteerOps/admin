# Portal error and recovery map � 1 November 2026

The frontend routes API, Firebase, form and assistant errors through one recovery-text layer. Known errors explain the problem and what the officer can do next. Unknown service errors do not expose provider internals, SQL, schema names or stack traces. The inventory in PORTAL_ERROR_INVENTORY.json records the originating backend files.

| Backend error family | What the officer sees |
| --- | --- |
| `TOKEN_EXPIRED`, `UNAUTHORIZED`, `SESSION_REVOKED`, `INVALID_FIREBASE_IDENTITY`, `FIREBASE_AUTH_REQUIRED` | Your sign-in has expired. Sign in again to continue. |
| `RECENT_AUTH_REQUIRED` | Confirm your password to make this change. Your work will stay here. |
| `EMAIL_VERIFICATION_REQUIRED` | Check your inbox and verify your email address before continuing. |
| `MEMBERSHIP_REQUIRED`, `SCOPE_REQUIRED`, `CAPABILITY_REQUIRED`, `PLATFORM_GRANT_REQUIRED`, `SUPPORT_GRANT_REQUIRED`, `RESPONDER_SCOPE_REQUIRED`, `GRANT_EXCEEDS_AUTHORITY` | Your current workspace role does not allow this action. Ask a workspace administrator for access. |
| `ORG_NOT_ACTIVE`, `ENROLLMENT_PENDING` | This workspace is not ready for operations yet. Ask its administrator to check its approval status. |
| `VERSION_CONFLICT`, `STALE_VERSION`, `ASSESSMENT_CHANGED`, `ASSESSMENT_POLICY_CHANGED`, `STREAM_RESNAPSHOT_REQUIRED` | This information changed since you opened it. Refresh, review the latest details, and try again. |
| `IDEMPOTENCY_CONFLICT`, `REPLAY_CONFLICT` | This request has already been used for a different change. Check the latest record before submitting again. |
| `AGENT_DAILY_LIMIT`, `INGRESS_LIMIT`, `INVITATION_LIMIT`, `JOIN_REQUEST_LIMIT`, `SHARE_LIMIT` | Your workspace has reached its limit for now. Try later or ask an administrator to review usage. |
| `WEATHER_SOURCE_UNAVAILABLE`, `GCS_READ_FAILED`, `PLACE_SEARCH_UNAVAILABLE`, `META_NOT_CONFIGURED`, `AGENT_UNAVAILABLE`, `ANALYSIS_FAILED` | The assistant could not finish this request. Your existing records have not changed. Try again shortly; if this continues, contact support. |
| `WEATHERNEXT_NOT_CONFIGURED`, `GCS_NOT_CONFIGURED` | Weather data access is not enabled for this service yet. Ask your administrator to finish connecting the weather data. |
| `FORECAST_QUERY_BUDGET_EXCEEDED` | This weather search is larger than the workspace's spending limit. Try a smaller area or ask an administrator to review the limit. |
| `WEATHER_ACCESS_DENIED`, `GCS_ACCESS_DENIED` | The weather provider has not granted this service access to the selected data. Your administrator needs to check the service account's access. |
| `WEATHER_BILLING_REQUIRED` | Google has stopped this weather search because of the project's billing or quota settings. Ask your administrator to review Google Cloud billing. |
| `FORECAST_RUN_UNAVAILABLE`, `STALE_FORECAST_RUN`, `ASSESSMENT_STALE`, `PROPOSAL_EXPIRED` | These forecast details are unavailable or too old for this decision. Run a new assessment using the latest available forecast. |
| `AGENT_IN_PROGRESS` | This assessment is still being prepared. Refresh its history in a moment; you do not need to submit it again. |
| `ANALYSIS_INTERRUPTED` | The assessment stopped before it finished. Check its history, then start a new assessment if needed. |
| `EMPTY_PROPOSAL` | Add at least one volunteer allocation before approving this plan. |
| `DISPATCH_PAUSED` | Dispatch is paused for this region. A coordinator must resume it before new deployments can start. |
| `STOCK_UNAVAILABLE`, `STOCK_LIMIT` | There is not enough available stock for this change. Check existing reservations or reduce the quantity. |
| `RESOURCE_MISMATCH` | These stock items use different types or units. Choose matching items for the transfer. |
| `RESPONDER_UNAVAILABLE`, `ASSIGNMENT_EXISTS` | This volunteer already has a commitment that prevents this assignment. Choose another person or time. |
| `CAPACITY_REACHED`, `COURSE_FULL` | There are no spaces left. Increase capacity if appropriate, or choose another activity. |
| `QUALIFICATION_REQUIRED`, `PREREQUISITE_REQUIRED`, `COURSE_PREREQUISITE` | The required training or qualifications are not complete. Review the person's qualifications before continuing. |
| `MISSION_CLOSED`, `MISSION_NOT_ACTIVE`, `INVALID_TRANSITION`, `COURSE_STATE`, `APPLICATION_NOT_WITHDRAWABLE` | This action is not available at the activity's current stage. Refresh and check its status. |
| `ASSIGNMENTS_OPEN` | Some assignments are still open. Complete or resolve them before closing the mission. |
| `ASSIGNMENT_WINDOW` | This action is outside the mission's scheduled time. Check the mission dates. |
| `APPLICATION_REQUIRED` | This volunteer must apply to the mission before they can be assigned. |
| `APPLICATION_EXISTS`, `ENROLLMENT_EXISTS`, `MEMBERSHIP_EXISTS`, `SPONSOR_EXISTS` | This person is already registered. Refresh to see their existing record. |
| `APPLICATION_LIMIT`, `SEAT_LIMIT` | The available places have been used. Ask your administrator to review capacity. |
| `MESSAGE_REMOVED`, `THREAD_UNAVAILABLE`, `CASE_RESOLVED`, `CASE_REPLY_LIMIT` | This conversation can no longer accept this change. Check its status or open a new support case. |
| `LESSON_ORDER`, `REVIEW_NOT_READY` | Complete the earlier learning steps before continuing. |
| `ASSESSMENT_COOLDOWN`, `ASSESSMENT_UNAVAILABLE` | This training assessment is not available yet. Check the course instructions and try when the waiting period ends. |
| `LAST_OWNER_REQUIRED`, `LAST_ADMIN_REQUIRED`, `HANDOVER_REQUIRED` | Assign another active administrator before removing this administrator's access. |
| `INVITATION_EXPIRED`, `INVITATION_CLOSED`, `INVITATION_UNAVAILABLE`, `INVITATION_ALREADY_ACCEPTED` | This invitation has expired, was already used, or is no longer available. Check your membership or ask for a new invitation. |
| `DOMAIN_DISPUTED`, `DOMAIN_CLAIM_CONFLICT` | Another workspace has a claim on this email domain. Ask support to review ownership. |
| `DOMAIN_PROOF_UNAVAILABLE`, `DNS_PROOF_UNAVAILABLE`, `DOMAIN_PROOF_MISMATCH` | We could not confirm the domain's verification record. Check the DNS instructions, then allow time for the change to appear. |
| `PRO_REQUIRED`, `VOLUNTEER_MEMBERSHIP_REQUIRED` | This feature is not included in the current account access. Check your membership and subscription. |
| `BILLING_UNAVAILABLE`, `BILLING_NOT_CONFIGURED`, `EMAIL_NOT_CONFIGURED`, `SSO_PROVIDER_NOT_CONFIGURED`, `STORAGE_NOT_CONFIGURED`, `STREAM_NOT_CONFIGURED` | This service has not been connected or is temporarily unavailable. Ask your workspace administrator to check its setup. |
| `STORAGE_QUOTA_EXCEEDED` | The workspace's file storage is full. Remove unneeded files or ask an administrator to increase storage. |
| `UPLOAD_EXPIRED`, `UPLOAD_NOT_FOUND`, `OBJECT_NOT_READY`, `OBJECT_MISMATCH` | This upload is unavailable or incomplete. Choose the file again and retry the upload. |
| `CONSENT_REQUIRED`, `DEVICE_CONSENT_REQUIRED` | Review and enable the required consent before using this feature. |
| `DEVICE_REVOKED`, `DEVICE_CONFLICT`, `DEVICE_LIMIT` | This device cannot be registered right now. Review your registered devices or contact support. |
| `PROFILE_SHARE_REVOKED`, `RECRUITMENT_UNAVAILABLE`, `MEMBERSHIP_REMOVED`, `REQUEST_CLOSED` | This access or request is no longer available. Refresh to review its current status. |
| `MFA_REQUIRED`, `SSO_REQUIRED` | Your workspace requires a different sign-in method. Contact its administrator for help. |
| `WEATHER_READ_BUDGET`, `WEATHER_WINDOW_TOO_WIDE`, `METRICS_RANGE_TOO_LARGE`, `PLANNING_SCOPE_TOO_LARGE`, `FORECAST_REGION_REQUIRES_TILING`, `FORECAST_RESULT_REQUIRES_TILING`, `ANALYSIS_REQUIRES_SMALLER_REGION` | This request covers too much information at once. Choose a smaller area or shorter date range. |
| `FORECAST_WINDOW_BEFORE_CYCLE` | The analysis starts before this forecast was issued. Choose a later start date, or use Ask Sentinel to explore earlier days. |
| `FORECAST_CYCLE_FUTURE` | This forecast has not been issued yet. Choose an earlier forecast time. |
| `FORECAST_HOUR_REQUIRED`, `INIT_MUST_BE_ON_HOUR` | Choose times on the hour, such as 09:00 rather than 09:30. |
| `INVALID_FORECAST_WINDOW`, `INVALID_ANALYSIS_WINDOW` | Choose an end time after the start, with no more than two days in this detailed assessment. Ask Sentinel for a broader question. |

## Other cases

- Firebase credentials: check email/password; no account-existence disclosure for a failed login.
- Invalid forms: identify the affected field and whether it needs more detail, a smaller value or a valid date. The detailed forecast form catches backward/future/subhour dates before a network request.
- HTTP 401/403/404/429/5xx: sign-in, access, missing item, wait, or service-recovery guidance respectively.
- Network failure: check connection and retry without losing the draft.
- Component failure: keep the workspace shell and offer a section retry. The root fallback also has clear recovery navigation.
- Map tiles: retain the evidence list if the map cannot load.
- Confirmation phrases for leaving a workspace or deleting an account remain explicit; these are deliberate safeguards.

Inventory codes handled by status-based fallback or representing record states rather than public error types: TENANT_MIGRATION_REQUIRED, FIREBASE_REVOCATION_FAILED, AUTHORIZATION_UNAVAILABLE, IDENTITY_UNAVAILABLE, CSRF_REJECTED, IDENTITY_CONFLICT, IDENTITY_MIGRATION_REQUIRED, VOLUNTEER, PUBLISHED, UNSAFE_RUNTIME_ROLE, REVOKED, WITHDRAWN, BOOTSTRAP_ALREADY_USED, IDENTITY_BINDING_REQUIRED, WORKSPACE_EXISTS, CASE_OWNER_REQUIRED, STORAGE_UNAVAILABLE, JOB_NOT_RETRYABLE.

## Scope of assisted and manual workflows

| Workflow | Default experience | Why review remains |
| --- | --- | --- |
| Weather preparedness | Three suggested questions, natural-language conversation and one-click briefing; controlled BigQuery/GCS retrieval | Sources may be missing; a forecast is not an observed flood impact |
| Detailed forecast/refresh | Advanced workflow; precise dates and plain validation | Reproducible review of a specific issued cycle |
| Region setup | Search city/district/country, choose match, inspect coverage map, give reason | Screening thresholds are defaults until locally reviewed |
| Mission planning | Spark asks one missing question at a time, incorporates corrections, produces a validated draft | Coordinator explicitly creates the mission; no automatic dispatch |
| Resource planning | Existing assistant checks qualifications, commitments and stock; weather evidence can carry into review | Approval enforces current stock, eligibility, scope and versions |
| Assistance cases | Clear status, handover and resolution actions | Case ownership and operational decisions must remain explicit |
| Stock, hours, qualifications and training | Labelled forms, field validation and actionable errors | These change auditable operational facts; the model does not invent quantities, hours or qualifications |
| Membership, roles, domains, privacy and billing | Guided forms, retained drafts and plain recovery text | Authority, consent and irreversible decisions remain explicit |
