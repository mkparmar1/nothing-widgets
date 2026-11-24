# New Widgets & Styles — Development Plan

Source: the 7 mockup sheets in `docs/new_widgets/` (~350 tiles).
Goal: add every mockup design that is buildable, **without changing or removing any existing widget**.

- ~150 tiles are designs of widgets the app **already has** → they become extra **Styles** of that widget.
- ~64 tiles are widgets the app **does not have** → they become **new widgets** (each also gets styles).
- The rest are duplicates across sheets, or not buildable (see "Out of scope").

Work ships in **4 batches of 50** (a "unit" = one new style on an existing widget, or one new widget),
after a framework phase, and ends with a verification phase.

---

## Ground rules (no existing feature is touched)

1. **Classic stays the default.** Every widget that gets styles gains a `Style` option
   (`WidgetOption.Choice`, key `design`, default `classic`). `classic` calls the *original* renderer,
   unchanged. Already-placed widgets have no `design` value → they render exactly as today.
2. **Additive code only.** New styles and widgets live in new files:
   - `widgets/styles/StyleCatalog.kt` — widget id → list of extra styles
   - `widgets/styles/StyledDefinition.kt` — wrapper that adds the option and dispatches
   - `widgets/styles/kit/SoftCardKit.kt`, `BoldTileKit.kt`, `StyleIcons.kt` — drawing helpers
   - `widgets/styles/*Styles.kt` — one file per category of styles
   - `widgets/newwave/NewWidgetsRegistry.kt` + `widgets/newwave/*Renderers.kt` — new widgets
   - new small repositories under `data/newwave/` when a new widget needs its own data
3. **Only touch points** in existing code:
   - `WidgetRegistry.register()` → wraps the definition through `StyleCatalog.decorate(def)`
     and `init` calls `NewWidgetsRegistry.registerAll(::register)` (one line each).
   - `WidgetDetailsScreen` → new "Style" picker row (live thumbnails), selection used for preview & pin.
   - `OptionControls` → hides the raw `design` chip row when the visual picker is shown.
   - `BrowserScreen` → "N styles" badge.
4. **Existing ids, categories, options, data stores, skins and Looks are not renamed or changed.**
5. **Safety test:** `StyleClassicParityTest` renders every widget with no `design` value and with
   `design=classic` and asserts the bitmaps are identical.
6. **Coverage:** `WidgetRenderSnapshotTest` already renders every registered widget; the new
   `StyleSnapshotTest` renders **every style** of every widget at every size in light + dark.
7. One commit per batch (or per category inside a batch) so any part can be reverted alone.

## Design families (from the mockups)

| Family | Look | Used by |
|---|---|---|
| **Classic** | Existing dot-matrix design (unchanged) | default for every existing widget |
| **Soft Card** | Rounded card, title-case label top-left, red status dot top-right (the existing corner accent dot), solid black glyph icons, large clean numerals (Geist), pill progress bars. Follows light/dark theme. | sheets 4–7 |
| **Bold Tile** | High-contrast solid tile (black in both themes), white numerals, accent highlights (green ring, red dot), glyph icons. | sheets 1–3 |
| **Special layouts** | Distinct layouts shown in the sheets (vinyl record, hourglass, arc gauge, analog trio, dotted glass…) | per widget |

All families still respect: background mode (solid/transparent/scrim), opacity, shape, accent colour,
text-size and corner-dot universal options.

## Style picker UX

- **Widget details screen**: a horizontal "Style" strip of live thumbnails (Classic first). Tapping a
  style updates the big preview (all sizes) and is used by "Add to Home Screen" and "Customize".
- **Setup / Edit placed widget**: the same `design` option appears in ADVANCED (chips), so the style can
  be changed at any time after placing.
- **Browse grid**: badge "N styles" on widgets with more than one style.

---

## Phase 0 — Style framework (no visible widget changes)

- [ ] `StyleCatalog` + `StyledDefinition` wrapper + hook in `WidgetRegistry.register()`
- [ ] `SoftCardKit`, `BoldTileKit`, `StyleIcons` (vector glyphs drawn in code)
- [ ] Details-screen Style picker, browse badge, ADVANCED `design` option
- [ ] `StyleClassicParityTest`, `StyleSnapshotTest`

## Batch 1 — 50 styles: Clock, Calendar, Weather, Battery

