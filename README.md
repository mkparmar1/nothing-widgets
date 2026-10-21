# Dot Widgets

A minimal, monochrome **dot-matrix Android widget pack** built with Jetpack Compose. It brings
**Nothing OS–style** dot-matrix widgets to any Android launcher — clocks, weather, battery, toggles,
productivity, finance, health, smart home, sports, learning, travel and more — each responsive across
2×2, 4×2 and 4×4 sizes, with live in-app previews and customizable tap actions.

> **Not affiliated with Nothing Technology.** Dot Widgets is an independent app. It is not affiliated
> with, endorsed by, or sponsored by Nothing Technology Limited. "Nothing OS" is referenced only to
> describe the visual style the app is inspired by.

---

## Highlights

- **223 widgets across 17 categories** — from clocks and weather to crypto, sleep, thermostats,
  Home Assistant controls, flashcards and flight countdowns.
- **Skinnable** — 8 render skins (Dot, Light Strip, Segment, Pixel, Terminal, Paper, Neon, Native Dot)
  plus 4 frame styles, applied centrally so any widget can be restyled without changing its code.
- **One-tap Looks** — 10 preset restyles (Dot Classic, Light Strip Night, Retro LCD, Terminal, Paper,
  Neon, 8-Bit, Material You, Glass, …) that repaint every placed widget at once.
- **Options on every widget** — every widget ships at least two universal controls (text size,
  corner accent dot) *plus* its own advanced options (toggle, choice, slider, text, colour rule)
  stored per widget, so the *same* widget can be reshaped without adding another catalogue entry.
- **112 generated wallpapers** matched to the app's accent palette (7 accents × 16 patterns).
- **Curated setups** — one-tap setups plus a set of curated looks.
- **Free forever** — every widget, size and theme is unlocked. No billing dependency is shipped.
- **Offline-first** — network data (weather, crypto, FX, stocks) is cached and degrades gracefully;
  most widgets work with no network at all.
- **Material You** — accent repaints from your wallpaper on Android 12+.
- **Lock screen / AOD** providers for clock, date and battery (Android 14+).

---

## Screens

The app has four bottom-nav tabs:

| Tab | What it does |
| --- | --- |
| **WIDGETS** | Browse the full catalogue by category, preview each widget live, configure it, and pin it to the home screen. |
| **SETUPS** | Apply curated widget setups, export/import your own as a shareable code (`DWSET1:`), and browse curated looks. |
| **WALLPAPERS** | Browse the 112 generated wallpapers and set one with a tap. |
| **SETTINGS** | One-tap **Looks** (restyle all widgets), global accent repaint, weather location & data access, permission status, widget status, and about info. |

A permissions screen is shown on first launch (and reminders while exact alarms are off) so each
sensitive feature explains why it needs access before it is granted.

---

## Widget categories

`WidgetRegistry` groups every widget into a `Category`. The catalogue currently spans:

| Category | Examples |
| --- | --- |
| **Clock** | Digital, analog, flip, word, world clock, 24-hour radial, moon time, segmented |
| **Calendar** | Day/date, agenda, timeline, week number, month heatmap, birthdays, ISO week |
| **Battery** | Ring, segmented, temp, cycles, charge ETA, health, multi-device |
| **Weather** | Current, hourly, 5-day, UV, sunrise/sunset, wind, pressure, dew point, AQI |
| **Sensors & System** | Compass, RAM, storage, CPU, uptime, network link, glyph battery |
| **Toggles** | Wi-Fi, Bluetooth, torch, DND, and more as a grid or single cap |
| **Shortcuts** | App shortcuts, favourite contacts, folders, search bar |
| **Productivity** | Steps, water, notes, to-do, habit matrix, burndown, focus timer |
| **Photo** | Single frame, photo grid |
| **Media** | Now playing (compact, waves), media controls |
| **Fun & Lab** | Dice, quote, snake, ambient, chronograph |
| **Finance** | Crypto tick/spark/portfolio, stocks, FX, budget ring, savings, payday |
| **Health** | Steps ring/week, hydration, sleep, heart rate, calories, weight, mindfulness |
| **Smart Home** | Device toggle, room status, lights, thermostat, scenes, energy, sensors |
| **Sports** | Team score, fixtures, tables, form, cricket and tennis from a sports feed you add; F1 next race (bundled) |
| **Learning** | Pomodoro, study sessions, word of the day, reading goal, quiz, exam countdown |
| **Travel** | Multi-city clock, world city, city weather, sun times, flight/trip countdown, packing |

