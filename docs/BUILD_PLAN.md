# Dot Widgets — Detailed Competitive Build Plan (phase-wise)

> **North star:** become the best Nothing-style widget app by **matching the table stakes first**,
> then **winning on widgets nobody else has**.
> **Tagline we build toward:** *"200+ designs — 60+ you won't find anywhere else. Sharp at every size. Made yours with advanced options. Honest widgets."*

This document is the deep version. Every phase lists **named tasks as cards**. Each card answers:
**What it is**, **Why it matters** (with competitor evidence), **How users use it**, and **How we build it**
(the concrete files, APIs and permissions in *this* repo), plus **Acceptance**.

Companion doc: `docs/UX_IMPLEMENTATION_PLAN.md` (Phases A–F, already largely implemented).

---

## How to read this document

- **Priority:** `P0` must ship · `P1` should ship · `P2` later.
- **Effort:** `S` < 1 day · `M` 1–3 days · `L` 3+ days.
- **"How we build it"** references real modules: `WidgetRegistry`, `WidgetDefinition`,
  `WidgetRenderEngine`, `RenderKit`, `DotMatrixRenderer`, `SizeResolver`, `WidgetSpec`,
  `WidgetSetupContent`, `Entitlements`, `PreferencesManager`, and the `data/*` repositories.
- **Terminology:**
  - *Widget* = one entry in `WidgetRegistry` (id, title, description, sizes, tap action, permission).
  - *Size bucket* = `S` (2×2), `M` (4×2), `L` (4×4) — `com.example.core.bitmap.SizeBucket`.
  - *Tap action* = `TapAction` (OPEN_CLOCK, OPEN_WEATHER, OPEN_CALENDAR, OPEN_BATTERY, OPEN_MAIN, DEFAULT).

---

## 1. Positioning — the three pillars

1. **Table stakes, done perfectly.** The widgets people actually use must exist at every size, look
   crisp, follow Material You, and support transparency. No excuses.
2. **Creative catalogue as the moat.** We ship novelty/creative dot widgets (flip, binary, word,
   lunar, chrono, crypto, snake, dice, ambient…) that no rival offers at this depth.
3. **Trust.** We removed unbacked performance claims, ask permissions only when a widget needs them,
   and keep all data on-device. We say only what we can prove.

**The promise users remember:** *more *interesting* widgets than anyone, that look sharp and don't lie.*

---

## 2. The competition — full profiles

Numbers below come from Play Store / APK listings and press coverage (2026). Prices are one-time
unless stated.

### 2.1 Official Nothing stack (baseline, not a rival to beat on count)

| App | Publisher | What it has | Why it matters to us |
|---|---|---|---|
| **Nothing Widgets** | NOTHING TECHNOLOGY LIMITED | Clocks, Weather, Quick Look, Photos, Compass; Lock screen + Always-On Display. ~4.8★, 4.37K reviews. Only on Nothing OS ≥2.0.3 | Sets the *visual expectation*. We must match the look on **any** Android phone and beat its widget count. |
| **Nothing Launcher** | NOTHING TECHNOLOGY LIMITED | Dot-matrix icons, monochrome widget templates, glyph widget row. Free, 10M+ downloads | Defines the aesthetic. Widgets ship *inside* the launcher — a reason many users never look elsewhere. |
| **Nothing Playground** | Nothing + community | AI-made "Essential Apps" widgets (Pulse clock, Big Eyes, Fidget spinner, Life in dots…). Needs Nothing OS 4.0+. Free | Free community supply is the real competition for *attention*. Our counter: pro-grade, reliable, offline widgets. |

### 2.2 Serious third-party rivals (the ones we must beat)

| App | Publisher | Designs | Price | Rating / reach | Material You | Wallpapers | Notes |
|---|---|---|---|---|---|---|---|
| **Everything Widgets** | JustNewDesigns | **400+** | IAP | 4.7★ / 11K reviews | ✅ | ✅ 100+ | Biggest catalogue, Snake game, Google/contact/quote/widget categories |
| **NewThing Widgets Pro** | AppsLab Co. | **150+** (one listing says 500+) | $0.99–1.99 | 4.5–4.6★ / 7.41K reviews | ✅ | ✅ | Exact alarms + foreground service, earbuds/quotes/location widgets |
| **Something OS Widgets** | Dita Cristian Ionut | **73** | Free + ~$1.50 unlock | 4.1★ / 438 reviews | ✅ | — | Transparency slider, folder widgets, Material You; reviewed by Pocket-lint |
| **N-Style / Justhing / ZeroThing Widgets: Dot Matrix** | Next Gen Studio | **40+** (listing says 80+) | $1.99 | 4.86★ / 40 reviews | ❌ | — | New (Jan 2026); 100–130 downloads; standalone dot pack |
| **Nothing Inspired Widgets** | Ninely Tech | **11 widget types** | $0.49 | ~1.8K installs | ❌ | — | Multi-ratio grid; on-device privacy angle; new (Sept 2026) |
| **Something KWGT – Nothing Style** | Space Dust | ~15–30 pack | $1.49 | 3.85★ / 10K+ | ❌ | — | **Requires KWGT Pro**; poor ratings ("money for nothing") |
| **Nothing KWGT: Nothing Widgets** | Droid Decor | ~20–40 pack | Free + KWGT Pro | 3.4★ / 50K+ | Partial | — | Same KWGT dependency trap |
| **Kiwi / androidwidgets.app** "Nothing" kit | Kasem | Growing (e.g. *Dot Bird* 4×2) | Free | — | — | — | Web-hosted single widgets, free |

**Patterns worth exploiting**
- **KWGT packs rate poorly** (3.4–3.85★) because of the extra install + Pro key. *Standalone is a feature* — we already are.
- **Everything/NewThing win on raw count but ship many variations of the same 10 staples.** They are wide, not deep. Our depth on *novelty* widgets is the wedge.
- **Nothing Inspired (11 widgets) is the quality-over-quantity play** and it's growing fast on privacy messaging — that's a warning that "honest + on-device" resonates.
- **Nothing beats us on Material You + transparency today — we don't have either.** That's Phase 0.

### 2.3 Where Dot Widgets stands today

- **91 designs / 134 size variants** across **11 categories**: Clock (14), Calendar (11), Weather (10),
  System (10), Battery (7), Toggles (9), Contacts (5), Media (4), Productivity (8), Photo (4), Fun (4).
- **Unique depth:** flip, word, binary, stacked, seconds-bar, chrono, crypto, year-progress, lunar,
  fitness rings, glyph battery, RAM/storage, dual capsule, ISO week, habit matrix, snake, dice, quote, ambient.
- **Technical edge:** real-size crisp rendering with a 1 MB Binder budget and dedicated 4×4 layouts.
- **Gaps:** no Material You, no transparency, no Compass, no hourly forecast, no real Contacts widget,
  no Search bar, no Folder, no lock-screen/AOD widgets.

---

## 3. Widget popularity tiers (build order)

Tier is assigned by **how universally competitors ship it** × **how often users actually place it**
(r/NothingTech, Play reviews, competitor feature lists).