| # | Existing widget | New style |
|---|---|---|
| 1 | `clock.dotdigital.m` | Bold Time — "10:45 · Tue, 18 Nov" |
| 2 | `clock.dotdigital.s` | Bold Tile |
| 3 | `clock.stacked.hhmm` | Bold Pill — stacked 10/45 with red dot |
| 4 | `clock.stacked.hhmm` | Soft Stack |
| 5 | `clock.analog.ring` | Classic Analog — white face, red seconds |
| 6 | `clock.analog.ring` | Classic Analog — black face |
| 7 | `clock.analog.ring` | Hybrid Dial — tick bezel + digital time |
| 8 | `clock.world.custom` | Soft City — "New York 12:15 AM GMT-5" |
| 9 | `clock.world.london` | Soft City |
| 10 | `clock.world.tokyo` | Soft City |
| 11 | `clock.world.nyc` | Soft City |
| 12 | `travel.clock.multi` | Analog Trio — Tokyo / London / Sydney dials |
| 13 | `calendar.day.date` | Bold Date — "TUE / 18 / Nov" |
| 14 | `calendar.day.date` | Stacked Dot Date |
| 15 | `calendar.minimal.split` | Soft Card |
| 16 | `calendar.month.grid` | Soft Month — red today |
| 17 | `calendar.week.progress` | Week Pill — M T **W** T F S S |
| 18 | `calendar.agenda.3` | Soft Agenda — "● Today · Team Meeting 10:30" |
| 19 | `calendar.agenda.3` | Timeline Bar |
| 20 | `calendar.countdown.date` | Hourglass — "Vacation in 12 days" |
| 21 | `travel.trip.countdown` | Soft Countdown |
| 22 | `weather.current.temp` | Bold Cloud — "24° Partly Cloudy · Ahmedabad" |
| 23 | `weather.current.temp` | Soft Sun — "24°C Feels like 26°" |
| 24 | `weather.current.temp` | Soft City — "24° Ahmedabad →" |
| 25 | `weather.5day.strip` | Soft Days — Wed…Sat |
| 26 | `weather.5day.strip` | Bold Clouds — Mon…Thu |
| 27 | `weather.hourly.strip` | Soft Hourly — 12PM…9PM |
| 28 | `weather.uv.index` | Soft Gauge |
| 29 | `weather.aqi.dial` | Soft Arc |
| 30 | `weather.air.quality` | Soft Arc |
| 31 | `weather.pressure.gauge` | Soft Gauge |
| 32 | `weather.wind.compass` | Soft Dial |
| 33 | `weather.sunrise.arc` | Soft Sun Times — "07:00 · 18:14" |
| 34 | `travel.sun.times` | Soft Sun Times |
| 35 | `weather.moon.phase` | Bold Moon — "Waxing Gibbous 78%" |
| 36 | `weather.moon.phase` | Soft Moon |
| 37 | `weather.feels.humidity` | Thermometer — "26°C Feels like 28°" |
| 38 | `weather.golden.hour` | Soft Card |
| 39 | `battery.ring` | Bold Ring — green ring "78% Battery" |
| 40 | `battery.ring` | Soft Ring |
| 41 | `battery.percentage` | Battery Cell (vertical) |
| 42 | `battery.minimal.pill` | Soft Pill |
| 43 | `battery.fuel.gauge` | Time Left — "78% · 2h 45m left" |
| 44 | `battery.charge.eta` | Time Left |
| 45 | `battery.charging.active` | Bolt Circle |
| 46 | `battery.segmented` | Battery Cell |
| 47 | `battery.multi.devices` | Bluetooth Pill Bar |
| 48 | `battery.combo.devices` | Soft Rows |
| 49 | `battery.temp.gauge` | Bold Temp — "35°C" |
| 50 | `battery.health.percent` | Soft Card |

## Batch 2 — 50 styles: System, Toggles, Media, Productivity

