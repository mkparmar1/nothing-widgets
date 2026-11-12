# Dot Widgets — Design Quality & Illustration Plan

> **Priority:** #1 — comes before Phase 2 features (Builder 2.0 etc.) in `docs/FEATURE_ROADMAP.md`.
>
> **Goal:** every widget should look *designed by a person*, not generated from one template —
> professional, unique and warm — while keeping the Nothing dot identity that makes us recognisable.
>
> **Started:** 2026-11-13 · **Owner:** mkparmar1 · **Roadmap IDs:** P22–P25

---

## How to use this file

- Work top to bottom: **D0 → D8**. Each phase has tasks, acceptance checks and a status.
- **Status values:** `Not started` · `In progress` · `Testing` · `Done` · `Parked`
- **Effort:** `S` < 1 day · `M` 1–3 days · `L` 3+ days.
- A phase is `Done` only when its acceptance checks pass, the before/after contact sheets are
  reviewed, and `./gradlew :app:testDebugUnitTest` passes.
- Contact sheets come from `WidgetRenderSnapshotTest` → `app/build/widget-snapshots/all/_sheet_*.png`.

---

## Progress at a glance

| Phase | What | Effort | Roadmap | Status |
|---|---|---|---|---|
| **D0** | Audit the rest + automatic layout checks + realistic demo data | M | P22 | Not started |
| **D1** | Fix overlaps, clipping, truncation and Large-size scaling | M | P22 | Not started |
| **D2** | Designed empty, loading and permission states | S–M | P22 | Not started |
| **D3** | Variety pass — no more look-alike widgets | M | P22 | Not started |
| **D4** | Hand-made dot icon set (weather, system, status) | M | P22 | Not started |
| **D5** | Textures and colour moods | M | P22 | Not started |
| **D6** | Photo art styles + sticker cut-out | M | P23 | Not started |
| **D7** | "Dot Art" illustrated collection + mascot | L | P24 | Not started |
| **D8** | Sticker layer in the Widget Builder | M | P25 | Not started |

**Release gate:** D0–D2 must be done before the next Play upload (they fix visible bugs).
D3–D5 make the store screenshots stand out. D6–D8 are the "wow" features.

---

## Why this matters (what we compared)

Apps like Color Widgets (tens of millions of installs) and Widgetable feel premium because of:
- **Illustration and character** — hand-drawn art, cute characters, stickers.
- **Personal photos turned into art** — cut-outs, cartoon/sketch looks, frames.
- **Variety** — every widget has its own composition, not one template recoloured.
- **Texture and mood** — paper, grain, gradients, soft colour palettes.
- **Care in the details** — nothing overlaps, empty states are designed, text never clips.

**Our strength:** a clear, consistent Nothing dot identity across 232 widgets, and the theme
widgets (`themes/ThemeWidgets.kt`) already show depth and craft.
**Our weakness:** most widgets share one template (black card, small label top-left, red dot
top-right, dot digits), several have visible layout bugs, and there is no illustration at all.

---

## Audit findings (first pass — 6 of 19 categories)

Rendered with `WidgetRenderSnapshotTest` (Robolectric sample data, so values like `0%` are test
data — the **layouts** are what matter). Sheets checked: CLOCK, BATTERY, CALENDAR, WEATHER (dark),
THEMES (dark), FUN (light).

### A. Overlaps and clipping (bugs — fix in D1)

| Widget | Size | Problem |
|---|---|---|
| `fun.dice.roll` | S | Title "DICE ROLL · TAP TO ROLL" runs into the corner dot |
| `clock.radial.24` | M | Caption under the time overlaps the ring of dots |
| `calendar.day.date` | M | "NOV 2026" overlaps the big day digits |
| `calendar.life.dots` | M | "99% OF THE DAY" caption overlaps the dot grid |
| `calendar.countdown.multi` | M | Labels "NEW YE…" / "YEAR END" overlap the digits |
| `calendar.countdown.date` | M | "DAYS TO GO" touches the digits |
| `calendar.birthdays` | S | "NEW YEAR" dot text clipped on both sides (and shows a holiday, not a birthday — check data source) |
| `battery.health.percent` | S | "UNKNOWN" clipped at the left edge |
| `battery.charge.eta` | M | "UNPLUGGED" runs edge to edge, no breathing room |
| `battery.ring` / `battery.percentage` / `battery.minimal.pill` | S | Percentage digits collide with the ring dots |
| `weather.city.london` / `.tokyo` | M | Condition line ("CLEAR · HIGH…") overlaps the temperature digits |
| `weather.feels.humidity` | M | "FEELS LIKE" crowds the digits |
| `weather.moon.phase` | S | Three lines truncated ("WAXING CR…", "9% ILLUMIN…", "FULL MOON …") |
| `battery.combo.devices` | L | "ALLOW NEARBY DEVI…" truncated |