| Tier | Widgets | Why this tier | Our status |
|---|---|---|---|
| **Tier 1** | Digital clock, Analog clock, Date/Calendar, Weather (now), Battery, Quick toggles, Now Playing, Steps, **Compass**, Photo frame | Every serious rival ships all of these; these are ~80% of real placements | ✅ except **Compass**, hourly forecast, real Contacts, Search, Folder |
| **Tier 2** | World clock, Forecast, Agenda, Screen time, Notes/To-Do, Contacts, Search, Folder, Astronomy (moon/sun), UV/rain | Common asks; strong retention | ✅ partial |
| **Tier 3** | Flip/word/binary/stacked clocks, Chrono, Crypto, Year, Lunar, Fitness rings, Glyph battery, RAM/Storage, Dual capsule, Snake, Dice, Quote, Ambient | Differentiators — few or no rivals have these | ✅ shipped, needs packaging |

**Rule:** finish all **Tier 1** widgets at S/M/L with Material You + transparency **before** polishing Tier 3.

---

## 4. Your current inventory (so we know what exists)

**Clock (14):** `clock.dotdigital.s/m/l`, `clock.analog.ring`, `clock.stacked.hhmm`, `clock.seconds.bar`,
`clock.binary`, `clock.world.london/tokyo/nyc`, `clock.flip.style`, `clock.text.words`,
`clock.seconds.live`, `clock.minimal.pill`.
**Calendar (11):** `calendar.day.date`, `calendar.month.grid`, `calendar.agenda.3`, `calendar.agenda.5`,
`calendar.week.progress`, `calendar.event.countdown`, `calendar.year.progress`, `calendar.iso.week`,
`calendar.minimal.split`, `calendar.timeline.dots`.
**Weather (10):** `weather.current.temp`, `weather.5day.strip`, `weather.sunrise.arc`,
`weather.city.london/nyc`, `weather.precipitation`, `weather.feels.humidity`, `weather.uv.index`, moon.
**Battery (7):** `battery.ring`, `battery.segmented`, `battery.combo.devices`, `battery.percentage`,
`battery.charging.active`, `battery.health.temp`.
**System (10):** `sensor.ram.meter`, `sensor.storage.bar`, `sensor.visualizer.eq`, `sensor.world`,
`sensor.year.progress`, `sensor.lunar.moon`, `sensor.fitness.rings`, `sensor.stopwatch.chrono`,
`sensor.battery.quad`.
**Toggles (9):** wifi, bluetooth, torch, dnd, airplane, and others.
**Contacts (5):** `shortcut.direct.dial`, `shortcut.direct.sms`, `shortcut.apps.4`, `shortcut.apps.6`, `shortcut.web.favorite`.
**Media (4):** `media.nowplaying.compact`, `media.nowplaying.art`, `media.controls.bar`, `media.volume.dots`.
**Productivity (8):** `prod.notes.pin`, `prod.todo.3`, `prod.todo.5`, `prod.step.counter`, `prod.water.tracker`,
`prod.focus.timer`, `prod.countdown.timer`, `sensor.fitness.rings`.
**Photo (4):** `photo.single.frame`, `photo.clock.overlay`, `photo.duo.collage`, `photo.monochrome.art`.
**Fun (4):** `fun.snake.dot`, `fun.quote.daily`, `fun.dice.roll`, `fun.pattern.generator`.

---

## Phase 0 — Cross-cutting foundations  *(P0 · M–L)*

### Why this phase first
Material You and transparency are **horizontal**: they change how *every* widget looks, so they must
land before we add more widgets. They are also the two features the fussiest rivals lead with
(Something OS Widgets, NewThing Pro, Everything Widgets). Doing them last would mean re-touching 91 widgets.

---

### 0.1 Material You dynamic color  *(P0 · M)*

**What it is.** On Android 12+, the system derives a colour palette from the user's wallpaper
("monet"). We read that palette and offer it as a first-class **accent option** ("Material You")
alongside the fixed accents (RED, etc.).

**Why it matters.** Three of the four serious rivals advertise it. It is the single most-requested
customisation feature and makes widgets feel native to the phone. Without it, we look like a toy pack.

**How users use it.** In setup (`WidgetSetupContent` → Accent row) they pick **Material You** instead of
a fixed colour. The widget immediately repaints to the wallpaper's dominant hue. Changing the wallpaper
re-tints placed widgets automatically.

**How we build it.**
- Read the palette: `WallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)` →
  `getPrimaryColor()` / `getSecondaryColor()` / `getTertiaryColor()`.
- Add a `AccentOption.MATERIAL_YOU` (or an `accentMode = DYNAMIC`) that resolves lazily at render time
  (do **not** bake a hex into `configJson`; store `accent = "material_you"` and resolve per render so it
  follows wallpaper changes).
- Resolve inside `WidgetRenderEngine` before building `WidgetSpec`, since `spec.accent.hex` is read by
  `RenderKit` and every renderer. Add a tiny `MaterialYouPalette.resolve(context): Int?` in
  `core/theme/`.
- Fall back to `AccentOption.RED` when the API is unavailable or the wallpaper has no colours.
- Declare `android.permission.READ_WALLPAPER_INTERNAL`? **No** — `getWallpaperColors` is public; do not
  add a dangerous permission.

**Acceptance.** A widget set to Material You matches the wallpaper's palette; changing the wallpaper
re-tints all placed widgets within one refresh.

---

### 0.2 Transparency control  *(P0 · M)*

**What it is.** A per-widget background transparency slider (0–100%) plus a dedicated **TRANSPARENT**
background mode where the dot content floats directly on the wallpaper.

**Why it matters.** The single most-liked community request for dot widgets is a **transparent dot clock**
(the top GitHub result for "Nothing dot matrix widget" exists *only* to make the background transparent).
Something OS Widgets and Ninely both ship transparency sliders.

**How users use it.** In setup (`Background` section) they drag transparency, or pick **Transparent**.
They see the wallpaper behind the dots. Great for matching a wallpaper and for OLED battery saving.

**How we build it.**
- `BackgroundMode` enum (in `core/theme/`) gains a `TRANSPARENT` value; add an `alpha: Int` field on
  `WidgetSpec` (0–255) and persist it in `configJson` (`bg_alpha`).
- `RenderKit.background()` / `DotMatrixRenderer.drawBackground()` already take `bgMode` + `isDark` +
  `accentColor`; add an `alpha` parameter and apply it to the fill (skip drawing the solid fill when
  alpha is 0; keep the dot colours opaque).
- `WidgetSetupContent` adds a `Slider(0f..1f)` bound to `WidgetSetupState.bgAlpha`.
- `WidgetSpec` gets `bgAlpha: Float`; propagate through `WidgetSetupState.toConfigJson()`/`fromInstance`.

**Acceptance.** Any widget can be set to fully transparent and blends into any wallpaper; the value
persists and survives edit/relaunch.

---

### 0.3 Theme/wallpaper propagation  *(P0 · S)*

**What it is.** A receiver that refreshes placed widgets when the system palette changes.

**Why it matters.** Material You is worthless if widgets keep the old colour until the user re-adds them.