| # | Existing widget | New style |
|---|---|---|
| 1 | `sensor.ram.meter` | Soft Bar — "RAM 72%" |
| 2 | `sensor.ram.meter` | RAM Rows (3-row card) |
| 3 | `sensor.ram.spark` | Soft Bar |
| 4 | `sensor.storage.bar` | Soft Bar — "Storage 64%" |
| 5 | `sensor.storage.bar` | Soft Ring — "64% Storage" |
| 6 | `sensor.storage.free` | Soft Ring |
| 7 | `sensor.cpu.load` | CPU/GPU Bars |
| 8 | `sensor.cpu.load` | Frequency Bars |
| 9 | `sys.network.speed` | Speed-Test Card |
| 10 | `sensor.uptime` | Soft Uptime — "00:15:30" |
| 11 | `sensor.compass.heading` | Soft Compass |
| 12 | `sensor.compass.heading` | Bold Compass — red needle |
| 13 | `sys.mobile.data` | Data Cap — "4.5 GB of 10 GB" |
| 14 | `sys.thermal.uptime` | Bold Temp |
| 15 | `toggle.grid.2x2` | Circle Row (4 black circles) |
| 16 | `toggle.grid.2x2` | Circle Grid (2×2) |
| 17 | `toggle.wifi` | Soft Icon |
| 18 | `toggle.wifi` | Bold Circle |
| 19 | `toggle.bluetooth` | Soft Icon |
| 20 | `toggle.bluetooth` | Bold Circle |
| 21 | `toggle.torch` | Bold Circle |
| 22 | `toggle.dnd` | Moon Pill — "Do Not Disturb" |
| 23 | `toggle.airplane` | Switch Pill — "Airplane Mode" |
| 24 | `toggle.volume.steps` | Slider Pill |
| 25 | `media.volume.dots` | Slider Pill |
| 26 | `toggle.hotspot` | Soft Icon |
| 27 | `media.nowplaying.art` | Art Card — "Better Days · Chill Vibes" |
| 28 | `media.nowplaying.art` | Vinyl |
| 29 | `media.nowplaying.compact` | Progress Card — "Good Vibes" |
| 30 | `media.controls.bar` | Vertical Controls |
| 31 | `sensor.visualizer.eq` | Equalizer Bars |
| 32 | `sensor.visualizer.eq` | Soft Wave |
| 33 | `prod.search.bar` | Search Pill |
| 34 | `prod.search.bar` | Search + App Chips |
| 35 | `prod.todo.3` | Soft To-Do (red dot) |
| 36 | `prod.todo.3` | Daily Goals |
| 37 | `prod.todo.5` | Soft To-Do |
| 38 | `prod.notes.pin` | Soft Note — "Notes · Stay Focused…" |
| 39 | `prod.focus.timer` | Dotted Ring — "10:45 · 3 min" |
| 40 | `learn.pomodoro.ring` | Dotted Ring |
| 41 | `prod.timer.presets` | Stopwatch Card — "00:25:00 Timer" |
| 42 | `sensor.stopwatch.chrono` | Stopwatch Card |
| 43 | `prod.countdown.timer` | Stopwatch Card |
| 44 | `prod.habit.dots` | Checkbox List |
| 45 | `prod.habit.dots` | Streak Bars |
| 46 | `prod.step.counter` | Steps Bars — "7,850 Steps" |
| 47 | `prod.step.counter` | Steps Ring — shoe icon |
| 48 | `health.steps.ring` | Steps Ring |
| 49 | `prod.water.tracker` | Dotted Glass |
| 50 | `prod.inbox.count` | Envelope — "1 unread" |

## Batch 3 — 36 styles + 14 new widgets

Styles (Finance, Health, Fun, Photo, Shortcuts, Smart Home, Travel)

| # | Existing widget | New style |
|---|---|---|
| 1 | `finance.crypto.spark` | Soft Coin — "BTC $87,420 +2.12%" |
| 2 | `finance.crypto.spark` | Bold Coin |
| 3 | `finance.crypto.tick` | Soft Coin |
| 4 | `finance.stock.spark` | Bold Stock — "AAPL $228.45" |
| 5 | `finance.stock.spark` | Soft Stock |
| 6 | `finance.stock.tick` | Soft Stock |
| 7 | `finance.currency.pair` | Converter — "1 USD = ₹87.45 ⟳" |
| 8 | `finance.crypto.portfolio` | Portfolio Value |
| 9 | `finance.crypto.portfolio` | Crypto Wallet |
| 10 | `finance.currency.multi` | Soft Rows |
| 11 | `health.heart.rate` | Red Line — "72 bpm" |
| 12 | `health.heart.rate` | Dotted Heart Ring |
| 13 | `health.heart.rate` | Bold BPM |
| 14 | `health.calories.ring` | Arc Gauge — "360 Cal" |
| 15 | `health.calories.ring` | Flame — "320 Cal" |
| 16 | `health.sleep.week` | Quality Bars |
| 17 | `health.sleep.last` | Moon Card — "7h 20m Sleep" |
| 18 | `health.meditation` | Yoga Figure |
| 19 | `health.meditation` | Breath Wave |
| 20 | `health.mood.log` | Faces Row |
| 21 | `health.water.dots` | Dotted Glass |
| 22 | `fun.quote.daily` | Bold Quote |
| 23 | `fun.quote.daily` | Soft Quote |
| 24 | `fun.dice.roll` | Three Dice |
| 25 | `fun.dice.roll` | Soft Dice |
| 26 | `fun.snake.dot` | Mini-games Card |
| 27 | `photo.single.frame` | Rounded Landscape |
| 28 | `shortcut.apps.4` | Bold Grid |
| 29 | `shortcut.apps.4` | Soft Grid |
| 30 | `shortcut.apps.6` | Soft Grid |
| 31 | `home.thermostat` | Slider |
| 32 | `home.device.toggle` | Smart Bulb |
| 33 | `home.energy.today` | Power Gauge |
| 34 | `home.lights.count` | Soft Card |
| 35 | `travel.flight.countdown` | Boarding Card — "6E-532 · DEL → BOM" |
| 36 | `prod.tally.counter` | Soft Counter |

