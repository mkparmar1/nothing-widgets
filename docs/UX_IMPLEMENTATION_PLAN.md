# Dot Widgets — UX & Design Implementation Plan

**Status:** Proposed · **Date:** 19 Oct 2026 · **Owner:** mkparmar1
**Target device for verification:** Nothing Phone (3a) Pro (A059P), Android 16 (API 36), Nothing Launcher

This plan turns the 11 UX/design recommendations from the app review into six implementation phases.
Each phase is self-contained: it leaves the app building, tested and installable, and ends with an
on-device check and a commit.

---

## Contents

1. [Background: where the app is today](#1-background-where-the-app-is-today)
2. [Goals and non-goals](#2-goals-and-non-goals)
3. [Phase overview](#3-phase-overview)
4. [Phase A — Choose and edit designs from the home screen](#phase-a--choose-and-edit-designs-from-the-home-screen)
5. [Phase B — Large layouts and sharp rendering](#phase-b--large-layouts-and-sharp-rendering)
6. [Phase C — Ask for permissions only when needed](#phase-c--ask-for-permissions-only-when-needed)
7. [Phase D — Easier browsing and honest wording](#phase-d--easier-browsing-and-honest-wording)
8. [Phase E — Polish: theme, colour rules, accessibility](#phase-e--polish-theme-colour-rules-accessibility)
9. [Phase F — Subscription-ready hook (no billing yet)](#phase-f--subscription-ready-hook-no-billing-yet)
10. [Testing strategy](#10-testing-strategy)
11. [Risks and mitigations](#11-risks-and-mitigations)
12. [Open decisions](#12-open-decisions)
13. [Appendix: file map](#13-appendix-file-map)

---

## 1. Background: where the app is today

| Area | Current behaviour | Problem for users |
|---|---|---|
| Adding from the launcher | Three providers ("Dot Widget 2×2 / 4×2 / 4×4"). Dropping one creates a **Digital Clock** (`BaseDotWidgetProvider.defaultWidgetTypeId`). Launcher preview is the app icon (`previewImage="@mipmap/ic_launcher"`). | Most users add widgets from the launcher, so they never reach the other 90 designs. |
| Editing a placed widget | Not possible. Settings live in `WidgetInstance.configJson` but no screen edits an existing instance. | Changing a design means delete + re-add. |
| Rendering size | Fixed bitmaps per bucket in `SizeResolver` (S 280×280, M 480×240, L 380×380), drawn with `fitCenter`. | Soft on high-density screens; letterboxed when the widget's real aspect differs. |
| Large layouts | Most renderers cap text at 14 px (`.coerceAtMost(14f)`) and reuse the M/S layout. | 4×4 widgets show tiny text in a corner with empty space. |
| Permissions | Startup screen lists all 8 permissions (`PermissionCatalog.ALL`). | Long, and asking up front makes users deny more. |
| Browse screen | One full-width card per widget (preview + "USE THIS WIDGET" button), 91 cards. | Very long scroll; button duplicates the card tap. |
| Wording | "0% IDLE DRAIN" chip (`BrowserScreen.kt`), "system audit" rows (`SettingsScreen.kt`). | Not measured by the app; can read as misleading. |
| Theme | Setup preview is always dark when theme is AUTO (`WidgetConfigSheet`: `ThemeMode.AUTO -> true`). | Light-mode users see the wrong preview. |
| Accessibility | Every widget's image has `contentDescription="@string/app_name"`. | TalkBack says "Dot Widgets" for all 91 widgets. |

---

## 2. Goals and non-goals

**Goals**
- Every widget design is reachable and editable from the home screen.
- Widgets fill their space and look crisp at every size, especially 4×4.
- Permissions are requested in context, with a one-tap path from any "needs access" message.
- Browsing 91 widgets is quick; the app makes no claims it can't back up.
- The app is ready for a single subscription gate later.

**Non-goals (this plan)**
- Google Play Billing / the ₹159/month subscription itself (only the hook in Phase F).
- New widget designs.
- Lock-screen or Glance/Compose widgets.

---

## 3. Phase overview

| Phase | Covers | Effort | Depends on | User-visible result |
|---|---|---|---|---|
| **A** | #1, #2 | ~1 day | — | Design picker when adding from the launcher; **Edit** on long-press |
| **B** | #3 | 1–2 days | A (shared setup UI) | Sharp, full-size widgets; real 4×4 layouts |
| **C** | #4 | ~0.5 day | — | Permission asked when a widget needs it |
| **D** | #5, #6, #10 | ~0.5–1 day | — | 2-column grid, honest status line, first-run tip |
| **E** | #7, #8, #9 | ~0.5 day | B | Light theme preview, consistent colours, TalkBack |
| **F** | #11 | ~1 hour | A | One `Entitlements` check at Save |

Recommended order: **A → B → C → D → E → F**.

---

## Phase A — Choose and edit designs from the home screen

### Outcome
- Dropping a Dot Widgets widget on the home screen opens a picker filtered to that size, then the setup screen.
- Long-pressing a placed widget shows **Edit** (Android 12+), which reopens the setup screen pre-filled.
- The launcher's widget list shows real dot-matrix previews instead of the app icon.

### A1. Extract shared setup UI
**Files:** `ui/config/WidgetConfigSheet.kt` → new `ui/config/WidgetSetupContent.kt`

- Move the sheet body (preview, `WidgetContentEditor`, size, accent, background, theme, corner radius, label, tap action) into:
  ```kotlin
  @Composable
  fun WidgetSetupContent(
      definition: WidgetDefinition,
      initial: WidgetSetupState,          // defaults, or values from an existing WidgetInstance
      lockedSize: SizeBucket?,            // set when the launcher already chose the size
      onStateChange: (WidgetSetupState) -> Unit
  )
  ```
- `WidgetSetupState` is a data class holding size, accent, background, theme, radius, label, tap action, with `toConfigJson()` / `fromInstance(WidgetInstance)` helpers. This replaces the `JSONObject` built inline in `pinWidgetToHomeScreen`.
- `WidgetConfigSheet` becomes a thin wrapper: `ModalBottomSheet { WidgetSetupContent(...) ; AddButton }`.

### A2. New `WidgetConfigureActivity`
**Files:** new `ui/config/WidgetConfigureActivity.kt`, `AndroidManifest.xml`

Flow:
1. Read `EXTRA_APPWIDGET_ID`. If invalid → `finish()`.
2. **Immediately** `setResult(RESULT_CANCELED, resultIntent)` so that backing out removes the half-added widget (Android contract).
3. Resolve the size bucket from the provider: `AppWidgetManager.getAppWidgetInfo(id).provider.className` → `DotWidgetProviderS/M/L` → `S/M/L`.
4. Load the existing `WidgetInstance` (if any). Exists with a non-default config → **edit mode**, skip the picker.
5. **Picker step** (new mode only): grid of `WidgetRegistry.getAll().filter { size in it.sizes }`, grouped by category, using `WidgetBitmapPreview`.
6. **Setup step:** `WidgetSetupContent(definition, initial, lockedSize = bucket)`.
7. **Save:** `insertOrUpdate(WidgetInstance(id, definition.id, 1, state.toConfigJson()))` → `WidgetRenderEngine.buildRemoteViews` → `updateAppWidget` → `setResult(RESULT_OK, resultIntent)` → `finish()`.

Manifest:
```xml
<activity
    android:name=".ui.config.WidgetConfigureActivity"
    android:exported="true"
    android:theme="@style/Theme.MyApplication"
    android:excludeFromRecents="true"
    android:taskAffinity="">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
    </intent-filter>
</activity>
```
(`exported="true"` is required: the launcher starts it.)

### A3. Provider XML updates
**Files:** `res/xml/dot_widget_info_s.xml`, `_m.xml`, `_l.xml`, `res/values/strings.xml`

```xml
android:configure="com.example.ui.config.WidgetConfigureActivity"
android:widgetFeatures="reconfigurable"
android:previewImage="@drawable/widget_preview_s"   <!-- _m / _l -->
android:description="@string/widget_desc_s"
```
- Keep `previewLayout` **removed** (it would override `previewImage` on Android 12+ and only shows the empty canvas layout).
- Strings: `widget_desc_s` → "Dot Widgets • Small 2×2 — clocks, battery, toggles", `_m` → "Dot Widgets • Wide 4×2 — weather, calendar, media", `_l` → "Dot Widgets • Large 4×4 — agenda, photos, month".
- Provider labels in the manifest: "Dot Widgets • Small / Wide / Large".

### A4. Launcher preview images
**Files:** `app/src/test/.../WidgetRenderSnapshotTest.kt`, new `res/drawable-nodpi/widget_preview_{s,m,l}.png`

- Add a test helper (run on demand) that renders `clock.dotdigital.s`, `weather.current.temp` (M) and `calendar.month.grid` (L) with the default accent and writes them as `widget_preview_*.png`.
- Copy those three PNGs into `res/drawable-nodpi/` and commit them.

### A5. Provider default behaviour
**Files:** `widgets/base/DotWidgetProvider.kt`

- Keep inserting a default instance in `onUpdate` (the launcher may call `onUpdate` before the configure activity finishes). The activity overwrites it on Save.
- Track "configured" with a `configured: true` key in `configJson` so edit mode can tell a fresh default apart from a chosen design.

### A6. Pinning from inside the app
- `pinWidgetToHomeScreen` keeps using `WidgetConfigReceiver` as the pin callback. On Android 8+, the configure activity is **not** launched for pinned widgets, so the flow stays as it is.
- **Verify on device** that Nothing Launcher doesn't also open the configure activity on pin. If it does, have the activity detect a pending pin (instance already `configured`) and finish immediately with `RESULT_OK`.

### Acceptance criteria
- [ ] Launcher → Widgets → Dot Widgets shows three entries with real previews and clear names.
- [ ] Dropping each size opens the picker with only designs that support that size.
- [ ] Back from the picker removes the widget; Save places the chosen design.
- [ ] Long-press → Edit opens the setup screen with the current settings; changes apply on Save.
- [ ] "Add to home screen" from inside the app still works and doesn't show the picker twice.

---

## Phase B — Large layouts and sharp rendering

### Outcome
Widgets are drawn at the widget's real pixel size and aspect ratio (within a memory budget), text scales with the widget, and 4×4 widgets have layouts designed for the space.

### B1. Real-size bitmaps with a budget
**Files:** `core/bitmap/SizeResolver.kt`, `widgets/base/WidgetRenderEngine.kt`

- Read size from widget options:
  - Android 12+: `OPTION_APPWIDGET_SIZES` (list of `SizeF` in dp), take the largest.
  - Older: portrait = `MIN_WIDTH × MAX_HEIGHT`, landscape = `MAX_WIDTH × MIN_HEIGHT`; use portrait.
- `px = dp × displayMetrics.density`.
- **Budget:** `width × height × 4 ≤ 1_000_000 bytes` (RemoteViews bitmaps travel over Binder, which fails around 1 MB). If over, scale both sides down by `sqrt(budget / bytes)`.
- Return the real aspect ratio; set the ImageView to `fitXY` once sizes match the widget exactly (no letterboxing).
- `density` passed to renderers = `px / dp` actually used, so `RenderKit.d` stays meaningful after scaling.
- Keep the bucket (S/M/L) for choosing a layout, derived from the dp size as today.

### B2. Text that scales
**Files:** `core/render/RenderKit.kt`

- Add a scale unit: `val u = minOf(width, height) / 100f` (1 u = 1% of the short side).
- `textPaint(color, sizeU)` sizes text in `u`; no fixed px caps.
- Keep the old `textPaint(color, sizeDp, maxPx)` temporarily for unmigrated renderers.

### B3. Migrate remaining renderers to `RenderKit`
**Files:** `widgets/catalogue/{Clock,Calendar,Battery,Weather,Productivity,MediaAndFun,UniqueSensor,Toggle}Renderers.kt`

About 40 functions still build colours, padding and background by hand. Move each to `RenderKit` (background, header, text sizes) one file at a time; run the snapshot test after each file.

### B4. Dedicated Large (4×4) layouts
For each, add an `if (spec.sizeBucket == SizeBucket.L)` branch or a separate `renderXxxLarge`:

| Widget | Large layout |
|---|---|
| Digital Clock L | Time across ~60% of the height, date + weekday below, next alarm (`AlarmManager.nextAlarmClock`) at the bottom |
| Flip Clock L | Two rows: HH cards on top, MM cards below |
| Agenda 5 / Timeline | Day header, 5 rows with time column and calendar-colour dot, 24 h strip at the bottom |
| Notes / To-Do 5 | Larger text, up to 8 lines / 8 tasks, done count |
| Month Grid L | Day numbers in each cell, today highlighted, events dotted (when calendar access) |
| 5-Day Forecast L | Icons + high + low + rain % per day |
| RAM / Storage / Year Progress | Big number on top, dot bar, breakdown lines |
| Device Battery (combo/quad) | 2×2 grid of rings instead of one row |

### B5. Verification
- Snapshot test renders at the 3a Pro's real widget sizes (read from `adb shell dumpsys appwidget` for each placed widget) in addition to the bucket defaults.
- Review contact sheets; install; compare against home-screen screenshots.

### Acceptance criteria
- [ ] No visible softness on 2×2 widgets on the 3a Pro.
- [ ] No letterbox bars at any size, including resized widgets.
- [ ] Every L widget uses at least ~80% of its area; no text smaller than 3 u.
- [ ] No `TransactionTooLargeException` in logcat with 6+ widgets placed.

---

## Phase C — Ask for permissions only when needed

### C1. Widgets declare their permission
**Files:** `widgets/base/WidgetDefinition.kt`, `WidgetRegistry.kt`

```kotlin
val requiredPermission: AppPermission? get() = null
```
Set it in `createDef` for: agenda/timeline → `Calendar`; steps/rings → `Activity`; media/soundwave → `MediaAccess`; screen time → `UsageAccess`; device battery → `NearbyDevices`; weather/sun (optional) → `Location`; DND toggle → `DndAccess`.

### C2. Inline banner in setup
**Files:** `ui/config/WidgetSetupContent.kt`

If `definition.requiredPermission?.isGranted(context) == false`, show a banner at the top: icon, one line ("This widget shows your calendar events"), **ALLOW** button reusing the launcher logic from `PermissionList`. Saving is still allowed.

### C3. Tappable "needs access" messages
**Files:** `core/render/RenderKit.kt`, `WidgetRenderEngine.kt`, `MainActivity.kt`

- When a widget renders `RenderKit.message(...)` because a permission is missing, the engine sets its tap action to `MainActivity` with extra `open_permission=<id>`.
- `MainActivity` opens Settings → Data access and scrolls to that permission (or launches it directly).

### C4. Lighter startup screen
**Files:** `ui/permissions/PermissionsScreen.kt`

Startup shows only **Alarms & reminders** (it affects every clock). All other permissions live in Settings → Data access and in the setup banners. Keep the "show again while alarms are off" rule.

### Acceptance criteria
- [ ] First launch shows one permission card.
- [ ] Picking Agenda without calendar access shows the banner; ALLOW opens Android's dialog.
- [ ] Tapping "ALLOW CALENDAR ACCESS IN THE APP" on a placed widget lands on the calendar permission.

---

## Phase D — Easier browsing and honest wording

### D1. Browse grid
**Files:** `ui/browse/BrowserScreen.kt`

- `LazyVerticalGrid(GridCells.Adaptive(minSize = 160.dp))` → 2 columns on phones.
- Compact card: preview, title (1 line), size badges. **The whole card is the tap target**; remove the "USE THIS WIDGET" button.
- Category chips in a `stickyHeader`-style row that stays visible while scrolling.
- **Recommended** row at the top (horizontal): Digital Clock, Glyph Battery, Weather, Agenda, Now Playing, Flip Clock.

### D2. Remove unbacked claims
- Delete the "0% IDLE DRAIN" chip (`BrowserScreen.kt`).
- Replace the Settings "system audit" card with a real **Widget status** card:
  - widgets placed (count from `AppWidgetManager.getAppWidgetIds`)
  - last refresh time (store a timestamp in `updateAllPlacedWidgets`)
  - next refresh ("in 1 min", or "paused — no widgets")
  - exact alarms on/off

### D3. First-run tip
- Dismissible card at the top of Browse until dismissed: "Add a widget: long-press your home screen → Widgets → Dot Widgets." Remember dismissal in preferences.

### Acceptance criteria
- [ ] All 91 widgets reachable within ~12 screens of scrolling (vs hundreds today).
- [ ] No text in the app claims something the app doesn't measure.

---

## Phase E — Polish: theme, colour rules, accessibility

### E1. Light theme
- `WidgetConfigSheet` / `WidgetSetupContent`: `ThemeMode.AUTO -> isSystemInDarkTheme()`.
- Snapshot test: add a light-mode pass (`isDark = false`), review sheets, fix contrast issues (grey `0xFF8A8A8A` on `0xFFF2F2F2` is borderline; use `0xFF6A6A6A` in light mode).

### E2. Colour rules (documented in `RenderKit`)
| Element | Colour |
|---|---|
| Header labels, captions | grey |
| Primary values | on-colour (white/black) |
| Live state only: running, charging, now, today, warnings | accent |
| Inactive dots | off-colour |

Apply across renderers (straightforward after Phase B3).

### E3. TalkBack
- Add `fun describe(context: Context): String` to `WidgetDefinition` (default: title). Implement for the main families: clocks ("Time 21:12"), battery ("Battery 85 percent, charging"), weather ("Mumbai 31 degrees, clear"), agenda ("3 events left today, next at 14:00 Design review"), timers, media.
- `WidgetRenderEngine`: `views.setContentDescription(R.id.widget_image_view, definition.describe(context))`.

### E4. NDot headers on Nothing phones
- If the `NDot57` system font exists (`Typeface.create("NDot57", NORMAL) != Typeface.DEFAULT`), use it for in-app section headers; otherwise keep the current font.

### Acceptance criteria
- [ ] Light-mode preview matches the placed widget.
- [ ] TalkBack reads a meaningful description for every widget family.

---

## Phase F — Subscription-ready hook (no billing yet)

**Files:** new `data/billing/Entitlements.kt`, `WidgetSetupContent` save button, `WidgetConfigureActivity`

```kotlin
object Entitlements {
    /** Replace with Google Play Billing; everyone is entitled until then. */
    fun hasFullAccess(context: Context): Boolean = true
}
```
- Checked in exactly two places: setup **Save / Add to home screen**, and `WidgetConfigureActivity` Save.
- When billing arrives: show the paywall (₹159/month, 7-day trial) at those two points; nothing else changes.

---

## 10. Testing strategy

| Layer | What | When |
|---|---|---|
| Render snapshot test (`WidgetRenderSnapshotTest`) | All 91 widgets × sizes, dark + light, contact sheets in `app/build/widget-snapshots/` | Every phase, before install |
| Unit tests | `WidgetSetupState` JSON round-trip; `SizeResolver` budget maths; `timeInWords`; astronomy | A, B |
| Build | `assembleDebug` + `testDebugUnitTest` must pass | Every phase |
| On device (3a Pro) | Phase acceptance checklist; `adb logcat` for crashes and `TransactionTooLargeException`; `dumpsys appwidget` for sizes | End of every phase |
| Regression | Clock ticks on the minute, live seconds, timers, toggles, media controls | End of A and B |

---

## 11. Risks and mitigations

| Risk | Likelihood | Mitigation |
|---|---|---|
| Large bitmaps exceed Binder limit on some launchers | Medium | 1 MB budget + automatic scale-down; catch failure and retry at 0.7× |
| Nothing Launcher handles `configure` / `reconfigurable` differently | Low–medium | Verify on device in Phase A; fallback: in-app "Edit placed widgets" list |
| Configure activity opened for pinned widgets on some launchers | Low | Detect already-configured instance and finish with `RESULT_OK` |
| Renderer migration (B3) changes layouts unintentionally | Medium | One file at a time; compare contact sheets before/after |
| Removing the startup permission list lowers grant rates | Low | Contextual banners in setup usually raise grant rates; watch feedback |

---

## 12. Open decisions

| # | Question | Recommendation |
|---|---|---|
| 1 | Cancelling the picker when adding from the launcher: remove the widget, or keep a Digital Clock? | **Remove** (standard Android behaviour) |
| 2 | Phase order | **A → B → C → D → E → F** (or D before B if browsing matters more short-term) |
| 3 | Git push identity (pushes currently use `Dushyant-Khoda`) | Set `credential.https://github.com.username mkparmar1` for this repo only |
| 4 | Recommended row contents (D1) | The six listed; adjust to taste |

---

## 13. Appendix: file map

**New files**
| File | Phase |
|---|---|
| `ui/config/WidgetSetupContent.kt` (+ `WidgetSetupState`) | A |
| `ui/config/WidgetConfigureActivity.kt` | A |
| `res/drawable-nodpi/widget_preview_{s,m,l}.png` | A |
| `data/billing/Entitlements.kt` | F |

**Changed files**
| File | Phases |
|---|---|
| `AndroidManifest.xml` | A |
| `res/xml/dot_widget_info_{s,m,l}.xml`, `res/values/strings.xml` | A |
| `ui/config/WidgetConfigSheet.kt` | A, C, E, F |
| `widgets/base/DotWidgetProvider.kt` | A |
| `core/bitmap/SizeResolver.kt` | B |
| `widgets/base/WidgetRenderEngine.kt` | B, C, E |
| `core/render/RenderKit.kt` | B, C, E |
| `widgets/catalogue/*Renderers.kt` | B, E |
| `widgets/base/WidgetDefinition.kt`, `WidgetRegistry.kt` | C, E |
| `ui/permissions/PermissionsScreen.kt` | C |
| `MainActivity.kt` | C |
| `ui/browse/BrowserScreen.kt` | D |
| `ui/settings/SettingsScreen.kt` | D |
| `app/src/test/.../WidgetRenderSnapshotTest.kt` | A, B, E |