### B. Large size isn't really large (fix in D1)

Many **L** widgets draw the **M** layout shrunk into a bigger box: tiny text, lots of empty space.
Seen in: `battery.screen.week`, `battery.history.24h`, `battery.fuel.gauge`, `battery.combo.devices`,
`calendar.agenda.5`, `calendar.timeline.dots`, `calendar.week.scroll`, `calendar.month.heat`,
`calendar.moon.month`, `weather.5day.strip`, `weather.hourly.strip`, `weather.hourly.graph`,
`weather.golden.hour`, `clock.dotdigital.l`, `clock.flip.style`.

### C. Plain empty / permission states (fix in D2)

Plain centred text in an empty card: "ALLOW CALENDAR ACCESS IN THE APP" (agenda, timeline,
week scroll), "NO DATA YET", "COLLECTING… CHECK BACK SOON", "NOT REPORTED", "NO ALARM SET",
"Not playing".

### D. Look-alike widgets (fix in D3)

- ~10 clocks are nearly identical: `clock.dotdigital.m`, `clock.world.london/nyc/tokyo/custom`,
  `clock.seconds.bar`, `clock.segment.largewaves`, `clock.hijri`, `clock.moon.time`.
- `battery.ring` S, `battery.percentage` S and `battery.minimal.pill` S render the same design.
- `calendar.week.numbers` ≈ `calendar.iso.week` ≈ `clock.iso.dots`.
- `weather.visibility`, `weather.dewpoint`, `weather.precip.bar`, `weather.pressure.gauge`:
  same "label + digits + caption" layout.

### E. Icons are unreadable (fix in D4)

Weather conditions in `weather.current.temp`, `weather.5day.strip`, `weather.hourly.strip` and
the city widgets are drawn as red dot blobs — sun, cloud and rain aren't recognisable.

### F. Everything shares one template (fix in D3 + D5)

Same black card, label top-left, red dot top-right, white + red only. The battery corner dot is
orange while all others are red — decide if that's intentional (status colour) and document it.

---

## D0 — Audit the rest, automatic layout checks, realistic demo data

**Status:** Not started · **Effort:** M

**What:** finish the audit, and add tools so layout bugs are caught automatically, not by eye.

**How we build it:**
1. **Finish the audit** for the 13 remaining categories (PRODUCTIVITY, SYSTEM, TOGGLES, CONTACTS,
   PHOTO, MEDIA, FINANCE, HEALTH, SMART_HOME, SPORTS, LEARNING, TRAVEL) in light and dark; add rows
   to the tables above.
2. **Layout recorder:** in `RenderKit` / `DotMatrixRenderer` text and dot-text helpers, record the
   bounding box of every text/digit block drawn while a debug flag is on (ThreadLocal list, like
   the existing style ThreadLocals). Zero cost when off.
3. **`WidgetLayoutLintTest`:** render every widget × size × light/dark with the recorder on and
   fail when a block:
   - goes outside the widget's safe area (corner radius + padding),
   - overlaps another text block (allow listed exceptions, e.g. intentional overlays),
   - is drawn below a minimum readable size (e.g. 9 sp at the widget's density),
   - was truncated with "…" (helpers report when they ellipsize).
4. **Realistic demo data:** a `DemoData` provider used by snapshot tests (and optionally the
   in-app gallery previews): battery 76% charging, Mumbai 27° clear, 3 calendar events, a song
   playing, 6,240 steps… so sheets look like real use and Play screenshots are easy.
5. **Before/after sheets:** keep a copy of today's sheets in `docs/design-audit/before/` (or a
   local folder, not committed if large) to compare after each phase.