**How we build it.** In the existing `com.example.widgets.base` package add a
`BroadcastReceiver` (or handle in `WidgetUpdateReceiver`) for `Intent.ACTION_WALLPAPER_CHANGED` and
`Intent.ACTION_CONFIGURATION_CHANGED` (`uiMode`) → call `WidgetRenderEngine.updateAllPlacedWidgets(context)`.
Register it in `AndroidManifest.xml`. Respect battery: debounce (max one refresh per few seconds).

**Acceptance.** Changing wallpaper or toggling dark mode repaints all placed widgets.

---

### 0.3b Update launcher previews  *(P0 · S)*

Regenerate `res/drawable/widget_preview_s/m/l` (and the snapshot PNGs) to show a Material You + transparent
variant so the picker reflects the new capability.

---

## Phase 1 — Tier 1 popular widgets: finish + fill gaps  *(P0 · M–L)*

> **Status: ✅ implemented.** 1.1 Compass (`sensor.compass.heading`), 1.2 Hourly forecast
> (`weather.hourly.strip`, Open-Meteo hourly added to `WeatherRepository`), 1.3 real Contacts
> (`ContactRepository` + in-app `ContactPickerDialog`, `READ_CONTACTS` permission), 1.4 Search bar
> (`prod.search.bar`, `TapAction.OPEN_SEARCH`) and Folder (`utility.folder.grid`, reusing the existing
> app-dock tap-region machinery so per-slot launch works without a separate activity) are in.
> Compass uses a fixed cadence refresh with no wakelock, per the open-decision recommendation.

### Why this phase
These are the widgets ~80% of users place. A missing Compass or hourly forecast is an instant
"uninstall, try NewThing" moment. We close **every** table-stakes gap here.

---

### 1.1 Compass  *(P0 · M)* — **the biggest single gap**

**What it is.** A live compass: a dot dial with N/S/E/W ticks and a signature **red north pointer**,
driven by the device orientation sensor.

**Why it matters.** **All four serious rivals list a Compass** (Everything, NewThing, Something OS,
Ninely). We are the only major pack without one. It's a top-3 "wow" widget in their screenshots.

**How users use it.** Place the 2×2 compass, hold the phone flat, and the needle tracks magnetic north.
Needs no permission; works offline.

**How we build it.**
- New renderer `CompassRenderers.renderCompass(spec, canvas, bearingDeg)` in `widgets/catalogue/`.
- Reuse `DotMatrixRenderer.drawDotRing` / angle helpers already used by `renderAnalogRing`.
- Watch `SensorManager.TYPE_ROTATION_VECTOR` (fallback `TYPE_ORIENTATION`). Because standard widgets
  can't animate at sensor rate, drive updates via a short-lived session or a `RemoteViewsService`
  (collection) — or update at a fixed cadence while the widget is visible; document the trade-off.
- Register in `WidgetRegistry` under `Category.SYSTEM`, sizes `S, M`, `TapAction.DEFAULT`.
- Graceful fallback message if no sensor: reuse `RenderKit.message(...)`.

**Acceptance.** Compass exists at S/M, needle points to north when the phone is held flat, degrades
gracefully on devices without a magnetometer.

---

### 1.2 Hourly forecast  *(P0 · M)*

**What it is.** Next 6–12 hours: hour label, weather icon, temperature, rain %.

**Why it matters.** Competitors ship "hourly forecast"; we only have a 5-day strip. It's the second
most-used weather widget after "current".

**How users use it.** Place the 4×2 → see today's curve at a glance; tap opens the weather sheet.

**How we build it.**
- Check `data/weather/WeatherRepository` — if it only exposes `daily`, extend it with `hourly`.
  If the backend lacks hourly, switch to **open-meteo** (`hourly=temperature_2m,precipitation_probability,weather_code`)
  which is keyless and free — keeps the on-device/privacy story.
- New renderer `WeatherRenderers.renderHourlyForecast(...)` reusing `DotMatrixRenderer.drawWeatherIcon`.
- Register `weather.hourly.strip` (Category.WEATHER, sizes M/L, `requiredPermission = Location`, `OPEN_WEATHER`).

**Acceptance.** Hourly widget shows 6–12 future hours with temp + icon + rain%, and respects the
stale-guard ("—" instead of wrong data) already used by `weather.current.temp`.

---

### 1.3 Contacts (real, with photos)  *(P0 · M)*

**What it is.** A picked contact with their photo (or a dot-styled monogram) and one-tap call/SMS.

**Why it matters.** Rivals list "favorite contacts / contact widgets". Our `shortcut.*` widgets are a
generic tray — no real contacts or photos.

**How users use it.** In setup, tap "Choose contact", pick from the phone book; the widget shows their
photo in a dot frame; tap calls them.

**How we build it.**
- New `AppPermission.Runtime` entry **READ_CONTACTS** in `PermissionCatalog` (with title/description in
  `strings.xml`), so the Phase-C inline banner logic already applies.
- Query `ContactsContract.CommonDataKinds.Phone.CONTENT_URI`; store the chosen `contactId` in `configJson`.
- Renderer `ContactRenderers.renderContact(...)`: circular photo crop + dot ring; fallback monogram when
  no photo. Use `ContactsContract.Contacts.openContactPhotoInputStream`.
- Tap actions: extend `TapAction` (or reuse `OPEN_MAIN` with an intent extra) to fire `ACTION_CALL` / `ACTION_SENDTO`.

**Acceptance.** User can bind a real contact; photo/initials render in dot style; tapping calls/messages;
permission asked in context with the Phase-C banner.

---

### 1.4 Search bar + Folder  *(P0 · M)*

**Search bar — what/why/how.** A slim 4×1/4×2 bar that fires system/Google search
(`Intent.ACTION_WEB_SEARCH`, or `ACTION_SEARCH` in-app). Reason: rivals list it; it's a common
home-screen fixture. Build: `SearchRenderers.renderSearchBar(...)` + `TapAction` that launches search.

**Folder — what/why/how.** Rivals ship "folder widgets". **Reality check:** a home-screen widget cannot
present a live launcher folder. Our approach: a dot-styled grid that **opens an in-app app-picker screen**
showing chosen apps as a modal folder. Document the limitation in the description so reviews stay honest.
- Build: `FolderRenderers` (dot grid of chosen app icons/letters) + a lightweight `FolderActivity` with
  a `LazyVerticalGrid` of chosen apps; store the app list in `configJson`.
- Do **not** use the `QUERY_ALL_PACKAGES` permission; use the launcher `queryIntentActivities` for
  `CATEGORY_LAUNCHER` with the `<queries>` manifest element scoped to MAIN/LAUNCHER.

**Acceptance.** Search widget opens the system search; Folder opens an in-app grid of chosen apps.

---

### 1.5 Tier-1 audit  *(P0 · M)*

Go widget-by-widget through Tier 1 (Digital clock, Analog, Date, Weather, Battery, Toggles, Now Playing,
Steps, Compass, Photo) and confirm each has **S/M/L (where sensible)**, **light/dark**, **Material You**,
**transparency**, and a meaningful `describe(context)` for TalkBack. Fix anything missing.

---

## Phase 2 — Tier 2 staples polish  *(P1 · M)*

