# Dot Widgets — App Redesign v2 (from the `docs/design` mockups)

> **Goal:** rebuild the app's screens to match the direction in `docs/design/` (the two 18-screen
> mockup boards), while keeping every feature that works today. Pixel-perfect copying of AI
> mockups is not the target — a consistent, polished app that *reads* the same is.
>
> **Started:** 2026-11-14 · **Owner:** mkparmar1 · **Roadmap ID:** P26
> **Related:** `docs/DESIGN_QUALITY_PLAN.md` (widgets themselves), `docs/FEATURE_ROADMAP.md`

---

## 0. Decisions needed before building

| # | Decision | Options | Default if no answer |
|---|---|---|---|
| D-1 | **Free or Premium?** Mockups show Premium / Go Premium / Subscription / Restore (₹79 / month, ₹499 / year, crown badges). The release plan says **free**. | A: Free (no paywall screens) · B: Free + Premium tier | **A — Free.** Premium screens (R9) are designed but not built. `Entitlements.hasFullAccess` stays `true`. |
| D-2 | Bottom nav | Mockup: Home · Explore · **+** · My Widgets · Settings | Use the mockup nav. Setups and Wallpapers move into Home/Explore sections with "See all". |
| D-3 | Customizer layout | Tabs (Skin / Shape / Accent / Size) vs today's one long scroll | Tabs, with the live preview pinned on top. All current sections stay, grouped under the tabs. |
| D-4 | Ratings / download badges on widget cards (★ 4.8, ⤓) | Real data needed; we have none | Drop them. Show size badge, LOCK / OPTIONS / EXCLUSIVE chips as today. |
| D-5 | Hero images | Mockups use fake 3D renders | Render our real widgets into the banner (reuses `WidgetBitmapPreview`). |

Everything below assumes the defaults. If D-1 = B, add R9 and about 1 week.

---

## 1. What the mockups contain (reference map)

Board 1 (`029138e6…png`) and board 2 (`ca6cccd2…png`). Screen numbers from board 2:

| # | Mockup screen | Today's screen | Change size |
|---|---|---|---|
| 1 | Home: hero banner, Quick Access (Widgets / Builder / Setups / Wallpapers), Featured Widgets, Popular Setups | — (Widgets tab is the start) | **New** |
| 2 | Widgets (Explore): search, category chips, grid, Featured | Widgets tab (`BrowserScreen`) | Restyle |
| 3 | Widget Details: big preview, name, description, tags, "Add to Home Screen" | Opens setup sheet directly | **New** (light) |
| 4 | Customizer: Skin / Shape / Accent / Size tabs, pinned preview, size slider | Setup sheet (`WidgetSetupContent`) | Rework |
| 5 | Widget Builder: preview, block chips, canvas, "Build Widget" | `CustomWidgetBuilderScreen` | Restyle |
| 6 | My Widgets: All / Active / Inactive, cards with play button | My widgets chip + Manage dialog | **New tab** |
| 7 | Setups: cards with mini widget strip, Apply Setup | `SetupsScreen` | Restyle |
| 8 | Wallpapers: full-bleed cards, Apply Wallpaper, grid | `WallpapersScreen` | Restyle |
| 9 | Settings: list with icons | `SettingsScreen` | Restyle |
| 10 | Appearance: Themes, Theme Style (Dark/Light/Auto), Accent Color, Widget Style | Appearance page | Restyle |
| 11 | Theme Preview: big preview, Dark/Light/System, Dynamic Color, Apply Theme | Part of Appearance | **New** (light) |
| 12 | Permissions: list with switches | `PermissionsScreen` | Restyle |
| 13 | Weather: connection toggle, location, current card, update interval, Test Connection | Weather & connections + Weather detail sheet | Merge + restyle |
| 14 | About: logo, version, links, "Made with ♥" | About page | Restyle |
| 15–17 | Go Premium, Manage Purchases, Subscription | — | **Only if D-1 = B** |
| 18 | Widget Status: big green check, "All widgets working", Check Again | Widget status page | Restyle |

