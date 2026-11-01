# ResQOps portal redesign — 30 October 2026

## Evidence and design status

The controlling source is the backend's seven-stage production roadmap, supplemented by its mission, Sentinel and organization contracts. The public website supplies an existing ResQ visual implementation: deep blue `#043473`, action blue `#2563eb`, gold `#e4d076`, sage `#b2b39d`, warm paper `#fdfcf7`, Oswald headings and Overlock text.

The requested Figma file is `TOYeWkoyeASTSC56TqHM3K`, Design sys link `139:34792`. MCP access is blocked by its seat quota. The user subsequently supplied six Design sys product shots; these supersede the older website typography and surface treatments. The implementation follows those shots, not an assertion of direct Figma inspection.

## Supplied design system

Raleway Bold: display 56/40 mobile and H1 40/32. Raleway SemiBold: H2 32/24 and H3 24/20. Open Sans: body 18/16/14 desktop and 16/14/13 mobile; captions 12/11. Source fonts are bundled locally with their OFL licences.

Semantic tokens: light canvas `#F8FAFC`, white cards, primary text/actions Konjo `#043473`, secondary text/hover Nude `#28518A`, muted Queen `#476998`; dark canvas `#021C40`, Konjo cards, white primary text, Mesh `#D7D6AE` secondary and Sage `#B2B39D` muted. Accent containers use Mesh at 25% in light and `#113E78` in dark. Papaya `#E4D076` is the dark primary action and status accent. Component corners are 16px. Focus must remain visible in both modes; component borders do not substitute for focus or essential control boundaries.

## Information architecture

Use six operational work areas plus a separate administration entry. Keep the full capability set, but stop exposing each record type as a primary destination.

| Work area | Local navigation | Primary operator question |
| --- | --- | --- |
| Overview | Attention queue, current missions, activity | What needs my decision now? |
| Missions | Missions, applications, assignments, reports, hours, deployment map | Who is doing what, where, and what is blocking them? |
| People | Roster, training, learning reviews, qualifications | Who can participate and what must be verified? |
| Sentinel | Radar, evidence, contextual assistant | What does the evidence show, and what remains uncertain? |
| Communications | Mission conversations, regional bulletins | What needs to be communicated, and to whom? |
| Resources | Inventory, stock history | What can we allocate without overcommitting? |
| Administration | Settings, dispatch controls, audit, insights, sponsorship | Who has access and how is the workspace governed? |

Profile, billing and support belong in secondary account navigation. A route remains addressable even when it is no longer a sidebar item.

## Journeys and interaction specification

### Coordinator starts a shift

1. Confirm workspace and region in the persistent header.
2. See current operational counts and a bounded queue derived from unresolved records, not interval-based vanity totals.
3. Open an attention item directly into its review context. Each item names the required decision; resolved items leave the queue after refresh.
4. Review current missions and their staffing, schedule and lifecycle. Zero activity produces one useful empty state with a relevant action, not multiple empty panels.

### Plan, staff and run a mission

1. Create a draft with a brief, meeting point, schedule, required skills and capacity.
2. Review the brief before publishing to eligible members. Publishing opens applications; activation is a separate human decision.
3. Open the mission's applications and assignments in context. Review qualifications and supplies before confirming an assignment. The server rechecks current capacity and availability.
4. Keep field reports and conversations adjacent to the mission. Review the latest record version before changing state.
5. Complete assignments before mission closure, then approve actual hours through a different coordinator.

### Review field evidence

1. Open an unresolved report from Overview or Missions.
2. Read its mission context and related evidence before choosing reviewing, action required or resolved.
3. Record a reason. Preserve versions and audit; a triage label does not claim emergency help was dispatched.

### Sentinel assessment

1. Open a map-led view with the region already selected. Keep ResQ's actual basemap style and attribution.
2. Select the forecast variable and valid time; independently toggle forecast, reported hazard and mission layers.
3. Inspect the same evidence through map selection or an accessible list. Show source/run time and coverage in the evidence panel.
4. Ask the assistant in a deliberate contextual panel. Missing evidence is explicit; neither a forecast nor an assistant answer becomes a movement order.
5. Proceed to mission planning for an authorized human decision. Never invent live positions, hazard probabilities or delivery receipts.

## Presentation rules

Use bounded page headings, concise labels, quiet separators and purposeful table columns. Use colour for selected navigation and meaningful status. Reserve gold for brand emphasis, never a universal warning. Put explanatory detail beside the decision that needs it or in disclosure panels. Avoid introductory paragraphs repeated on every screen, raw JSON, full ISO timestamp entry in normal workflows, generic 'Create' actions and decorative empty cards.

Keep loading, access denied, failed refresh, empty results and unconfigured providers distinct. Preserve pending mutation keys; confirmation must describe the effect. Search and filtering belong on the server so pagination cannot silently hide matching records. Browsing is keyboard operable; dialogs can be dismissed when no request is pending.