> **Status: ✅ implemented.** 2.1 time-zone picker (`clock.world.custom` + `TimeZonePicker` UI, zone
> stored in configJson). 2.2 agenda/notes already have empty-state CTAs + permission banners. 2.3 weekly
> screen-time widget (`battery.screen.week`, 7-day chart). 2.4 hourly strip now folds rain risk + UV into
> one summary line.

### 2.1 World-clock set + timezone picker  *(P1 · S)*
**What/why.** We hard-code London/Tokyo/NYC. Add a **time zone picker** in setup (like the contact picker)
and register a generic `clock.world.custom` so any city works. **How.** Store the zone id in `configJson`;
`ClockRenderers.renderDigitalClock` already accepts `timeZoneId`, so this is mostly UI + registry wiring.

### 2.2 Agenda / Notes / To-Do polish  *(P1 · S)*
Verify L layouts (done in B4), add empty-state calls-to-action ("ADD TASKS IN THE APP" already exists),
and make sure the calendar permission banner appears in setup when missing.

### 2.3 Screen time & astronomy  *(P1 · S)*
Confirm `UsageAccess` banner + L layouts; add a Weekly screen-time variant (7 bars) to match competitor
"screen time" screenshots.

### 2.4 Weather extras  *(P1 · S)*
Surface rain-chance and UV inside the hourly set rather than as separate widgets, reducing catalogue noise.

---

## Phase 2b — Quality pass on Tier 1/2 (the "sharp at every size" claim)

> **Status: ✅ implemented.** `WidgetRenderSnapshotTest` gained a `tier 1 and 2 widgets render at real
> device sizes` test rendering 27 Tier 1/2 widgets at the launcher's real dp sizes (155², 329×155,
> 329² on the Phone 3a Pro); sheets written to `build/widget-snapshots/real/_sheet_real_*.png`.

**What.** A verification pass that every Tier 1/2 widget renders crisply at real device sizes.

**Why.** This is our differentiated claim — but only if it's actually true. Competitors just say "resizable".

**How.** Extend `WidgetRenderSnapshotTest` to render at the **Nothing Phone (3a) Pro's real widget sizes**
(from `adb shell dumpsys appwidget`) in addition to bucket defaults; eyeball the contact sheets; fix any
widget that uses < 3 u text or leaves > 20% empty area.

**Acceptance.** No blur, no letterboxing, no tiny text across Tier 1/2.

---

## Phase 3 — Differentiators: the moat  *(P0 · M–L)*

> **Status: ✅ implemented.** 3.1 "ONLY IN DOT WIDGETS" row + ★EXCLUSIVE card badge in `BrowserScreen`
> (`FEATURED_EXCLUSIVE`). 3.2 six brand-new exclusive widgets in `ExclusiveRenderers.kt`: Glyph-Reactive
> Clock, Life in Dots, Battery Fuel Gauge, Week in Dots, Focus Companion, Orbit Ambient. 3.3 wallpapers
> expanded to **112** palette-matched designs (7 accents × variants). 3.4 marketing sheets are produced by
> `WidgetRenderSnapshotTest` (`build/widget-snapshots/_sheet_*.png`).

### Why this phase
Once we match table stakes, the reason to choose *us over 400-widget rivals* is the widgets only we have.
This is where we win the marketing line.

---

### 3.1 Package the "unique set"  *(P0 · S)*

**What it is.** A curated, named collection in `BrowserScreen` ("Only in Dot Widgets") containing the
designs no rival ships: flip, word, binary, stacked, seconds-bar, chrono, crypto, year-progress, lunar,
fitness rings, glyph battery, RAM/storage, dual capsule, ISO week, habit matrix, snake, dice, quote, ambient.

**Why.** Makes the "60+ exclusive" claim scannable and true. Rivals bury everything in one grid.

**How users use it.** Open Browse → the "Only in Dot Widgets" row at the top → tap to add.

**How we build it.** A `FEATURED_EXCLUSIVE` id list (like the existing `RECOMMENDED`) rendered as a
horizontal row; add a badge on exclusive cards.

---

### 3.2 Add 5–10 brand-new "only us" widgets  *(P0 · L)*

Ideas, each impossible to find in rivals:
- **Glyph-reactive clock** — dot clock that pulses an accent ring the way Nothing's glyph lights do on charge/notification.
- **"Life in dots"** — a day rendered as a 1,440-dot grid (one dot per minute), filling as the day passes.
- **Animated dot art** — slow procedural animation (reuse `fun.pattern.generator` engine) with a clock overlay.
- **Battery "fuel gauge"** — horizontal dot gauge with time-to-full/empty from `BatteryManager`.
- **Week-in-dots** — 7 columns × 24 dots habit/screen-time heatmap.
- **Focus companion** — a focus timer that fills a dot ring plus a silent "streak" row.

**How we build it.** New functions in the relevant `*Renderers.kt`; register in `WidgetRegistry`;
reuse `RenderKit`/`DotMatrixRenderer`; where animation is needed, document the widget-refresh cadence
trade-off (the same constraint as the compass).

---

### 3.3 Bundle matching wallpapers  *(P1 · M)*

**What/why.** Rivals ship 100+ wallpapers. You have a `WallpapersScreen` already — turn it into a
selling point: 100+ dot-styled backgrounds **matched to the widget palette**.

**How.** Curate/generate dot-wallpapers (monochrome + Material You tints), ship as `assets/wallpapers/`
or a compressed pack; deep-link "Set wallpaper" via `WallpaperManager.setBitmap`, or hand off to the
system picker. Group by palette so users can match a widget instantly.

---

### 3.4 Marketing assets from snapshots  *(P0 · S)*

Use `WidgetRenderSnapshotTest`'s contract sheets to produce the Play Store screenshots + the "60+ exclusive"
collage. No extra design tooling needed.

---

## Phase 4 — Platform features  *(P1 · L)*