**Tasks:**
- [ ] Audit remaining 13 categories → add findings above
- [ ] Layout recorder in text/dot-text helpers (debug flag)
- [ ] `WidgetLayoutLintTest` (bounds, overlap, min size, truncation)
- [ ] Allow-list for intentional overlaps, each with a reason
- [ ] `DemoData` provider for tests/previews
- [ ] Save "before" contact sheets

**Acceptance:** the lint test runs in the normal unit test task; on today's code it reports at
least every bug in table A (proves it works); demo-data sheets look like a real phone.

---

## D1 — Fix overlaps, clipping, truncation and Large sizes

**Status:** Not started · **Effort:** M

**What:** fix every finding in tables A and B, and anything new D0 finds.

**How we build it:**
- **Fit, don't clip:** big dot-text auto-picks a smaller dot size (or fewer characters, e.g.
  "UNKN." → a "?" glyph) when it doesn't fit; captions get a reserved band so digits can't grow
  into them.
- **Safe area:** one shared inset function (corner radius + padding + corner-dot zone) used by
  every renderer — the corner dot gets its own reserved square so titles stop running into it.
- **Real Large layouts:** for each L widget in table B, add content instead of scaling: more
  rows (agenda 5 → 7 events), extra stats (battery: health, temp, time left), a chart, or a
  bigger hero number. Rule: **L must show more, not the same thing bigger-but-smaller**.
- **Truncation:** short labels where space is tight ("WAXING CR…" → icon + "9%"), full text in L.

**Tasks:**
- [ ] Shared safe-area + corner-dot reserve
- [ ] Auto-fit for dot text (shrink dot size → shorten → icon fallback)
- [ ] Fix every table A row (tick in the table when fixed)
- [ ] Redesign every table B L layout
- [ ] `WidgetLayoutLintTest` passes with no new allow-list entries

**Acceptance:** lint test green; every widget readable at S/M/L in light and dark; a person
reviewing the sheets finds no overlap or clipping.

---

## D2 — Designed empty, loading and permission states

**Status:** Not started · **Effort:** S–M

**What:** a shared `EmptyState` renderer: a small dot illustration (e.g. calendar with a lock for
permission, hourglass for collecting, music note for "not playing"), a one-line title and a short
hint ("Tap to allow calendar"), laid out to suit S/M/L. Tapping opens the right screen
(permission request, app, or settings).

**Kinds:** needs permission · collecting data · no data · nothing playing · no alarm · offline
(last updated time) · not supported on this phone.

**Tasks:**
- [ ] `EmptyState` renderer + 7 dot illustrations (hand-made in D4 style)
- [ ] Replace every plain empty text (table C) — search renderers for the strings
- [ ] Tap targets open the right place
- [ ] Snapshot tests render each empty kind at S/M/L

**Acceptance:** no widget shows bare centred text when it has nothing to show; every empty state
tells the user what to do.

---

## D3 — Variety pass (no more look-alike widgets)

**Status:** Not started · **Effort:** M

**What:** give every widget a reason to exist and its own composition.

**How we build it:**
- **Layout archetypes** — each widget picks one, and look-alikes must pick different ones:
  *Hero number* · *Ring/arc* · *List* · *Chart* · *Poster* (big typographic, edge-to-edge) ·
  *Split* (two halves, like `calendar.minimal.split`) · *Grid of tiles* · *Illustration + caption*.
- **World clocks:** one city per widget becomes a *Poster* with a skyline dot silhouette and
  day/night tint, instead of the plain digital clock with a city label.
- **Merge or redesign duplicates** (table D): e.g. `battery.percentage` becomes a big hero number
  without a ring; `battery.minimal.pill` becomes an actual pill; ISO week / week number widgets get
  distinct designs or are merged with a style option.
- **Label placement varies:** not every widget needs a top-left caps label; the red corner dot
  becomes a per-archetype choice (status dot only where it means something).
- **Typography:** use a second weight/size scale for posters and lists so not everything is
  dot digits.

**Tasks:**
- [ ] Assign an archetype to all 232 widgets (spreadsheet column in this file or a CSV)
- [ ] Redesign the ~10 look-alike clocks
- [ ] Battery S trio, week-number trio, weather "label + digits" family
- [ ] Corner-dot rule documented and applied
- [ ] Review sheets: no two widgets in a category look the same at a glance