---

## 2. Design system changes (do first — everything else uses them)

Files: `ui/theme/DesignTokens.kt`, `Color.kt`, `Type.kt`, `Theme.kt`, `ui/components/DotComponents.kt`.

- **Colours:** background `#0A0A0A`, card `#141414`, card-raised `#1C1C1C`, outline `#262626`,
  accent red `#E0162B` (keep current accent system; this is the default), text `#FFFFFF` /
  `#A3A3A3`. Red "glow" gradient for hero cards (radial, 12–18 % alpha). Light theme keeps the
  same structure with inverted neutrals.
- **Shapes:** cards 20 dp, chips full pill, buttons 16 dp, hero 24 dp. Bottom nav bar floating
  with 28 dp radius and a raised centre "+" button.
- **Type:** Geist (already bundled). Display 32/40 bold for hero titles; section titles 22 semibold;
  card titles 16 semibold; captions 12. Geist Mono for widget labels and numbers.
- **Components to add to `DotComponents.kt`:**
  - `HeroCard(title, subtitle, cta, preview)` — red-glow gradient card with a rendered widget.
  - `QuickAccessTile(icon, label, selected)` — 4-up square tiles.
  - `SectionHeader(title, "See all")`.
  - `WidgetCard(definition, badges)` — preview on dark tile, name, caption, size badge.
  - `SetupCard(setup)` — title, caption, 3 mini widgets, Apply button.
  - `WallpaperCard(wallpaper, selected)` — full-bleed image, check mark when applied.
  - `SettingsRow(icon, title, caption)` — with the round icon tile.
  - `SegmentTabs(items)` — Skin / Shape / Accent / Size and Dark / Light / Auto.
  - `ColorDots(colours, selected, onAdd)` — accent picker row with "+".
  - `BottomNav(items, onPlus)` — floating bar, centre "+".
  - `StatusHero(icon, title, caption)` — big circle icon (Widget Status).
- **Motion:** 200 ms fade-through between tabs; cards scale 0.98 on press; hero preview rotates
  through 3 widgets every 5 s (respects "reduce motion").