Widgets are registered in `app/src/main/java/com/example/widgets/base/WidgetRegistry.kt`; renderers
live under `widgets/catalogue/`.

---

## Architecture

```
app/src/main/java/com/example/
├── MainActivity.kt              # Compose host + bottom nav + permissions gating
├── core/                        # Reusable, UI-free building blocks
│   ├── astro/                   # Offline moon-phase + sunrise/sunset maths
│   ├── bitmap/                  # Size buckets + dimension resolution
│   ├── config/                  # WidgetOption schema, WidgetOptions, UniversalOptions
│   ├── render/                  # RenderKit, DotMatrixRenderer, Skin/Frame, ColorRule, LayoutVariants
│   ├── theme/                   # Accent palette, theme + background modes
│   └── time/                    # Clock formatting
├── data/                        # Repositories — the only place widgets read data from
│   ├── prefs/ db/               # SharedPreferences + Room persistence
│   ├── weather/ market/ finance/# Network repos (cached, offline-first)
│   ├── activity/ sensor/ health/# Device sensors and user-logged metrics
│   ├── home/ sports/ learn/     # Smart home, sports, learning
│   ├── travel/ calendar/ contacts/ media/ photos/ usage/
│   ├── setups/ wallpapers/
│   ├── looks/                   # One-tap Looks: preset skin/accent/frame/background combos
│   └── billing/                 # Entitlements seam (always true — app is free)
├── ui/                          # Compose screens, sheets, editors, theme
│   ├── browse/ setups/ wallpapers/ settings/ permissions/ weather/
│   ├── config/ components/ theme/    # OptionControls, WidgetConfigSheet, LooksCard, editors
└── widgets/
    ├── base/                    # WidgetSpec, WidgetDefinition, providers, registry, alarms
    ├── catalogue/               # Renderer objects per family
    └── collection/              # Collection widget (service + views)
```

### Rendering model

Widgets are drawn to a `Bitmap` with a shared **`RenderKit`** (theme colours, padding, background,
header) and **`DotMatrixRenderer`** primitives (`drawText`, `drawDotRun`, `drawDotRing`,
`drawAnalogClock`, `drawWeatherIcon`, …). This lets the app render every widget identically in three
places:

1. **Live previews** in the browse screen.
2. **Home-screen widgets** via `AppWidgetProvider` + `RemoteViews`.
3. **Snapshot tests** that render every widget at every size.

Renderers always read the requested accent via `spec.accentColor` and only use accent colour for live
state; headers and captions stay neutral grey for legibility.

### Skins, frames and Looks

`core/render/Skin.kt` defines the visual language a widget is drawn in — `Skin` (Dot, Glyph, Segment,
Pixel, Terminal, Paper, Neon, Nothing NDot) and `Frame` (Hairline, None, Dotted, Glow). `DotMatrixRenderer`
applies them centrally (digits, labels, dot runs/rings, background), so a change of skin restyles every
widget without any renderer code changing. Some skins force a light/dark look (`PAPER`, `NEON`,
`TERMINAL`).

`data/looks/Looks.kt` bundles skin + accent + frame + dot style + background into **10 named presets**.
`Looks.apply(context, look)` writes the chosen look into every placed widget's config (keeping each
widget's own content and options) and redraws them all in one tap from Settings.

### Options on every widget

Two layered option systems keep 200+ widgets shapeable:

1. **Universal options** (`core/config/UniversalOptions.kt`) — `text_size` and `corner_dot` are added
   to *every* `WidgetDefinition` at registration (`WidgetRegistry`), so each design exposes at least
   these two controls.
2. **Per-widget options** — `core/config/WidgetOption.kt` defines a sealed schema (`Toggle`, `Choice`,
   `Slider`, `Text`, `ColorRule`) that a definition may additionally declare. Widgets with two or more
   of their own options show an **ADVANCED OPTIONS** badge in the browse grid (`BrowserScreen`).