**Acceptance:** in each category's contact sheet, a person can tell every widget apart in 2
seconds; nothing lost from the Nothing identity (dots, red accent, monochrome base).

---

## D4 — Hand-made dot icon set

**Status:** Not started · **Effort:** M

**What:** a set of pixel-perfect dot icons designed **by hand, dot by dot** — that's what makes it
feel human-made. Two grids: 9×9 (small) and 15×15 (large).

**Set (first version, ~40 icons):**
- **Weather (16):** clear day, clear night, partly cloudy day/night, cloudy, fog, drizzle, rain,
  heavy rain, thunderstorm, snow, sleet, hail, wind, hot, cold.
- **System (12):** battery, charging bolt, wifi, bluetooth, torch, DND, airplane, location, alarm,
  calendar, music, steps.
- **Status / empty (12):** lock, hourglass, check, warning, offline cloud, sparkle, heart, moon,
  sun, umbrella, gift, star.

**How we build it:**
- Icons stored as small string grids in `core/render/DotIcons.kt` (e.g. `"..XXX.."` rows) so they
  are easy to edit and review; drawn with the current skin and dot style.
- Optional 2-frame variants (rain falling, bolt flashing) for live overlays later.

**Tasks:**
- [ ] Design 40 icons (sketch on 9×9 / 15×15 grid paper first)
- [ ] `DotIcons` + renderer helper
- [ ] Replace weather blobs in all weather widgets (table E)
- [ ] Use icons in D2 empty states
- [ ] Snapshot sheet of all icons in every skin

**Acceptance:** 5 people can name each weather icon without help.

---

## D5 — Textures and colour moods

**Status:** Not started · **Effort:** M

**What:** optional surface finishes and palettes so widgets aren't all flat black/white + red.

- **Textures (background option):** Paper grain · Frosted glass · Soft gradient · Fine noise ·
  Dot-grid paper. Drawn procedurally (no big images), subtle by default.
- **Colour moods (one tap, sets accent + background tint + secondary colour):** Classic Red ·
  Midnight Blue · Forest · Sand · Lilac · Mono. Material You stays available.

**How we build it:** extend `BackgroundMode` / Theme Studio with a texture choice; textures drawn
in `DotMatrixRenderer.drawBackground` (same place the photo background is drawn); moods stored in
config like the custom accent.

**Tasks:**
- [ ] 5 procedural textures + intensity slider
- [ ] 6 colour moods + "secondary colour" support in renderers that use two colours
- [ ] Setup UI section; Theme Studio integration
- [ ] Contrast check for every mood × light/dark

**Acceptance:** textures render the same in preview and on the home screen; no mood fails
readable contrast.

---

## D6 — Photo art styles + sticker cut-out (P23)

**Status:** Not started · **Effort:** M

**What:** more ways to turn the user's photo into art, next to the existing dot filter (P1):

| Style | Look | How (on the phone) |
|---|---|---|
| Cartoon | flat colour areas + dark outlines | colour quantise (posterise / k-means on downscaled image) + edge detection overlay |
| Sketch | pencil drawing | greyscale → invert → blur → colour-dodge blend |
| Duotone | two-colour poster (uses accent) | luminance → gradient map |
| Halftone | newspaper print dots | dot size by luminance (close cousin of the dot filter) |
| Sticker cut-out | you / your pet cut out with a white outline, placed over the widget | Google ML Kit Subject Segmentation (on-device) → mask → outline stroke |

**Privacy:** everything runs on the phone; nothing is uploaded. ML Kit's on-device model is
downloaded by Google Play services — check its size, Android version support and licence before
committing, and update the privacy policy / Data safety form if anything changes.

**How we build it:** extend `PhotoFilter` + `PhotoDotFilter` pattern (cached, built off the main
thread, live preview in `PhotoPlacementEditor`); sticker cut-out stores the mask PNG next to the
photo in `BackgroundImageStore`.

**Tasks:**
- [ ] Cartoon, Sketch, Duotone, Halftone filters + unit tests on known images
- [ ] ML Kit spike: size, speed on a mid-range phone, failure cases
- [ ] Sticker cut-out mode (cut-out over plain/texture background, position + size in editor)
- [ ] Editor UI: style chips with live preview; strength slider
- [ ] Apply to Photo widgets too