**Tasks**
- [x] Tokens + light/dark palettes + contrast check (4.5:1 body, 3:1 large) — v2 palette in `DesignTokens.kt`, shapes `button`/`card`/`hero`/`pill`, type scale updated
- [x] 11 components above with previews — in `DotComponents.kt` (`DotSetupCard` to avoid clashing with the Setups screen's card)
- [ ] Component screenshot test (Robolectric) so restyles don't regress

---

## 3. Build order

Each step ships on its own and keeps the app usable. Tick when done; record the commit.

### R1 — Navigation shell + Home (new)
**Status:** Done · **Effort:** M

- `MainActivity`: tabs → Home · Explore · (+) · My Widgets · Settings. "+" opens the Builder.
- `ui/home/HomeScreen.kt`:
  - Top bar: dot-grid logo + "Dot Widgets." (no crown unless D-1 = B).
  - Hero: "Make your home screen smarter", caption, **Explore Widgets →**, rendered widget,
    3-dot pager (3 hero cards: Widgets / Builder / Setups).
  - Quick Access tiles: Widgets · Builder · Setups · Wallpapers.
  - Featured Widgets (horizontal, 2 visible) from the "Only in Dot Widgets" list → See all = Explore.
  - Popular Setups card → See all = Setups screen.
- Setups and Wallpapers become full screens reached from Home (and from Explore's top chips),
  not tabs.
- Acceptance: cold start lands on Home in < 1 s after splash; every tile opens the right place;
  back from any sub-screen returns to Home.

### R2 — Explore (Widgets) restyle
**Status:** Not started · **Effort:** M

- Keep `BrowserScreen` logic (search, categories, counts, "Only in", Recommended, All).
- New `WidgetCard`; grid 2-up on phones, 3-up ≥ 600 dp; category chips pill style.
- Remove the "Add a widget: long-press…" banner from the top; move the hint to Widget Details.
- Tap a card → **Widget Details** (R3), not straight to the sheet.
- Acceptance: 233 widgets still listed; search and chips unchanged in behaviour; scroll 60 fps.

### R3 — Widget Details (new, light)
**Status:** Not started · **Effort:** S

- Big preview (all sizes in a pager), title, description, tags (Clock · Minimal · Dark), size
  badges, permissions needed, **Customize** and **Add to Home Screen**.
- "Add to Home Screen" uses current defaults via `pinWidgetToHomeScreen`.
- Acceptance: works for every widget incl. custom and collection widgets; lock-safe badge shows.

### R4 — Customizer (setup sheet rework)
**Status:** Not started · **Effort:** L

- Pinned live preview with size pager; tabs:
  - **Skin:** skin carousel, frame, dot style.
  - **Shape:** size, corners, label on/off, background mode + opacity, **background image** (photo,
    placement editor, dot filter — unchanged features).
  - **Accent:** accent swatches, custom colour, theme (System / Dark / Light).
  - **Size / More:** size, tap action, advanced options, clock formats, stack items, content
    editors (notes, to-dos, photos, shortcuts).
- Save bar: **Add to Home Screen** (or **Save** when editing a placed widget).
- Same `WidgetSetupState`; only layout changes. `WidgetConfigureActivity` (launcher configure)
  uses the same composable.
- Acceptance: every existing setting reachable within 2 taps; `WidgetSetupScreenTest` updated and
  green; photo editor and dot filter unchanged.

### R5 — My Widgets tab (new)
**Status:** Partial — tab, All/Active/Inactive, previews, edit and delete shipped; duplicate and add-to-home still open · **Effort:** M

- Tabs: **All / Active / Inactive**. Lists custom (builder) widgets *and* placed widgets.
  - Active = placed on the home screen (from `WidgetInstance`), Inactive = saved but not placed.
- Card: preview, name, size, last refresh; actions: Edit (opens Customizer or Builder), Add to
  home, Duplicate, Delete.
- Replaces the "My widgets" chip + `CustomWidgetManagerDialog` (keep the dialog code until R5 is
  done, then remove).
- Acceptance: a placed widget shows as Active within one refresh; deleting a custom widget that is
  placed shows a warning first.

### R6 — Builder restyle
**Status:** Not started · **Effort:** S–M

- Mockup layout: preview on top, block chips with icons (Time · Date · Battery · Weather · Text ·
  Image · Progress · Steps · Next event · Divider · Spacer), canvas, **Build Widget** button.
- Templates row stays. Functionality unchanged (P2); Builder 2.0 features are a separate item.
- Acceptance: `CustomWidgetBuilderTest` green; builder opens from the "+" nav button.

### R7 — Setups + Wallpapers restyle
**Status:** Not started · **Effort:** M

- Setups: `SetupCard` with mini widget strip and **Apply Setup**; filter chips (All · Minimal ·
  Bold · Classic); Share / Import stays at top.
- Wallpapers: `WallpaperCard` full-bleed grid, selected check mark, **Apply Wallpaper** on the
  featured card; filter chips (palette names as today).
- Acceptance: apply flows unchanged; wallpaper never set without confirmation.

### R8 — Settings family restyle
**Status:** Not started · **Effort:** M

- Settings list with `SettingsRow` icon tiles.
- Appearance: Themes carousel → **Theme Preview** screen (new): big preview, Dark / Light /
  System, Dynamic Color toggle (Material You), **Apply Theme**. Accent Color dots. Widget Style
  (Looks) grid.
- Permissions: rows with switches (switch = granted; tapping asks). Keep the first-launch version.
- Weather: merge "Weather location" settings with the Weather detail sheet into one screen:
  connection toggle, location row, current weather card, update interval, **Test Connection**
  (fetches once and shows result). City chips (Mumbai / London / NY / Tokyo) stay in the card.
- About: dot logo, version, links (Terms, Privacy, Open-source licences), data sources.
- Widget Status: `StatusHero` with green check "All widgets working" or amber "N widgets need
  attention", last checked, **Check Again** (forces a refresh of all placed widgets).
- Acceptance: every setting from today still exists; `SettingsPagesTest` updated and green.

### R9 — Premium (only if D-1 = B)
**Status:** Parked (waiting on D-1) · **Effort:** L

- Go Premium (benefits list, Monthly / Yearly cards, Start Free Trial), Manage Purchases
  (Restore), Subscription (status), crown badges on premium items.
- Needs: Play Billing Library, products in Play Console, `Entitlements` real implementation,
  what is free vs premium (proposal: all 233 widgets free; premium = extra theme packs, setups
  and future illustrated collection), Play subscription policy checks, privacy policy update.
- Not started until decided.

### R10 — Polish + release
**Status:** Not started · **Effort:** S–M

- Light theme pass on every screen; font scale 130 % and 200 %; landscape; tablets (≥ 600 dp).
- TalkBack labels for new components (ties to P17).
- New store screenshots from the real app (reuse `screenshots/` flow).
- Update `docs/UI_AUDIT.md` with the verification record.

---

## 4. What we keep exactly as is (do not touch)

- All 233 widget renderers, `WidgetRenderEngine`, Smart Stack rotation, lock-screen providers.
- `WidgetSetupState` / config JSON — only the UI around it changes.
- Photo background, placement editor, dot filter (P1).
- Setup share codes, community picks, wallpaper apply logic.
- Permissions logic and first-launch flow.
- Package name, signing, version scheme.

---

## 5. Risks and how we handle them

| Risk | Handling |
|---|---|
| Mockups are AI-made: garbled text, inconsistent nav/tabs between boards | Section 0 defaults fix the choices; the board is a direction, not a spec |
| "Same to same" expectations | Build R1 + R2 first, install on the phone, review together before the rest |
| Regressions in setup sheet (most complex screen) | R4 keeps state/logic; existing UI tests updated per step; device test run before each install |
| Premium vs free policy conflict | R9 parked until D-1 is answered |
| Copyright | Nothing is copied from the third-party images (`00nHSKSr…jpg`, `CPb7M7…jpg`); they are style references only. Widget-style work from them lives in `DESIGN_QUALITY_PLAN.md` D3–D7 |
| Claims in the UI ("350+", ratings) | Only real numbers (233 widgets); no fake ratings or download counts |

---

## 6. Estimate

| Step | Effort |
|---|---|
| Design system (§2) | 2 days |
| R1 Nav + Home | 2–3 days |
| R2 Explore | 2 days |
| R3 Details | 1 day |
| R4 Customizer | 3–4 days |
| R5 My Widgets | 2–3 days |
| R6 Builder | 1–2 days |
| R7 Setups + Wallpapers | 2 days |
| R8 Settings family | 3 days |
| R10 Polish + release | 2 days |
| **Total (free)** | **≈ 3 weeks** |
| R9 Premium | + 1 week |

---

## 7. Progress

| Step | Status | Commit |
|---|---|---|
| §2 Design system | Done (component test pending) | |
| R1 | Done | |
| R2 | Not started | |
| R3 | Not started | |
| R4 | Not started | |
| R5 | Partial (actions pending) | |
| R6 | Not started | |
| R7 | Not started | |
| R8 | Not started | |
| R9 | Parked (D-1) | |
| R10 | Not started | |

## Changelog

| Date | Item | Commit |
|---|---|---|
| 2026-11-14 | Plan written from the 4 reference images in `docs/design/` | — |
| 2026-11-14 | §2 tokens + v2 components; R1 Home + floating nav; R5 tab (partial) | — |
