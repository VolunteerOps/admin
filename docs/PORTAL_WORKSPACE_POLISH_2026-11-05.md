# Portal workspace refinements — 5 November 2026

Implemented locally in the React/Next.js admin portal:

1. Forms only save drafts when values differ from their initial defaults. Untouched
   forms disable Save draft; reverting entered values removes the draft. Empty/default-only
   saved entries are not offered for resumption. Existing assistant conversations retain
   their content guards.
2. Browser and Apple icon metadata use `/logo-monochrome.png`.
3. Reduced sidebar logo, heading and link spacing; removed the bottom slogan.
   All links fit the checked 1366×768 laptop viewport. Scrolling remains available
   for unusually short windows or enlarged text so links are never inaccessible.
4. Replaced the Pro banner with a compact papaya membership-style badge, without
   an arrow. It links to the existing subscription page and does not imply active entitlement.
5. The top bar remains sticky and gains a subtle shadow after scrolling.
6. Removed title eyebrows from page/drawer headers; primary tabs sit on the page
   canvas with curved shoulders, edge shadows and a bordered content sheet.
7. Hazard filters use compact, lightly tinted toggles with colour keys and counts.
8. Secondary tabs use a small segmented control instead of a second folder header.
9. Ask Sentinel opens beside the map at widths of 768px and above and stacks on
   phones. Closing it retains unsent text and conversation state. The map observes
   container resizing. Missing map configuration shows the hazard list without a blank canvas.

Validation: 26 distinct browser scenarios passed across workspace-polish,
qa-review, improvements and reference-experience; application/test TypeScript
checks and production build passed. Screenshots reviewed in light and dark mode,
including desktop and tablet. Browser tests use provider fixtures, not live Muse
or Mapbox service requests. No backend changes, Git push or deployment in this pass.

Screenshot output in the improvements tests now uses Playwright's per-test output
directory so running tests from a different working directory cannot write outside
the repository or overwrite design reference images.