**Acceptance:** each style renders in < 300 ms on a mid-range phone after first build (cached);
no network calls; cut-out handles "no subject found" gracefully.

**Not doing:** AI "cartoon of your face" models that need a server or a paid API — breaks the
"nothing leaves your phone" promise.

---

## D7 — "Dot Art" illustrated collection + mascot (P24)

**Status:** Not started · **Effort:** L

**What:** a new category of widgets built around **hand-drawn illustrations** in a dot/pixel
style, plus a small mascot ("Dot") whose mood follows real data.

**Mascot states (first set):** happy (normal) · sleepy (battery < 20%, night) · excited (charging)
· umbrella (rain) · sunglasses (hot) · scarf (cold) · party (birthday/holiday) · focused (focus
timer) · proud (step goal met) · confused (no data).

**Launch widgets (12):** mascot clock, mascot battery, mascot weather, mascot steps, mascot focus,
greeting card (morning/evening scenes), plant that grows with streaks (ties to P8), city skyline
weather, cosy room clock (lamp on at night), aquarium battery (water level), space scene date,
seasonal scene (changes by month).

**How we build it:**
- **Real illustrator** (human-made is the point). Deliverables: SVG / Android VectorDrawable on a
  dot grid, layered (background, character, props) so renderers can recolour with the accent and
  skin; each state as a separate layer set.
- Renderer: `widgets/catalogue/DotArtRenderers.kt` draws layers + live data; vectors converted to
  dot-style at render time or drawn as provided.
- **Licence:** written agreement giving the app full commercial rights; credit in About screen.

**Illustrator brief (to send):**
- Style: Nothing-inspired dot/pixel art, mostly monochrome + one accent colour, friendly,
  minimal, readable at 2×2 widget size (~150 dp).
- Grid: 1 dot = 1 unit; character fits 24×24 and 48×48 grids.
- Deliverables: mascot in 10 states, 8 scenes, 30 props; SVG + source file; light and dark.
- Usage: commercial, mobile app widgets and store screenshots.

**Tasks:**
- [ ] Write and send illustrator brief; agree price, timeline, licence
- [ ] Mascot sketches → approval → final vectors
- [ ] Layer format + recolouring by accent/skin
- [ ] 12 widgets at S/M/L with demo data
- [ ] New "Dot Art" category in the browser
- [ ] Store screenshots featuring the mascot

**Acceptance:** illustrations crisp at every size and density; recolour correctly with all
accents; mascot state always matches the real data.

---

## D8 — Sticker layer in the Widget Builder (P25)

**Status:** Not started · **Effort:** M · **Depends on:** D4, D7 (art), P14 Builder 2.0

**What:** a "Sticker" block in the builder: pick from the hand-drawn sticker pack (D7 props + D4
icons), plus decorative extras — washi tape, doodle frames, sparkles, arrows, hand-written-style
labels. Stickers can be rotated and scaled; the user's own sticker cut-out (D6) can be placed too.

**Tasks:**
- [ ] Sticker block type + schema (`"v": 2` builder layout)
- [ ] Sticker picker with categories
- [ ] Rotate / scale / layer order in builder canvas
- [ ] User cut-out stickers from D6
- [ ] Tests: sticker render at S/M/L, schema round trip

**Acceptance:** stickers render identically in builder, preview and home screen.

---

## Per-widget design review checklist

Use for every new or changed widget before marking a phase done:

- [ ] Nothing overlaps or touches the corner dot / edges (lint test green)
- [ ] Readable at S, M, L — L shows **more**, not the same smaller
- [ ] Looks right in light, dark, transparent and photo backgrounds
- [ ] Empty / permission / offline states designed (D2)
- [ ] Icons recognisable (D4 set)
- [ ] Clearly different from the other widgets in its category (D3)
- [ ] Uses demo data in snapshots; screenshot-ready
- [ ] TalkBack description makes sense (ties to P17)

---

## Changelog

| Date | Item | Commit |
|---|---|---|
| 2026-11-13 | Plan created after visual audit of 6 categories | — |