> **Status: ✅ implemented.** 4.1 lock-screen/AOD providers (`DotWidgetProviderLockS/M`, `keyguard`
> category, `lockSafe` flag + browser badge). 4.2 setup export/import as a compressed `DWSET1:` code
> (`SetupShareRepository`, share card in the Setups tab, imported configs pre-fill the config screen).
> 4.3 community feed bundled as `assets/community_setups.json` (`CommunityRepository`, "COMMUNITY
> PICKS" section). Tests: round-trip, malformed-code rejection, community asset parse, lock-safe set.

### 4.1 Lock screen / Always-On Display widgets  *(P1 · L)*

**What it is.** Widgets you can place on the Android 14+ lock screen (and AOD where supported).

**Why.** Only the **official** Nothing Widgets ships this. Matching it is a real parity win no third-party
rival currently has on non-Nothing phones.

**How users use it.** Long-press the lock screen → Add widget → Dot Widgets → pick a lock-safe widget
(clock, battery, date). Keep it simple: only S/M clock + battery + date widgets on the lock screen.

**How we build it.** Android 14 (API 34) lock-screen widgets use the same `AppWidgetProvider`; declare
which widgets are lock-safe via `android:widgetCategory="keyguard"` in the provider info XML (or the newer
lock-screen widget metadata). Because target is API 36, this is available. Gate with a feature check and
hide the option on older devices.

**Acceptance.** A clock and battery widget can be placed and render correctly on the lock screen.

---

### 4.2 Setup export / import  *(P1 · M)*

**What it is.** Share a whole home-screen setup (widget ids + configs + accent + background) as a short
code or a file, so a user can restore it or share it.

**Why.** Community sharing is the growth engine on r/NothingTech and Nothing Playground. Setup-sharing
turns users into distributors.

**How.** Serialize the placed `WidgetInstance` list (already in Room) + configs to JSON, encode to a
compact string/QR; an import screen re-inserts them (storing configs so widgets render on re-add).

---

### 4.3 In-app "Featured / Community" tab  *(P2 · M)*

A lightweight curated feed (bundled JSON, no backend) of setups + exclusive widgets. Keeps users in-app
and gives a home for setup-sharing.

---

## Phase 5 — Monetization  *(P1 · M)* — ⛔ DROPPED (product decision: app is fully free)

> **Status: ❌ dropped.** Dot Widgets ships **entirely free** — every widget, size and theme is
> unlocked for everyone. No Google Play Billing dependency, no paywall, no free-tier gating. This is
> an intentional positioning choice: it maximises adoption and review sentiment, and removes the
> single most common complaint rivals get ("asking money again" / restore bugs). `Entitlements`
> remains the one seam that Save consults (always returns `true`), so a paid tier could be added
> later by changing only that implementation. Sections 5.1–5.4 below are retained for reference only.

### Why this phase
`data/billing/Entitlements.kt` already gates Save (`hasFullAccess()` returns `true`). We only swap the
implementation. Pricing must undercut or match rivals' one-time $0.99–1.99 while feeling fair.

### 5.1 Wire Google Play Billing  *(P1 · M)*
- Add **Play Billing Library** (`com.android.billingclient:billing-ktx`), create products in Play Console.
- Implement `Entitlements.hasFullAccess(context)` using the billing client + a cached entitlement in
  `PreferencesManager`; verify purchases on `onPurchasesUpdated` and on app start (query purchases).
- Offline-safe: trust the cache until the next successful query.

### 5.2 Pricing  *(P1 · S)*
Offer **both**: a one-time "Lifetime Unlock" (price-match rivals, ~$1.99–2.99) **and** a small
subscription (~₹159/mo or $1.49/mo with a 7-day trial) for people who prefer low upfront. A/B the
default shown. Most rivals are one-time — a subscription is only worth it if we keep shipping.

### 5.3 Free tier  *(P1 · S)*
Free = a curated ~15-widget set (the Tier 1 staples + 2–3 exclusives as a taste). Full catalogue unlocks.
This maximises the free→paid funnel without a demo-only wall.

### 5.4 Paywall + restore  *(P1 · S)*
A dot-styled paywall shown at Save when `!hasFullAccess`, with a clear list of what unlocks, plus
**Restore purchases** and refund-friendly copy (rivals get bad reviews for "asking money again").

---

## Phase 6 — Growth & launch  *(P1 · M)*

### 6.1 Play listing rewrite  *(P1 · M)*
Lead with the positioning line. Screenshots (from Phase 3.4) ordered: exclusive widgets → Material You →
transparency → lock screen. 60-second demo video. Keywords: "Nothing widgets", "dot matrix", "Nothing OS
widgets", "glyph", "monochrome". Feature graphic = the dot collage.

### 6.2 Ratings prompt  *(P1 · S)*
Use Play In-App Review after the Nth successful widget add (not on first launch). Ratings are the single
strongest ranking input — rivals have 40–11K reviews; we start from zero.

### 6.3 Community seeding  *(P1 · S)*
Post on r/NothingTech, Nothing Community, and Nothing Playground with free promo codes. The current
top recommendation in those threads is NewThing — that's the specific app we're displacing.

### 6.4 Privacy-friendly analytics + crash reporting  *(P2 · S)*
Count widget adds (local, anonymised) and crashes (e.g. Play Console vitals or a lightweight SDK). No ad
or tracking SDKs — that contradicts the "honest/on-device" pillar.

---

## Phase 7 — Catalogue expansion (100+ new widgets)  *(P1 · L)*

> **Status: ⬜ planned.** Today the catalogue is **103 widgets** across 11 categories. Phase 7 adds
> **100+ new, genuinely distinct widgets** — no re-skinned duplicates — reaching **200+**. Depth comes
> from **new categories the rivals ignore** plus **rich variants** of the existing deep families.
> Every new widget ships with the **Phase 8 advanced options** baked in, so it isn't "another clock"
> but *a clock you can shape*.

### 7.0 Ground rules (apply to every widget below)

- **No clones.** A new id must render a *different* composition, data source, or interaction — not a
  colour swap. The snapshot contact sheets are the honesty check.
- **Data via repositories only.** Renderers read `data/*` (or a cheap local computation). No network
  in render paths; prefetch schedules belong in `WidgetDataPrefetch`.
- **Sizes chosen deliberately.** Each def declares only the buckets it truly fits (`S` 2×2, `M` 4×2,
  `L` 4×4); the crispness pass (2b) must pass at real device sizes.
- **Colour rules respected.** Headers/captions grey; values on-colour; **accent only for live state**;
  renderers use `spec.accentColor`, never `spec.accent.hex`.
- **Permissions minimal.** A new data source ⇒ a new `AppPermission` entry only if truly required, and
  the widget shows the tappable "needs access" prompt when denied.
- **Registration is one line.** All new defs go through `createDef(...)` in `WidgetRegistry.kt`.
- **Every widget gets at least two Phase 8 options** (e.g. layout variant + one behaviour toggle).

### 7.1 New categories

| Category | Why it's missing | New category enum | Target count |
|---|---|---|---|
| **Finance** | Nobody in the dot-widget niche ships a finance family; NewThing has only a crypto ticker. | `FINANCE` | ~12 |
| **Health & Fitness** | Steps exist, but no sleep/water/heart/workout depth. | `HEALTH` | ~12 |
| **Smart Home / IoT** | No rival binds to Home Assistant–style device state. | `SMART_HOME` | ~10 |
| **Sports & Live** | Zero live-score widgets anywhere in the niche. | `SPORTS` | ~10 |
| **Learning / Focus** | No study/pomodoro/reading-depth family. | `LEARNING` | ~10 |
| **Travel & World** | Only a London/NYC clock; no travel depth. | `TRAVEL` | ~10 |

Add the six enum values to `Category` in `WidgetSpec.kt` and icon entries (Material Symbols names) so
the browse chips (`BrowserScreen`) pick them up automatically.

### 7.2 Finance family (`FINANCE`, ~12)

| id | Title | Sizes | Data source | Advanced options |
|---|---|---|---|---|
| `finance.crypto.tick` | Crypto Ticker | S/M | `CryptoRepository` | coin pair, up/down colour rule |
| `finance.crypto.spark` | Crypto Sparkline | M | `CryptoRepository` (history) | coins, timeframe, fill style |
| `finance.crypto.portfolio` | Portfolio Value | M/L | user-entered holdings + `CryptoRepository` | holdings list, currency |
| `finance.stock.tick` | Stock Ticker | S/M | new `StockRepository` (cached) | symbol, market |
| `finance.stock.spark` | Stock Sparkline | M | `StockRepository` | symbol, range |
| `finance.currency.pair` | FX Rate Pair | M | new `FxRepository` | base/quote, inverse |
| `finance.currency.multi` | Multi-FX Board | L | `FxRepository` | up to 6 pairs |
| `finance.market.status` | Market Open/Closed | S | computed from exchange hours | exchange, holiday-aware |
| `finance.spend.daily` | Daily Spend Meter | M | user-entered budget | budget amount, reset cadence |
| `finance.budget.ring` | Budget Ring | S/M | user budget + spend | period, warn threshold (colour rule) |
| `finance.savings.goal` | Savings Goal Dots | M | user goal | goal amount, target date |
| `finance.payday.countdown` | Payday Countdown | S | user date | payday rule (weekly/monthly) |

New data: `data/finance/StockRepository.kt`, `data/finance/FxRepository.kt` (cache-backed like
`WeatherRepository`; prefetch in `WidgetDataPrefetch`; `requiresNetwork = true`).

### 7.3 Health & Fitness family (`HEALTH`, ~12)

| id | Title | Sizes | Data source | Advanced options |
|---|---|---|---|---|
| `health.steps.ring` | Steps Ring | S/M | `StepRepository` | goal, ring vs bar |
| `health.steps.week` | 7-Day Steps | M | `StepRepository` (history) | goal line on/off |
| `health.water.dots` | Hydration Dots | S/M | `PreferencesManager` water | glass size, goal |
| `health.sleep.last` | Last Night Sleep | M | new `SleepRepository` (Health Connect) | source, show stages |
| `health.sleep.week` | Sleep Week | M/L | `SleepRepository` | goal hours |
| `health.heart.rate` | Heart Rate | S | `SleepRepository`/Health Connect | resting vs live |
| `health.workout.streak` | Workout Streak | M | user habit log | weekly target |
| `health.calories.ring` | Calories Ring | S/M | Health Connect | goal, active/basal |
| `health.weight.trend` | Weight Trend | M | user entries | unit, goal weight |
| `health.meditation` | Mindfulness Minutes | S/M | Health Connect | daily goal |
| `health.stand.hours` | Stand Hours | M | Health Connect | goal |
| `health.body.battery` | Body Battery | M | derived (sleep+activity) | formula preset |

New data: `data/health/SleepRepository.kt` (Health Connect, gated behind a new
`AppPermission.HealthConnect` settings page). All degrade to a "connect Health" prompt when unavailable.

### 7.4 Smart Home / IoT family (`SMART_HOME`, ~10)

| id | Title | Sizes | Data source | Advanced options |
|---|---|---|---|---|
| `home.device.toggle` | Device Toggle | S | new `HomeAssistantRepository` (webhook) | entity id, on/off labels |
| `home.room.status` | Room Status Grid | M/L | `HomeAssistantRepository` | entities list |
| `home.lights.count` | Lights On Counter | S/M | `HomeAssistantRepository` | entities list |
| `home.thermostat` | Thermostat Dial | M | `HomeAssistantRepository` | entity, unit |
| `home.scene.button` | Scene Button | S | `HomeAssistantRepository` | scene id |
| `home.energy.today` | Energy Today | M | `HomeAssistantRepository` | entity, currency |
| `home.sensor.temp` | Room Temperature | S | `HomeAssistantRepository` | entity |
| `home.humidity` | Humidity Gauge | S | `HomeAssistantRepository` | entity |
| `home.door.status` | Door/Window Status | S/M | `HomeAssistantRepository` | entities list |
| `home.presence` | Presence Tracker | S | `HomeAssistantRepository` | person entities |

New data: `data/home/HomeAssistantRepository.kt` (optional base URL + token in prefs, REST GET/POST).
No SDK dependency — plain `HttpURLConnection` + cached last-known state, so it stays offline-honest.

### 7.5 Sports & Live family (`SPORTS`, ~10)

| id | Title | Sizes | Data source | Advanced options |
|---|---|---|---|---|
| `sports.score.live` | Live Score | M | new `SportsRepository` (cached API) | team/league |
| `sports.fixture.next` | Next Fixture | M | `SportsRepository` | team |
| `sports.table.mini` | Mini League Table | L | `SportsRepository` | league, rows |
| `sports.form.guide` | Form Guide (W/D/L) | M | `SportsRepository` | team |
| `sports.countdown.match` | Match Countdown | S | `SportsRepository` | team |
| `sports.f1.next.race` | Next F1 Race | M | `SportsRepository` | series |
| `sports.cricket.overs` | Cricket Overs | M | `SportsRepository` | match id |
| `sports.tennis.sets` | Tennis Sets | S/M | `SportsRepository` | match id |
| `sports.standings.dot` | Standings Dots | M | `SportsRepository` | league |
| `sports.highlights.link` | Highlights Link | S | tap-only (open app/URL) | URL |

New data: `data/sports/SportsRepository.kt` (pluggable provider; ships with a free cached source,
degrades to "no match today" cleanly).

### 7.6 Learning / Focus family (`LEARNING`, ~10)

| id | Title | Sizes | Data source | Advanced options |
|---|---|---|---|---|
| `learn.pomodoro.ring` | Pomodoro Ring | S/M | `WidgetStateManager` timer | 25/5 presets, long break |
| `learn.study.sessions` | Study Sessions Today | M | user log | daily target |
| `learn.flashcard.daily` | Word of the Day | M | bundled JSON wordlist | language, list |
| `learn.reading.goal` | Reading Goal Dots | M | user log | pages/day or mins |
| `learn.streak.days` | Learning Streak | S/M | user log | min streak |
| `learn.quiz.daily` | Daily Quiz Prompt | M | bundled JSON | category |
| `learn.focus.total` | Total Focus Time | M | `WidgetStateManager` | today/week |
| `learn.course.progress` | Course Progress | M | user entries | course name, modules |
| `learn.notes.last` | Last Note Snippet | S/M | `PreferencesManager` notes | show title/body |
| `learn.timer.exam` | Exam Countdown | S/M | user date | exam name |

New data: `assets/words.json`, `assets/quiz.json` (bundled, offline). Reuses `WidgetStateManager`.

### 7.7 Travel & World family (`TRAVEL`, ~10)

| id | Title | Sizes | Data source | Advanced options |
|---|---|---|---|---|
| `travel.clock.multi` | Multi-City Clock | M/L | time zones | up to 4 cities |
| `travel.world.single` | Single World City | S | time zones | city |
| `travel.weather.city` | Weather: Other City | M | `WeatherRepository` | city override |
| `travel.sun.times` | Sunrise/Sunset | S/M | `WeatherRepository` | location |
| `travel.flight.countdown` | Flight Countdown | M | user date/time | flight code |
| `travel.packing.check` | Packing Checklist | M/L | `PreferencesManager` | items list |
| `travel.trip.countdown` | Trip Countdown | S/M | user date | destination |
| `travel.currency.tip` | Travel FX Card | M | `FxRepository` | home/away currency |
| `travel.tz.diff` | Time-Zone Diff | S | time zones | home + away city |
| `travel.daylength` | Daylight Length | S | `WeatherRepository` | location |

### 7.8 Deep variants of existing families (~34)

Add **distinct** compositions (not colour swaps) to make each staple family feel deep. Each reuses an
existing renderer family with a new `renderX` method and Phase 8 options.

- **Clock (+8):** `clock.tally.marks` (hour tally), `clock.radial.24` (24-hour radial), `clock.week.arc`
  (week progress arc), `clock.dual.meridiem` (AM/PM split), `clock.iso.dots` (ISO week in dots),
  `clock.moon.time` (time + moon), `clock.hijri` (secondary calendar time), `clock.segment.largewaves`
  (segmented display).
- **Weather (+6):** `weather.wind.compass`, `weather.pressure.gauge`, `weather.visibility`,
  `weather.dewpoint`, `weather.aqi.dial`, `weather.precip.bar`.
- **Battery (+6):** `battery.temp.gauge`, `battery.cycles`, `battery.charge.eta` (time-to-full),
  `battery.drain.rate`, `battery.health.percent`, `battery.multi.devices`.
- **Calendar (+6):** `calendar.month.heat`, `calendar.week.numbers`, `calendar.birthdays`,
  `calendar.moon.month`, `calendar.quarter.progress`, `calendar.countdown.multi`.
- **Productivity (+4):** `prod.habit.matrix` (month heatmap), `prod.project.burndown`,
  `prod.meeting.count`, `prod.inbox.count`.
- **System/sensor (+6):** `sensor.cpu.load`, `sensor.ram.spark`, `sensor.net.updown`, `sensor.uptime`,
  `sensor.storage.free`, `sensor.net.speedtest.link`.

Counting: **64 new-category widgets + 40 deep variants = 104 new**, taking the catalogue from 103 to
**207**. Each is a distinct composition (new renderer method), verified on the snapshot contact sheets.

### 7.9 Registration & catalogue wiring

- Register every def in `WidgetRegistry.kt` via `createDef(...)` (one line each), grouped by family.
- `WidgetDescription.of(...)` gets a branch for each new family id prefix (Finance, Health, Home,
  Sports, Learning, Travel) so TalkBack is meaningful, not the title fallback.
- `BrowserScreen`: new chips appear automatically from `Category`; extend `RECOMMENDED` /
  `FEATURED_EXCLUSIVE` to surface the best new widgets.
- `ShortcutStore.WIDGETS` entries only when a widget needs slot binding (e.g. `home.device.toggle`).
- Update the tagline and wallpaper/description strings to reflect the new count.

### 7.10 Acceptance (Phase 7)

- `WidgetRegistry.getAll().size >= 200`, with ≥ 6 new categories present.
- `WordCount`/snapshot test renders **every** new def at every declared size, dark + light, no crash.
- Real-device-size pass (2b) green for all new widgets.
- Every new widget declares ≥ 2 Phase 8 options and they round-trip through config JSON.

---

## Phase 8 — Advanced options system  *(P1 · L)*

> **Status: ✅ done.** "Advanced" means the *same* widget can be reshaped without adding another
> catalogue entry. Phase 7 widgets ship with these; existing widgets gain them incrementally.
>
> **Landed:** `WidgetOption` (Toggle/Choice/Slider/Text/ColorRule) + `WidgetOptions.parse/write` under
> the `"opt"` config object; `WidgetSpec.options`/`WidgetDefinition.options`; generic `OptionControls`
> with collapsible ADVANCED section in `WidgetSetupContent`; `ColorRule` + `LayoutVariants` helpers.
> Six flagship widgets retrofitted (battery.ring, battery.segmented, prod.step.counter,
> prod.water.tracker, calendar.day.date, weather.current.temp). Tests: `WidgetOptionsTest` (6) cover
> round-trip, unknown-key preservation, deletion, malformed config, choice schema and `ColorRule`.
> **Deferred to 8.3.5:** BrowserScreen "Advanced options" filter/badge.

### 8.0 The option model

Extend the existing config pipeline (`WidgetSetupContent` ↔ `configJson`) with a **typed option
schema** so widgets declare their options and the setup UI renders controls generically.

```kotlin
// core/config/WidgetOption.kt (new)
sealed interface WidgetOption {
    val key: String
    val label: String
    val default: Any
    data class Toggle(...)   : WidgetOption   // on/off
    data class Choice(...)   : WidgetOption   // enum/radio
    data class Slider(...)   : WidgetOption   // int range
    data class Text(...)     : WidgetOption   // free text
    data class Slots(...)    : WidgetOption   // ordered list (already used by folders)
    data class PickList(...) : WidgetOption   // multi-select from a source (e.g. coins, entities)
    data class Color(...)    : WidgetOption   // accent override / rule colour
}
```

- `WidgetDefinition.options: List<WidgetOption>` (default empty). `createDef` gains an `options` param.
- `WidgetSpec` carries the parsed option values (`val options: Map<String, Any>` resolved from
  `configJson`) so renderers read `spec.option("goal", 10000)` — no ad-hoc JSON parsing in renderers.
- `WidgetSetupContent` renders the controls from `definition.options` (grouped section: **ADVANCED**).
- Round-trip is one code path: options serialize into the same `configJson` (keys under an `"opt"`
  object to avoid colliding with `accent`/`background`/etc.), so export/import (Phase 4.2) works free.

### 8.1 Option categories (what "advanced" includes)

1. **Layout variants** — `Choice` (e.g. `ring` / `bar` / `grid`) selecting a render branch.
2. **Data source / binding** — `PickList`/`Text` (coin pair, HA entity, city, calendar filter).
3. **Behaviour toggles** — `Toggle` (24h time, show goal line, animate, show label).
4. **Thresholds & goals** — `Slider` (step goal, budget, water glasses) driving both value and colour.
5. **Colour rules** — `Color` rule (e.g. "≥90% = accent", "below threshold = warn colour") applied via a
   shared `ColorRule` helper so accent-for-live-state stays consistent.
6. **Interactions** — `Toggle`/`Choice` for in-widget tap behaviour (increment counter vs open app).

### 8.2 Shared helpers (new)

- `core/config/WidgetOptions.kt` — schema types + `parse(configJson)` / `write(...)`.
- `core/render/ColorRule.kt` — `apply(value, goal, warnRatio, accentColor, warnColor)` used by every
  goal/threshold widget so the semantics are identical everywhere.
- `core/render/LayoutVariants.kt` — common helpers (`ringOrBar`, `rowsOrColumns`) to keep renderers thin.
- `ui/config/OptionControls.kt` — generic Compose controls for each `WidgetOption` type, themed
  dot-style, reusing `TagPill`/existing setup widgets.

### 8.3 Rollout order

1. Land `WidgetOption` + `WidgetOptions.parse/write` + `WidgetSpec.options` (**no behaviour change**).
2. Generic `OptionControls` + **ADVANCED** section in `WidgetSetupContent`.
3. Retrofit 6 flagship existing widgets (digital clock, battery ring, steps, weather current, calendar
   agenda, water) to prove the pipeline.
4. Ship Phase 7 widgets with options from day one.
5. `BrowserScreen`: add an **"Advanced options"** filter/badge for widgets with ≥ 2 options.

### 8.4 Acceptance (Phase 8)

- A widget's option values survive: set → save → reopen setup (pre-filled) → export/import (Phase 4.2).
- Unit test: every `WidgetOption` type round-trips; unknown option keys are preserved (forward-compat).
- Every flagship + Phase 7 widget exposes ≥ 2 options; renderers read only via `spec.option(...)`.
- No renderer parses option JSON ad-hoc (grep guard in the test suite).

### 8.5 Risks & mitigations

| Risk | Mitigation |
|---|---|
| Option sprawl → setup screen clutter | Group under a collapsed **ADVANCED** section; ≤ 6 controls per widget in v1. |
| Health/FX/HA/Sports need network or a service | Cache last-known value; degrade to a clean empty state; never block the bitmap render. |
| More widgets → Binder/time budget | Reuse `SizeResolver` budget; keep expensive data in `WidgetDataPrefetch`, not render. |
| Catalogue bloat hurts browse UX | Category chips + search + "Advanced" badge; keep `RECOMMENDED` curated at ~12. |
| New permissions dent trust | One permission per source, asked lazily, with the existing tappable prompt. |

---

## 5. Dependency order

```
Phase 0 (foundations) → Phase 1 (Tier 1 gaps) → Phase 2 (Tier 2 polish) → Phase 2b (crispness pass)
        └────────────→ Phase 3 (moat) → Phase 4 (lock screen, sharing)
Phase 1 → Phase 5 (billing, dropped) → Phase 6 (launch)
Phase 6/4 → Phase 8 (option model) → Phase 7 (100+ new widgets) → re-run 2b crispness pass
```

**Recommended sequence:** `0 → 1 → 3 → 2 → 2b → 5 → 4 → 6 → 8 → 7`.
Reason: foundations (0) + gaps (1) make us *competitive*; the moat (3) makes us *choosable*; billing (5)
is dropped; launch (6) is mostly non-code. **Phase 8 (option model) lands before Phase 7** so every new
widget is born with advanced options instead of being retrofitted, and Phase 2b is re-run after 7 to keep
the "sharp at every size" guarantee across the grown catalogue.

---

## 6. Acceptance gates

| After phase | Gate |
|---|---|
| 0 | Any widget can be set to Material You + 0% opacity; wallpaper change re-tints placed widgets. |
| 1 | Compass, hourly forecast, real Contacts, Search, Folder exist at required sizes; Tier 1 all have S/M/L + Material You + transparency. |
| 2 / 2b | No blur, letterboxing, or < 3 u text across Tier 1/2 at real device sizes. |
| 3 | 20+ widgets no rival ships, screenshot-backed; 100+ matching wallpapers. |
| 4 | A clock + battery render on the lock screen; setups export/import. |
| 5 | Free users add curated set; paid unlock all; purchases restore. |
| 6 | Listing live; ratings prompt firing; community posts seeded. |
| 7 | Catalogue ≥ 200 widgets across ≥ 6 new categories; every new widget has advanced options. |
| 8 | Advanced options round-trip through config JSON; every option has a test; browse UI filters by option. |

---

## 7. Open decisions (confirm before building)

| # | Question | Decision |
|---|---|---|
| 1 | Pricing model | ✅ **Fully free — no billing.** Every widget/size/theme unlocked; no Play Billing dependency |
| 2 | Material You default | **Default ON** for Android 12+, user can switch to a fixed accent |
| 3 | Free tier size | ✅ **N/A** — everything is free (was ~15 widgets under the old freemium plan) |
| 4 | Lock-screen scope | **Clock + battery + date only** to start |
| 5 | Compass update cadence | Fixed cadence refresh (document the animation trade-off) — no wakelock |
| 6 | Expansion size | ✅ **100+ new widgets → 200+ total**, across 6 new categories (Finance, Health, Smart Home, Sports, Learning, Travel) |
| 7 | Advanced options scope | **Typed option schema** (layout / binding / toggles / goals / colour rules / interaction) parsed into `WidgetSpec.options`; ≥ 2 options per new widget |
| 8 | 3rd-party integrations | **Opt-in, cached, offline-honest** — Health Connect (sleep/heart/calories) and Home Assistant (webhook/REST) only; no SDKs, no ads, no tracking |
| 9 | New-network families hosting | **Keep app free; no backend we host.** Finance/Stock/FX/Sports use free cached public sources + graceful empty states |

---

## 8. Appendix — technical reference for implementers

**Permission model.** `ui/permissions/PermissionCatalog.kt` defines `AppPermission` as a sealed class
(`Runtime`, `SettingsPage`) with `isGranted`, `isApplicable`, `intent`, `permissions`, `title`,
`description`. `WidgetDefinition.requiredPermission` binds a widget to one; `WidgetRenderEngine` renders
a tappable "needs access" message with an `open_permission` extra that `MainActivity` routes to the
right dialog/settings page. Phase 1 adds `READ_CONTACTS` (`Runtime`) here.

**Rendering stack.** `WidgetRenderEngine` → `SizeResolver.resolveFromOptions()` (real px + 1 MB Binder
budget) → `WidgetSpec` → `definition.render(...)` → `RenderKit` / `DotMatrixRenderer` →
`RemoteViews` with `setContentDescription(definition.describe(context))`.

**Colour rules (from `RenderKit`).** headers/captions = grey (`0xFF8A8A8A` dark / `0xFF6A6A6A` light);
primary values = on-colour (white/black); **accent only for live state** (running, charging, now, today,
warnings); inactive dots = off-colour.

**Data repositories (`data/*`).** `BatteryHelper.getBatteryStatus`, `WeatherRepository.getWeatherInstant`
(cache-backed), `MediaRepository.nowPlaying`, `StepRepository.todaySteps`, `ScreenTimeRepository`,
`BluetoothBatteryRepository.connectedDevices`, `CryptoRepository`, `CalendarRepository`, `DeviceToggles`.
Use these; don't add network calls in render paths.

**Key files by area.**
- Registry/definition: `widgets/base/WidgetRegistry.kt`, `WidgetDefinition.kt`
- Engine: `widgets/base/WidgetRenderEngine.kt`, `widgets/base/WidgetUpdateReceiver.kt`
- Render core: `core/render/RenderKit.kt`, `DotMatrixRenderer.kt`, `core/bitmap/SizeResolver.kt`
- Catalogue: `widgets/catalogue/*Renderers.kt`
- Setup UI: `ui/config/WidgetSetupContent.kt`, `WidgetConfigureActivity.kt`, `WidgetConfigSheet.kt`
- Browse/settings: `ui/browse/BrowserScreen.kt`, `ui/settings/SettingsScreen.kt`
- Prefs/billing: `data/prefs/PreferencesManager.kt`, `data/billing/Entitlements.kt`
- Tests: `app/src/test/.../WidgetRenderSnapshotTest.kt` (dark + light contact sheets)

**Build/verify commands** (Windows PowerShell):
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$gradleExe="C:\Users\parma\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat"
& $gradleExe :app:assembleDebug --offline
& $gradleExe :app:testDebugUnitTest --offline   # writes build/widget-snapshots
```
