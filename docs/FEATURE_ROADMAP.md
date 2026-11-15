# Dot Widgets — Advanced Feature Roadmap

> **Goal:** stay the most complete Nothing-style widget app on *any* Android phone, and answer
> Nothing's own moves (AI-built widgets, Relaxation Hub, Glyph Matrix) with features that work
> everywhere.
>
> **Started:** 2026-11-12 · **Owner:** mkparmar1 · **Companion docs:** `docs/BUILD_PLAN.md`,
> `docs/UX_IMPLEMENTATION_PLAN.md`, `docs/THEME_PLAN.md`, **`docs/DESIGN_QUALITY_PLAN.md` (current #1)**

---

## How to use this file

- Tick a box `[x]` when a task is done; update the card's **Status** line.
- **Status values:** `Not started` · `In progress` · `Testing` · `Done` · `Parked`
- **Priority:** `P1` is built first. **Effort:** `S` < 1 day · `M` 1–3 days · `L` 3+ days.
- Every card has **Acceptance** checks. A feature is only `Done` when all of them pass on a real
  phone *and* the unit tests pass (`./gradlew :app:testDebugUnitTest`).
- Record what shipped in the **Changelog** at the bottom (date + commit).

---

## Progress at a glance

| #   | Feature                              | Priority | Effort | Status      |
|-----|--------------------------------------|----------|--------|-------------|
| P1  | Dot-matrix photo filter              | P1       | S      | Done        |
| P2  | Widget Builder (make your own)       | P2       | L      | Done        |
| P3  | Better weather widgets               | P3       | M      | Done        |
| P4  | Smarter Smart Stack (time of day)    | P4       | S–M    | Done        |
| P5  | Lock screen check (Android 16)       | P5       | S      | Done        |
| P6  | Share a design (code / QR)           | P6       | S      | Partly done |
| P7  | Breathing & focus widget             | P7       | S–M    | Not started |
| P8  | Streak & habit widgets               | P8       | M      | Not started |
| P9  | Automatic night look                 | P9       | S      | Not started |
| P10 | Photo shuffle album                  | P10      | S–M    | Not started |
| P11 | Glyph light companion (Phone 3)      | P11      | M      | Not started |
| P12 | On-device usage statistics           | P12      | S      | Not started |
| P13 | Animated widgets (Remote Compose)    | P13      | L      | Parked      |
| P14 | Builder 2.0 (tap actions, rules, more blocks) | N1 | L   | Not started |
| P15 | Complete Looks (one-tap full theme)  | N2       | M      | Not started |
| P16 | Languages (Hindi + 4) & store listing | N3      | M      | Not started |
| P17 | Accessibility pass (TalkBack, contrast) | N8    | S      | Not started |
| P18 | Integrations: RSS news, Todoist      | N10      | M      | Not started |
| P19 | Wear OS tiles                        | N12      | L      | Not started |
| P20 | Social / shared widgets              | —        | L      | Parked      |
| P21 | "Describe your widget" (AI)          | —        | L      | Parked      |
| P22 | **Design quality pass** (D0–D5: layout bugs, Large sizes, empty states, variety, icons, textures) | **#1** | L | In progress |
| P23 | Photo art styles + sticker cut-out (D6) | #2    | M      | Not started |
| P24 | "Dot Art" illustrated collection + mascot (D7) | #3 | L | Not started |
| P25 | Builder sticker layer (D8)           | with N1  | M      | Not started |
| P26 | **App redesign v2** (Home, new nav, Details, tabbed Customizer, My Widgets tab, restyled Settings) — plan in [`docs/REDESIGN_V2.md`](REDESIGN_V2.md) | **#1 (with P22)** | L | In progress — R1–R5 done, R6 builder restyle next |

**Phase 1 (done):** P1 → P2 → P3 → P4 → P5, all checked on a phone on 2026-11-13.
**Current priority (#1):** design quality — full plan in
[`docs/DESIGN_QUALITY_PLAN.md`](DESIGN_QUALITY_PLAN.md) (D0 → D8). D0–D2 are required before the
next Play upload.
**Then Phase 2:** see [Phase 2 — next features](#phase-2--next-features-after-p1p5) (N1 → N13).

---

## Already done (baseline for this roadmap)

- [x] **Custom background photo** for 223 widgets (Theme widgets excluded — they paint their own art).
      Photo stays in app storage; config keeps id + darkening. — commit `e3e5bd0`
- [x] **Instagram-style placement editor:** widget-shaped window, pinch zoom (1–5×), drag, double-tap
      reset, thirds grid, Darken slider, Adjust later. Saves zoom + focus point so the same spot
      stays centred when the widget size changes. — commit `e3e5bd0`
- [x] **Android 14+ fix** for Favourite Contacts / Week Agenda row taps (`RowLaunchActivity`). — commit `25431dc`
- [x] **Device tests** (UI Automator): every widget draws, pin through launcher, refresh, tap. — commit `25431dc`

---

## Competitive picture (research, Nov 2026)

| | Dot Widgets | Nothing Widgets / Essential Apps | N-Style Widgets: Dot Matrix | KWGT (+ dot packs) |
|---|---|---|---|---|
| Widgets | **232**, 19 categories | Small set (clocks, weather, photos, compass…) + AI-made | "40+" | Unlimited (DIY) |
| Works on | **Any Android** | Nothing phones only | Any Android | Any Android |
| Make your own | ❌ (→ **P2**) | ✅ AI prompt (beta, approval, 3 permissions) | ❌ | ✅ (complex) |
| Styles | 8 skins, 4 frames, dot shapes, Material You, Theme Studio, icon pack | Nothing look only | Dot look only | Anything (manual) |
| Photo background + zoom | ✅ | Photo widget | ❌ | Manual |
| Cost to user | Free | Free (Nothing phone) | Free/IAP | KWGT Pro paid |

**What users want most (sources at the bottom):** weather (46% of ~3,200 votes), widget stacks
(top launcher wish), icon packs (2nd), streak/progress widgets (Google I/O 2026 template),
lock-screen widgets (Android 16 QPR2).

---

## P1 — Dot-matrix photo filter

**Status:** Done · **Effort:** S · **Area:** photos

**What:** an optional filter that redraws the background photo (and the 4 Photo widgets) as a grid
of dots, like Nothing's dot-matrix art. Modes: *Off*, *Mono dots* (white on black), *Accent dots*
(widget accent colour), *Colour dots* (photo colours). A *Dot size* slider.

**Why:** no competitor offers it; it's Nothing's signature look; it builds directly on the photo
feature just shipped, so it's cheap and visibly impressive in screenshots.

**How we build it:**
- `core/render/PhotoDotFilter.kt` — sample the photo on a grid (cell = dot size × density), draw one
  circle per cell; radius from brightness (mono/accent) or fixed radius with sampled colour (colour).
  Reuse `DotMatrixRenderer` dot shapes so the current **Dot style** (round/square/diamond…) applies.
- Cache the filtered bitmap per (photo id, mode, dot size, accent, widget size) — it's expensive.
- `BackgroundImageStore.Choice` + config keys: `bg_image_filter`, `bg_image_dot`.
- `DotMatrixRenderer.drawPhotoBackground` — draw filtered bitmap when filter ≠ Off.
- UI: filter chips + dot-size slider in `PhotoPlacementEditor` (live) and in `BackgroundImageSection`.

**Tasks:**
- [x] Filter renderer (mono / accent / colour) + unit test on a known gradient image
- [x] Config keys + `Choice` round trip test
- [x] Bitmap cache with sensible memory limit
- [x] Editor chips + dot-size slider (live preview)
- [x] Apply to Photo widgets (`PhotoRenderers.kt`) as well
- [x] Phone check: render speed on 4×4 widget < 150 ms after first draw

**Acceptance:** filter visible in editor, setup preview and home screen identically; switching
modes never crashes; no visible lag when dragging in the editor; Off = exactly today's behaviour.

---

## P2 — Widget Builder (make your own widget)

**Status:** Done · **Effort:** L · **Area:** new feature

**What:** users compose their own widget from blocks on a grid, all drawn in Dot Widgets style.
Blocks: Time, Date, Battery, Weather (temp/icon), Steps, Next event, Text, Progress ring, Image,
Divider, Spacer. Each block: position, size, alignment, font scale, colour (accent/primary/muted).
Saved designs appear in the catalogue under **My widgets** and can be placed like any widget.

**Why:** Nothing's headline feature is AI-built widgets — but only on Nothing phones, in beta, with
account approval and 3 permissions. A block builder gives the same "it's mine" feeling on every
Android phone, with no account, and is the strongest new line for the Play listing.

**How we build it:**
- Data: `data/builder/CustomWidget.kt` (Room entity: id, name, supported sizes, JSON layout), DAO, repository.
- Render: `widgets/builder/BuilderRenderer.kt` — one `WidgetDefinition` per saved design, registered
  dynamically in `WidgetRegistry` (prefix `custom.`), each block drawn with existing `RenderKit` /
  `DotMatrixRenderer` helpers so skins, frames, accent and photo backgrounds all apply for free.
- Data sources reuse existing repositories (`data/battery`, `data/weather`, `data/calendar`, `data/health`).
- UI: `ui/builder/BuilderScreen.kt` — canvas with snap grid (4×4 cells per widget cell), block
  palette, property sheet; undo/redo; templates to start from.
- Permissions requested only when a block needs them (same `AppPermission` flow as today).

**Tasks:**
- [x] Layout JSON schema + versioning (`"v": 1`)
- [x] Room entity, DAO, migration
- [x] Dynamic registration in `WidgetRegistry`
- [x] Block renderers (Time, Date, Battery, Text, Progress ring)
- [x] Builder canvas: add / move / resize / delete, snap grid
- [x] Property sheet per block
- [x] Undo / redo
- [x] 6 starter templates
- [x] Weather, Steps, Next event, Image blocks
- [x] "My widgets" section in browse
- [x] Tests: schema round trip, every block renders at S/M/L, broken JSON falls back safely

**Acceptance:** a user with no instructions can build and place a clock + battery widget in under
2 minutes; design survives app restart and phone reboot; all skins/frames apply; bad data never
crashes the launcher (falls back to a plain clock like other widgets).

**Remaining validation:** verify the two-minute build flow, restart/reboot persistence, and launcher
placement on a real phone before changing status to `Done`.

---

## P3 — Better weather widgets

**Status:** Done · **Effort:** M · **Area:** weather

**What:** fill the gaps in weather: hourly temperature graph (dot sparkline), rain chance bars,
air-quality ring, severe-weather alert badge, sunrise/sunset arc with daylight left, "feels like",
wind with direction dots.

**Why:** weather is the most-used widget type by far (46% in Android Authority's poll). Better
weather = more daily views = better reviews.

**How we build it:**
- Audit existing weather widgets in `WeatherRenderers.kt` first — list what already exists
  (we have `WeatherRepository`, `AirQualityRepository`, sunrise arc, precip bar).
- Extend `WeatherRepository` with hourly data and alerts if the provider supports them.
- New renderers only for real gaps; reuse dot-graph helpers.

**Audit and implementation notes:** the app already had current conditions, a 5-day strip, an hourly
strip, a precipitation widget, live AQI, sunrise/sunset, feels-like/humidity, and wind speed. It had
no hourly temperature graph; the deep precipitation widget incorrectly plotted temperature as rain
chance. A new graph preserves the existing hourly strip, and precipitation bars now use the provider's
probability field. Severe-weather indicators are opt-in and derived from forecast weather codes; they
are not official emergency alerts. Network-backed weather surfaces show cached-data age when offline.

**Tasks:**
- [x] Audit: list existing weather widgets and data fields → write gaps here
- [x] Hourly graph widget (M, L), without replacing the existing hourly strip
- [x] Rain chance bars use hourly precipitation probability rather than temperature
- [x] Forecast severe-weather indicator on supported widgets (opt-in option)
- [x] Offline / stale data state ("updated 3 h ago") on forecast-backed weather widgets
- [x] Tests: renderer snapshots plus full, partial and missing forecast-helper inputs
- [x] Connected-phone rendering of the hourly graph, hourly strip and both precipitation widgets with live forecast data
- [ ] Phone acceptance: place the new graph and existing hourly strip, then check online/offline refresh behavior

**Acceptance:** every weather widget shows sensible content with no network; data refresh respects
battery (no more often than today); location permission asked only once, with a clear reason.

---

## P4 — Smarter Smart Stack

**Status:** Done · **Effort:** S–M · **Area:** collection widgets

**What:** Smart Stack can rotate automatically: pick which card shows by time of day (e.g. Morning
→ Weather, Day → Calendar, Evening → Battery, Night → Clock), or "smart" (next event soon → Calendar,
battery < 20% → Battery).

**Why:** stacks were the most-wanted launcher feature in a reader poll; we already have the stack
(`CollectionWidgets.STACK_KEY`), so this upgrades an existing feature cheaply.

**How we build it:**
- New stack options: `stack_mode` (Manual / Time of day / Smart) + time slots.
- `WidgetAlarmScheduler` schedules a refresh at slot boundaries; `RemoteViews.setDisplayedChild`
  (or stack adapter position) picks the card.
- UI in `StackItemsSection`.

**Tasks:**
- [x] Options + config round trip (Manual / Time of day / Smart and slot card choices)
- [x] Inexact time-slot alarm scheduling and displayed-card selection at each slot boundary
- [x] Pure selection rules (event within 30 min, battery below 20%, charging, time-slot fallback)
- [x] Manual swipe still works and remains selected until the next slot boundary
- [x] Tests for slot selection logic (pure function)

Smart signals are evaluated at slot boundaries; this preserves manual swipes between boundaries without
requiring the launcher to report its current StackView position.

**Acceptance:** card changes within a few minutes of a slot boundary; no extra battery drain
visible in Battery settings over 24 h.

---

## P5 — Lock screen check (Android 16)

**Status:** Done · **Effort:** S · **Area:** release quality

**What:** make sure widgets look and behave right in Android 16's phone lock-screen widget area
(vertical stack, up to 3 per page).

**Why:** Android 16 QPR2 brings lock-screen widgets to phones. Broken lock-screen widgets mean bad
reviews right when people discover the feature. We already have lock providers
(`res/xml/dot_widget_lock_s.xml`, `dot_widget_lock_m.xml`) and `LockScreenWidgetsTest`.

**Tasks:**
- [ ] Check which widgets the lock providers expose; confirm none show private data (to-dos, contacts)
- [ ] Emulator on Android 16 QPR2+ with lock-screen widgets enabled — screenshot each lock widget
- [ ] Check text size / contrast on the lock wallpaper (transparent background mode)
- [ ] Taps from the lock screen ask to unlock first where needed
- [ ] Decide whether home-screen providers (`dot_widget_info_*.xml`, `widgetCategory="home_screen"`) should also allow `keyguard`

**Acceptance:** every lock widget readable on light and dark wallpapers; no private content shown
while locked; no crash on tap.

---

## P6 — Share a design (code / QR)

**Status:** Not started · **Effort:** S · **Area:** growth

**What:** "Share design" in widget setup creates a short code / QR / link containing the style
(skin, frame, accent, background, options — **not** personal content or photos). "Import design"
applies it.

**Why:** people love showing off home screens; every shared design is free promotion. Config is
already JSON (`importedConfigJson` path exists in `WidgetSetupState.defaults`), so no server needed.

**How we build it:**
- Whitelist of shareable keys → compact JSON → Base64URL (optionally deflate) → `dotwidgets://design/<code>`.
- QR via a small encoder (or ZXing core) — no camera permission needed to *show* it; importing by
  paste or Android's built-in QR scanner opening the link.
- Strip `bg_image*`, notes, contacts, any personal fields.

**Tasks:**
- [ ] Shareable-key whitelist + encode/decode + tests (round trip, malicious input rejected)
- [ ] Deep link intent filter
- [ ] Share sheet + QR image
- [ ] Import screen with preview before applying

**Acceptance:** design shared from one phone reproduces exactly on another; no personal data in the
code; garbage codes show a friendly error.

---

## P7 — Breathing & focus widget

**Status:** Not started · **Effort:** S–M · **Area:** health / wellbeing

**What:** tap the widget to start a 1-minute breathing session shown as dots growing and shrinking
(in, hold, out), with gentle vibration on each phase. Patterns: Calm 4-7-8, Box 4-4-4-4, Quick 3-3.

**Why:** Nothing's newest widgets (Relaxation Hub in Nothing OS 4.1) are breathing exercises with
haptics — we match it on every phone.

**How we build it:**
- Widget shows a static preview + "Tap to breathe"; tap opens a small translucent activity
  (like `RowLaunchActivity`) that runs the animation in Compose with `Vibrator` patterns.
- Optional daily minutes counter on the widget.

**Tasks:**
- [ ] Widget renderer (S, M)
- [ ] Session activity with 3 patterns + haptics (respects system "touch vibration" off)
- [ ] Minutes-today counter
- [ ] Tests: pattern timing logic

**Acceptance:** session starts within 1 s of tap; vibration matches phases; works with vibration off.

---

## P8 — Streak & habit widgets

**Status:** Not started · **Effort:** M · **Area:** productivity

**What:** habits (water, reading, workout, custom) with a tap-to-check widget and a streak counter
drawn as a dot calendar (last 7 / 30 days).

**Why:** brings users back daily; Google is promoting a streak widget template (I/O 2026).

**Tasks:**
- [ ] Room table for habits + check-ins
- [ ] Interactive check button on the widget (broadcast, explicit intent)
- [ ] Dot-calendar renderer (S, M, L)
- [ ] Streak rules (missed day resets; optional "rest days")
- [ ] Midnight rollover refresh
- [ ] Tests: streak calculation across time zones / DST

**Acceptance:** checking in from the widget updates within 1 s; streak correct after a day change
and after reboot.

---

## P9 — Automatic night look

**Status:** Not started · **Effort:** S · **Area:** customisation

**What:** global setting: at bedtime switch all widgets to a night style (dim, red dots, or force
dark) and back in the morning. Times manual or follow system Bedtime / sunset.

**Why:** easy, noticeable, and comfortable at night.

**Tasks:**
- [ ] Setting + schedule (two daily refreshes)
- [ ] Night style applied in `WidgetRenderEngine` (override accent/dim, don't touch saved config)
- [ ] Per-widget opt-out
- [ ] Tests: style selection by time

**Acceptance:** switch happens within minutes of the set time; turning it off restores every widget exactly.

---

## P10 — Photo shuffle album

**Status:** Not started · **Effort:** S–M · **Area:** photos

**What:** choose several photos for a widget background (or Photo widget); it changes every hour /
day / on tap. Each photo keeps its own zoom placement (reuses the editor).

**Tasks:**
- [ ] Multiple `Choice` entries per widget (config array) + migration from single photo
- [ ] Rotation schedule + "next photo" tap action
- [ ] Editor: album strip, reorder, adjust each
- [ ] Clean up photo files no longer used by any widget (also fixes today's leftover-file note)

**Acceptance:** rotation survives reboot; deleting a photo never leaves a broken widget.

---

## P11 — Glyph light companion (Nothing Phone 3)

**Status:** Not started · **Effort:** M · **Area:** Nothing phones

**What:** mirror chosen widgets (timer, battery, notification count, clock) onto the Phone (3)'s
back Glyph Matrix light.

**Why:** unique — no general widget app does it, and Nothing fans love it. Limited to a few phones,
so lower priority.

**Tasks:**
- [ ] Read Nothing's Glyph Matrix developer kit docs and licence; confirm Play policy fit
- [ ] Prototype one "toy" (battery)
- [ ] Hide the feature on non-Nothing phones

**Acceptance:** works on a real Phone (3); app unaffected on every other phone.

---

## P12 — On-device usage statistics

**Status:** Not started · **Effort:** S · **Area:** product insight

**What:** record (on the phone only) which widgets are placed, tapped and kept, using Android 16's
widget event APIs (`AppWidgetEvent`) where available. Optional screen in Settings: "Your widgets".

**Why:** tells us what to improve next without collecting data off-device (privacy policy stays true).

**Tasks:**
- [ ] Needs `compileSdk 36.1` — check build impact
- [ ] Local counters in Room; never uploaded
- [ ] Settings screen (optional)

**Acceptance:** no network calls added; privacy policy unchanged.

---

## P13 — Animated widgets (Remote Compose) — Parked

**Status:** Parked · **Effort:** L

**What:** smooth animations (ticking dots, flowing bars) using Google's Remote Compose widgets
announced at I/O 2026 (Android 16+).

**Why parked:** brand new, few phones, APIs still settling. Revisit when it's stable and the
launcher support is wider. Today's live overlays already cover ticking clocks/timers.

---

## Phase 2 — next features (after P1–P5)

### Where we stand now (Nov 2026)

| | **Dot Widgets** | Nothing Widgets + Essential Apps | Color Widgets | Material You Widgets | Widgetable | Chronus | KWGT |
|---|---|---|---|---|---|---|---|
| Size / reach | New | Nothing phones only | ~25–50M installs, ~4.6★ | ~4.6★, 4.6K reviews, $0.99 | Social, large | Long-running | The power-user standard |
| Ready widgets | **232** + own designs | Small set + AI-made | Many templates | "300+" | Social widgets | ~10 types | DIY / packs |
| Make your own | ✅ block builder (P2) | ✅ AI prompt (beta, approval) | ❌ | ❌ | ❌ | ❌ | ✅ (complex) |
| Dot / Nothing look | ✅ 8 skins, frames, dot shapes | ✅ (only look) | ❌ | ❌ | ❌ | ❌ | Via packs (KWGT Pro) |
| Photo background + zoom + dot filter | ✅ | Photo widget | Photos | ❌ | Photos | ❌ | Manual |
| Smart Stack auto-rotate | ✅ (P4) | ❌ | ❌ | ❌ | ❌ | ❌ | Manual |
| Lock screen widgets | ✅ hardened (P5) | ✅ (OS 4.1) | ✅ | ? | ✅ (main feature) | ✅ | ✅ |
| One-tap full theme (wallpaper + widgets + icons) | Pieces exist (Theme Studio, icon pack, wallpapers) | System themes | ✅ main draw | ❌ | ❌ | ❌ | Via KLWP |
| Share a setup | ✅ code (`SetupShareRepository`) + bundled community picks | Playground (paused) | ❌ | ❌ | ✅ with friends | ❌ | Export presets |
| Tap actions / rules inside a widget | Fixed tap per widget | Limited | ❌ | ❌ | ❌ | Some | ✅ formulas, Tasker |
| Tasks / news / stocks integrations | Calendar, finance, sports… | Calendar, contacts | ❌ | ❌ | ❌ | ✅ Google Tasks, Todoist, RSS, stocks | Via plugins |
| Social widgets with friends | ❌ (no server, by design) | ❌ | ❌ | ❌ | ✅ main feature | ❌ | ❌ |
| Languages | **English only** | Many | Many | ? | Many | Many | Many |
| Wear OS | ❌ | — | ❌ | ❌ | ❌ | ✅ | ❌ |
| Price | Free | Free (Nothing phone) | Free + IAP/sub | $0.99 | Free + sub | Free + IAP | Free + Pro |

**We already win on:** number of dot-style designs, the builder + dot look on any phone, photo
editor with dot filter, auto-rotating Smart Stack, free with no account, on-device privacy.

**Gaps that stop us being clearly #1:**
1. Builder can't do tap actions or "if this then that" rules (KWGT can; Nothing's AI fakes it).
2. No one-tap "complete look" — Color Widgets' biggest draw (wallpaper + widgets + icons together).
3. English only — limits Play search reach, especially in India where we already have India widgets.
4. No task/news integrations (Chronus' strength).
5. Social widgets (Widgetable) — needs a server; conflicts with our privacy promise → parked.

### Phase 2 priority order

| Order | ID | Feature | Why | Effort |
|---|---|---|---|---|
| **N1** | P14 | **Builder 2.0** | Closes the KWGT gap and beats Nothing's AI on any phone: tap actions per block, rules, more blocks, share designs | L |
| **N2** | P15 | **Complete Looks** | Color Widgets' #1 feature; we already have the pieces, so it's mostly packaging | M |
| **N3** | P16 | **Languages + translated store listing** | Biggest cheap boost to Play discovery; no competitor dot app is localised | M |
| **N4** | P8 | **Streak & habit widgets** | Daily return reason; Google promotes the streak template | M |
| **N5** | P6 | **Share single design / QR** (finish) | Setup code exists; add one-widget share, QR, and builder-widget share | S |
| **N6** | P7 | **Breathing & focus** | Matches Nothing OS 4.1 Relaxation Hub on every phone | S–M |
| **N7** | P9 | **Automatic night look** | Small, very noticeable | S |
| **N8** | P17 | **Accessibility pass** | Play quality + reviews; TalkBack labels for every widget | S |
| **N9** | P10 | **Photo shuffle album** | Reuses the photo editor + dot filter | S–M |
| **N10** | P18 | **RSS news + Todoist** | Chronus' strength, without Google sign-in (Todoist uses a personal token) | M |
| **N11** | P11 | **Glyph light companion** | Unique on Nothing Phone (3) | M |
| **N12** | P19 | **Wear OS tiles** | Chronus has it; smaller audience | L |
| **N13** | P12 | **On-device usage stats** | Tells us what to build next | S |
| Parked | P20, P21, P13 | Social widgets, AI describe, animated widgets | Need a server / paid AI / new APIs; conflict with "no account, on-device" | — |

---

## P14 — Builder 2.0

**Status:** Not started · **Effort:** L · **Area:** builder

**What:** make the builder as capable as KWGT's everyday use, but simple.
- **Tap action per block:** open app, open URL, toggle torch, start timer, next stack card, open
  a builder page.
- **Rules:** "if battery < 20% → colour red", "if event within 30 min → show", "if charging → show
  bolt", "night (22:00–06:00) → dim". Simple dropdown rules, no formulas.
- **More blocks:** analog clock, music now-playing, alarm next, countdown, screen time, AQI,
  steps ring, world clock, moon phase, icon/shortcut.
- **Share a builder design** as a code / QR (no personal text, no photos).
- **Templates gallery:** 20 starter designs.

**Tasks:**
- [ ] Per-block tap actions (explicit intents only; Android 14+ safe like `RowLaunchActivity`)
- [ ] Rule engine (pure function, unit-tested) + rule editor UI
- [ ] New blocks (each with S/M/L render test)
- [ ] Design share code + import preview
- [ ] 20 templates
- [ ] Layout schema `"v": 2` with migration from v1

**Acceptance:** tap targets work on Pixel and Nothing launchers; rules re-evaluate on the right
events (battery, time, calendar) without extra battery drain; v1 designs open unchanged.

---

## P15 — Complete Looks (one-tap full theme)

**Status:** Not started · **Effort:** M · **Area:** customisation

**What:** a gallery of complete home-screen looks — wallpaper + widget style (skin, frame, accent,
background) + icon pack suggestion + a recommended set of widgets. One tap applies the wallpaper and
style to all placed widgets; shows which widgets to add for the full look.

**Why:** Color Widgets (tens of millions of installs) is built around this. We already have Theme
Studio, theme packs, `WallpaperManagerHelper`, the Clay icon pack and setup sharing.

**Tasks:**
- [ ] Look format (reuse `SetupBundle`) + 12 launch looks
- [ ] Apply: wallpaper (home/lock/both), restyle all placed widgets, undo
- [ ] "Add missing widgets" checklist with pin buttons
- [ ] Before/after preview

**Acceptance:** apply + undo leaves every widget exactly as before undo; wallpaper never changed
without explicit confirmation.

---

## P16 — Languages and translated store listing

**Status:** Not started · **Effort:** M · **Area:** reach

**What:** move UI text to `strings.xml`, then translate to Hindi, Spanish, Portuguese (BR),
Indonesian and German; translate the Play listing for the same languages. Widget text (days,
months) already follows the phone's locale where formatters are used — audit the rest.

**Tasks:**
- [ ] Extract hard-coded UI strings (lint `HardcodedText` / Compose strings)
- [ ] Locale-aware dates/numbers in all renderers
- [ ] Translations + native-speaker review for Hindi
- [ ] Check long words don't break widget layouts (German)
- [ ] Translated store listing + screenshots

**Acceptance:** app fully usable in each language; no clipped text in widgets at any size.

---

## P17 — Accessibility pass

**Status:** Not started · **Effort:** S · **Area:** quality

**Tasks:**
- [ ] Every widget's image has a spoken description (e.g. "10:42, Thursday, battery 80%")
- [ ] All setup controls have labels; touch targets ≥ 48 dp
- [ ] Contrast check for light/dark and photo backgrounds
- [ ] Font scale 200% check in the app screens

**Acceptance:** TalkBack can read and operate the app and every widget.

---

## P18 — Integrations: RSS news and Todoist

**Status:** Not started · **Effort:** M · **Area:** productivity

**What:** an RSS/Atom news widget (user adds feeds; headlines in dot style) and a Todoist tasks
widget using the user's personal API token (no Google sign-in, no server). Both opt-in; network only
to the services the user chose; privacy policy updated.

**Tasks:**
- [ ] RSS parser + feed manager + widget (M, L)
- [ ] Todoist token entry (stored encrypted) + tasks widget with check-off
- [ ] Privacy policy + Data safety form update

---

## P19 — Wear OS tiles

**Status:** Not started · **Effort:** L · **Area:** platform

**What:** dot-style tiles for watch: time, battery (phone + watch), next event, weather.
Separate Wear module; data from the phone app via the Data Layer.

---

## P20 / P21 — Parked

- **P20 Social widgets** (friends' status, shared notes — Widgetable style): needs accounts and a
  server, which breaks the "no account, nothing leaves your phone" promise. Revisit only with a
  clear privacy design.
- **P21 "Describe your widget" (AI):** Nothing's approach; needs a paid AI API or a large on-device
  model and an account. Builder templates + rules (P14) cover most of the value.

---

## Release checklist (before next Play upload)

- [ ] Rerun device tests on emulator after the `WidgetE2ETest` fix (`connectedDebugAndroidTest`)
- [x] P5 lock-screen check done
- [ ] Design quality D0–D2 done (layout lint green, Large sizes, empty states) — see `docs/DESIGN_QUALITY_PLAN.md`
- [ ] `versionCode` → **5** (code 4 may already be used), `versionName` → next version
- [ ] Signed release AAB built with the upload key (never overwrite signing files)
- [ ] Store listing text + screenshots updated with new features (photo background, editor)
- [ ] Privacy policy still accurate (photos stay on device)
- [ ] Explicit owner approval before uploading / publishing

---

## Changelog

| Date       | Item                                                   | Commit    |
|------------|--------------------------------------------------------|-----------|
| 2026-11-12 | Background photo + Instagram-style placement editor    | `e3e5bd0` |
| 2026-11-12 | Android 14+ list-row fix, device widget tests          | `25431dc` |
| 2026-11-12 | Roadmap created                                        | —         |
| 2026-11-12 | P1 dot-matrix photo filter (renderer, cache, editor preview, Photo widgets, device speed test) | `3115810` |
| 2026-11-12 | P5 lock-screen widgets hardened + safety tests         | `28022f1` |
| 2026-11-12 | P2 custom widget builder end to end                    | `58d2b60` |
| 2026-11-12 | P4 Smart Stack automatic rotation                      | `9c6c823` |
| 2026-11-12 | P3 weather widgets: presentation layer, offline/stale states, tests | `e806168` |
| 2026-11-12 | P1–P5 installed on phone; 75/75 unit tests pass; awaiting on-phone checks before marking Done | — |

---

## Sources

- Nothing OS 4.1 / Relaxation Hub — [9to5Google](https://9to5google.com/2026/03/17/nothing-phone-4a-debuts-these-new-features-in-nothing-os-4-1/), [GSMArena](https://www.gsmarena.com/nothing_phone_4a-review-2948p4.php), [Gizmochina](https://www.gizmochina.com/2026/03/17/nothing-phone-4a-first-update-nothing-os-4-1-features/)
- Nothing OS 4 / Essential Apps — [Trusted Reviews](https://www.trustedreviews.com/news/nothing-os-4-is-rolling-out-heres-whats-new), [9to5Google hands-on](https://9to5google.com/2026/02/10/nothing-essential-apps-hands-on/), [Android Police](https://www.androidpolice.com/nothing-launches-a-vibe-coded-app-store/), [Nothing wiki](https://nothing.wiki/software/essential_apps_and_playground), [91mobiles](https://www.91mobiles.com/hub/nothing-os-4-roundup-release-timeline-eligible-phones-features/)
- Competitors — [N-Style Widgets (AppGoblin)](https://appgoblin.info/apps/com.nothingness.widgetpack), [Something KWGT (AppGoblin)](https://appgoblin.info/apps/nothing.kwgt.app), [Nothing Widgets (APKMirror)](https://www.apkmirror.com/apk/nothing-technology-limited/nothing-widgets/), [KWGT alternatives](https://alternativeto.net/software/kwgt-kustom-widget-maker)
- Glyph Matrix — [Android Authority](https://www.androidauthority.com/nothing-glyph-matrix-gimmick-3602057), [Glyph Museum 2.0](https://nothing.community/d/60931-glyph-museum-20-is-here)
- User demand — [Widget poll](https://androidauthority.com/favorite-android-widget-poll-results-3679420), [Pixel Launcher wishes](https://www.androidauthority.com/google-pixel-launcher-missing-features-2026-3686830/), [Remote Compose / streak template](https://androidauthority.com/android-widgets-overhaul-remote-compose-3670122/)
- Android 16 — [Widget metrics (developer.android.com)](https://developer.android.com/develop/ui/compose/glance/metrics.md.txt), [Lock-screen widgets (Droid Life)](https://www.droid-life.com/2025/08/20/android-16-qpr2-adds-lock-screen-widgets-to-phones-again/)
- Phase 2 competitors — [Color Widgets (AppGoblin)](https://appgoblin.info/apps/colorwidgets.ios.widget.topwidgets), [Material You Widgets](https://apppricinglab.com/app/google_play/com.appslab.materialyou.widgetspro), [Widgetable (AppGoblin)](https://appgoblin.info/apps/com.widgetable.theme.android), [Widgetable (App Store)](https://apps.apple.com/us/app/widgetable-besties-couples/id1641107226), [Chronus release notes (APKMirror)](https://www.apkmirror.com/apk/dvtonder/chronus-home-lock-widgets-wear-os/chronus-home-lock-widgets-wear-os-7-3-release/)

> Research note: Nothing OS coverage found is from spring 2026; check Nothing's release notes for
> later changes. Install counts come from third-party trackers, not Google Play.