Users edit them in a collapsible **ADVANCED** section (`ui/config/OptionControls.kt`); values
round-trip through the widget's `configJson` under the `"opt"` key via `WidgetOptions.parse` /
`WidgetOptions.write`. `UniversalOptionsTest` asserts every widget has at least two options.

### Data & permissions

Repositories are private-constructor singletons (`getInstance(context)`), prefs-backed and
offline-first. Network repositories cache last-known values and refresh opportunistically on a
background coroutine with an `AtomicBoolean inFlight` guard; nothing blocks the render path.

Sensitive permissions are **only requested when the matching widget is used**:

| Permission | Used for |
| --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Weather, crypto, FX, stocks, Home Assistant |
| `ACCESS_COARSE_LOCATION` | Weather for your current place |
| `READ_CALENDAR` | Agenda widgets |
| `READ_CONTACTS` | Favourite-contact widgets |
| `ACTIVITY_RECOGNITION` | Step counter |
| `BLUETOOTH` / `BLUETOOTH_CONNECT` | Bluetooth toggles + connected-device battery |
| `PACKAGE_USAGE_STATS` | Screen-time widgets |
| `ACCESS_NOTIFICATION_POLICY` | Do Not Disturb toggle |
| `SET_WALLPAPER` | Wallpaper screen |
| `SCHEDULE_EXACT_ALARM`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` | Accurate clock ticks, timer alarms, widget refresh after reboot |

**Network endpoints** (all HTTPS, no API keys): Open-Meteo (weather + geocoding), CoinGecko (crypto),
Stooq (stocks), open.er-api.com (FX), plus a user-configured Home Assistant server.

---

## Tech stack

- **Kotlin** 2.2.10 · **Jetpack Compose** (Material 3) · AGP 9.1.1
- **compileSdk / targetSdk** 36 · **minSdk** 24
- **Room** (persistence) · **Retrofit / OkHttp / Moshi** (networking)
- **Coil** listed but not enabled; **Firebase** dependencies are present in Gradle but to be removed
  (see `docs/nothing_phone_widget_play_store_audit_prompt.md` — no Firebase/Gemini code exists)
- Fonts: **Space Grotesk** & **JetBrains Mono** (SIL Open Font License, see `assets/licenses/OFL.txt`)

### Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

On Windows (PowerShell) with Android Studio's bundled JDK:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
& .\gradlew :app:assembleDebug :app:testDebugUnitTest
```

### Tests

| Test | Covers |
| --- | --- |
| `WidgetRenderSnapshotTest` | Renders **every** widget at every size, dark + light, native graphics; writes contact sheets to `build/widget-snapshots/`. Includes a real-device-size (2b) crispness pass. |
| `WidgetOptionsTest` | `WidgetOption` round-trip, unknown-key preservation, deletion, malformed config, choice schema, `ColorRule`. |
| `UniversalOptionsTest` | Every widget has ≥2 options; text-size/corner-dot round-trip; Looks have unique ids. |
| `MaterialYouPaletteTest` | Material You accent derivation. |
| `SkinSnapshotTest` | Every skin renders the sample widgets. |
| `ClockFormatSnapshotTest` | Clock formats + dot styles render and read back from config. |
| `ExampleRobolectricTest` | Catalogue size/categories, glyph table, share codes, community feed, lock-safe widgets. |
| `WidgetSetupStateTest` | Setup state round-trip. |

---

## Project status

Phases 0–4 and 8 are complete and pushed; Phase 7 (catalogue expansion to 200+) is complete.

**Before Play submission**, run the read-only audit in
[`docs/nothing_phone_widget_play_store_audit_prompt.md`](docs/nothing_phone_widget_play_store_audit_prompt.md)
and clear its "Must fix" items (remove unused Firebase/AppCheck deps, fix `metadata.json`, add the
non-affiliation disclaimer, harden the release build). See also
[`docs/BUILD_PLAN.md`](docs/BUILD_PLAN.md) and
[`docs/UX_IMPLEMENTATION_PLAN.md`](docs/UX_IMPLEMENTATION_PLAN.md).

---

## License

App code © the Dot Widgets authors. Bundled fonts are under the SIL Open Font License
(`app/src/main/assets/licenses/OFL.txt`). Third-party data providers (Open-Meteo, CoinGecko, Stooq,
open.er-api.com) are used under their respective terms.