New widgets

| # | New widget | Category |
|---|---|---|
| 37 | Ring Bezel Clock | Clock |
| 38 | Dot Orbit Seconds | Clock |
| 39 | Hourglass Day | Calendar |
| 40 | Greeting Card ("Good Morning") | Clock |
| 41 | IP Address | Sensors & System |
| 42 | VPN Status | Sensors & System |
| 43 | Router Status | Sensors & System |
| 44 | Ping & Packet Loss | Sensors & System |
| 45 | Cell Signal (dBm) | Sensors & System |
| 46 | RAM + Storage Twin Rings | Sensors & System |
| 47 | QR Code (your text/link) | Shortcuts |
| 48 | Wi-Fi Share QR | Shortcuts |
| 49 | Low Power tile | Toggles |
| 50 | Storage Cleaner tile | Toggles |

## Batch 4 — 50 new widgets

| # | New widget | Category |
|---|---|---|
| 1 | NFC tile | Toggles |
| 2 | Cast Screen tile | Toggles |
| 3 | Silent / Ringer toggle | Toggles |
| 4 | Brightness slider | Toggles |
| 5 | Calculator keypad | Productivity |
| 6 | Metronome | Productivity |
| 7 | Quick Soundboard | Fun & Lab |
| 8 | Voice Note tile | Shortcuts |
| 9 | Single App tile | Shortcuts |
| 10 | 9-App Grid | Shortcuts |
| 11 | Appliance Hub | Smart Home |
| 12 | Light Zones | Smart Home |
| 13 | Fan Speed dial | Smart Home |
| 14 | Front Door Camera | Smart Home |
| 15 | Smart Lock | Smart Home |
| 16 | Garage Door | Smart Home |
| 17 | Water Leak Alarm | Smart Home |
| 18 | Active Minutes ring | Health |
| 19 | Stretch Timer | Health |
| 20 | Posture Check | Health |
| 21 | Eye-Strain Break | Health |
| 22 | Breathing Pace | Health |
| 23 | Medication Tracker | Health |
| 24 | HRV (manual log) | Health |
| 25 | Workout Reps | Health |
| 26 | Journal Prompt | Productivity |
| 27 | Priority Matrix (2×2) | Productivity |
| 28 | Daily Challenge | Productivity |
| 29 | Stock Watchlist | Finance |
| 30 | Expense Categories | Finance |
| 31 | Altitude & Elevation | Travel |
| 32 | GPS Coordinates | Travel |
| 33 | Dot World Map | Travel |
| 34 | Distance to Place | Travel |
| 35 | Dot Text Banner | Fun & Lab |
| 36 | Virtual Pet | Fun & Lab |
| 37 | Retro Brick | Fun & Lab |
| 38 | Pixel Invader | Fun & Lab |
| 39 | Game On tile | Fun & Lab |
| 40 | Coin Flipper | Fun & Lab |
| 41 | Daily Trivia | Learning |
| 42 | Cassette | Media |
| 43 | Rotating Cube | Fun & Lab |
| 44 | Constellation | Fun & Lab |
| 45 | Dot Radar | Fun & Lab |
| 46 | Coffee Counter | Health |
| 47 | Mood Board (6 photos) | Photo |
| 48 | Colour of the Day | Fun & Lab |
| 49 | Pixel Heart | Fun & Lab |
| 50 | Smiley Mood | Health |

## Phase 5 — Finish

- [ ] Soft Card / Bold Tile style for each new widget where both look good
- [ ] Full build + unit + snapshot tests, light and dark, tablet and large-font checks
- [ ] README widget/style counts

---

## Out of scope (not buildable or not allowed)

| Tile | Reason |
|---|---|
| Gas fees, NFT floor, Stake APY, Transaction status, Network difficulty | Need keyed/paid APIs (app rule: keyless HTTPS only) |
| Live flight status, Transit route delay | Same |
| Background-apps killer, Hotspot client count, Read/write disk speed, Noise-cancellation control | Not permitted by Android / Play policy |
| Noise level (dB), BPM analyzer, Audio tuner, Live dictation | Need a live microphone; widgets cannot run one |
| Sleep phases | No data source |
| Grid guide, Aspect ratio, Stroke thickness, Brush presets, Mini paint | Design-tool tiles with no function as a widget |

Branding: app tiles show each installed app's own icon (no drawn third-party logos); no widget is named
or captioned "Nothing" (the app is not affiliated with Nothing Technology).
