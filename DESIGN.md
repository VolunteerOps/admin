# Admin design system

The September 2026 redesign prioritizes clear next steps, readable mobile layouts, and a calm community response identity. Existing local demonstration workflows must be described accurately; do not imply live telemetry, verified credentials, or message delivery without service integration.

## Color tokens

| Token | Value | Role |
| --- | --- | --- |
| Volunteer blue | `#2563EB` | Primary actions, active navigation |
| White | `#FFFFFF` | Cards and bottom navigation |
| Navy | `#0F172A` | App header and strong content |
| Slate | `#475569` | Supporting copy |
| CanvasBg | `#F5F8FC` | Page and modal header |
| BorderLight | `#DDE6F0` | Dividers and borders |
| HealingTeal | `#0F766E` | Available and positive status |
| On a mission | `#A16207` | Amber text on light surfaces |
| Taking a break | `#B42318` | Red text on light surfaces |

Use semantic labels as well as color. Admin assigned/deployed states use amber; unavailable uses red. Do not substitute pale yellow text on white: retain readable amber.

## Typography

Bundle Oswald for headlines and metric values, and Overlock for body, controls and labels. Use the installed regular, medium/bold weights rather than synthesized tracking or all-uppercase command language. Volunteer page headlines use 30sp/37sp, section headlines 23sp/30sp, body 16sp/24sp, labels 14sp/20sp. Admin uses its MaterialTheme typography definitions with these same font families; theme source is authoritative for individual role sizes. Allow system font scaling and natural wrapping.

The splash wordmark is traced from the approved artwork and is a brand artwork exception to the interface typography.

## Layout and navigation

- Single-column reading order on phones. Use adaptive metric columns and bounded 1100–1120dp content on larger screens. At 840dp, switch to rail/sidebar navigation.
- Page padding: 20dp horizontal, 24dp top, 28–32dp bottom; 18–20dp between major blocks. Cards: 20dp padding, 20–22dp corners and 1dp border.
- Volunteer header is white; admin header is navy. Paint its background behind the top system inset and use a 4dp bottom shadow. Keep controls below status icons.
- The white bottom navigation surface owns the bottom system inset. Paint through the gesture area, with touch targets above the handle. Do not apply a second safe-area padding around the entire app or hide system navigation.
- Remove page eyebrow labels above main titles. Keep useful card labels such as “Your next step.”
- Secondary destinations place a Back icon directly left of their title, inside a grey glass rounded square. Preserve the source destination for returning.
- Header actions belong at the far right of the title row. Use icon-only controls with clear accessibility descriptions. Titles consume the remaining width and may wrap; actions retain a minimum 48dp target.
- Profile skill search and add button share one row, with weights 4:1 and a 12dp gap. Add uses only the plus icon, with “Add a skill” accessibility text.

## Modal anatomy

Use the shared Sheet/DetailSheet. Pin a tinted page-background header above the scrolling body. Include the title, optional Back at its left, and close action at its right. A full-width divider always marks the bottom; a 5dp shadow appears after body scrolling begins. Content has 20dp padding and 16dp vertical spacing. Limit width to 560dp volunteer / 620dp admin and constrain height to the safe viewport, including keyboard insets. Do not nest an independently scrolling form unnecessarily.

## Glass and component behavior

Glass badges use translucent semantic tint, a thin tinted rim, a restrained white sheen, and readable foreground text. This is a portable surface treatment, not a claim of physical backdrop refraction. Back/action squares use 12dp corners and neutral slate tint. Buttons have at least 48dp touch height; primary actions are blue with white text. Prefer one next step and contextual secondary actions over repeated large banners.

## Motion

Active navigation runs one 2200ms illustration-specific sequence: home doorway illumination, searching lens scan, flag fabric flutter, typing dots, a profile nod, coordinated people acknowledgements, or tools emerging from the menu. Retain a stable icon silhouette and return to rest. Never loop navigation animation continuously. Volunteer’s reduced-motion preference disables this motion; animation timing also uses Compose’s animation machinery. Read animated values during drawing, not while building paths. Validate the final behavior under platform motion settings.

## Assets and identity

Use supplied linear OpsSymbol icons consistently. The volunteer header/launcher uses logo-mobile; admin uses the monochrome logo. Launcher display labels are **Volunteer** and **Admin**. The supplied atmospheric relief-camp splash stays shared in visual language, with role-specific identity. Preserve its aspect ratio and painted tagline reveal.

## Review checklist

Check narrow phones, expanded layouts, large text, keyboard-open forms, scrolled modal headers, status/gesture area continuity, meaningful icon accessibility labels, selected and inactive navigation, and reduced motion. Compile common metadata, Android, desktop/JVM, JS and Wasm. iOS rendering/build verification requires macOS. Verify the installed package version and APK hash against the original project build before calling a device update complete.
