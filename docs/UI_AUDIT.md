# Dot Widgets: UI audit and design system

Date: 2026-10-31. Project: Android, Kotlin, Jetpack Compose (not React Native, so the brief's RN and
TypeScript items were applied as their Compose equivalents).

Nothing OS is visual inspiration only: no Nothing logos or proprietary fonts are bundled, and the
app says it is independent. The colour values are this app's own tokens.

## 1. Measured before and after (`ui/`, `themes/ui/`, `MainActivity.kt`)

| Measure | Before | After |
|---|---|---|
| Fonts | Space Grotesk, JetBrains Mono | Geist, Geist Mono (OFL) |
| Hardcoded grey outlines and fills (`8A8A8A`) | 67 uses | 1 |
| Outline borders | 30 | 13 (selection rings and previews) |
| Corner sizes in use | 9 | 5 (4, 8, 12, 16, 24) |
| Text-field colour styles | Per screen | 1 (`dotFieldColors`) |
| Shared button and card components | None | `DotButton`, `DotCard` and others |
| All-caps labels | About 98 | Technical mono labels only |
| Surface levels | 2 | 4 (background, surface, secondary, elevated) |

## 2. Findings

| Screen | Problem | Severity | Fix | Files |
|---|---|---|---|---|
| All | No token system; colours written inline | High | `DotColors`, `DotTheme`, Material role mapping | `ui/theme/DesignTokens.kt`, `Theme.kt` |
| All | Flat: page and cards nearly the same black, cards told apart by grey outlines | High | Four surface levels on #101010 and #F5F5F3; outlines removed | `DesignTokens.kt`, all screens |
| All | Nearly everything in capitals, so hierarchy was lost | High | Sentence case; mono caps only for small technical labels | All screens |
| All | Weak type scale | High | Geist scale with distinct display, heading, body and label roles | `Type.kt` |
| All | Lime status colour fought the brand red | Medium | Muted semantic success, info, warning and danger colours | `DesignTokens.kt` |
| Shell | Battery chip and nav bar outlined; all nav labels capitals; state shown by colour only | Medium | Quiet chip, hairline divider, outlined/filled icons, crossfade between tabs | `MainActivity.kt` |
| Widgets | Section titles were tiny labels; decorative stars | Medium | Heading-sized sections, plain chips, shared card with press feedback | `BrowserScreen.kt` |
| Setups, Wallpapers | Outlines on previews and cards | Medium | Surface tones, consistent corners | `SetupsScreen.kt`, `WallpapersScreen.kt` |
| Settings | Long mixed list | High | Fixed earlier: menu with one page per group | `SettingsScreen.kt` |
| Permissions | Plain first screen | Medium | Faint dot-grid backdrop behind the welcome | `PermissionsScreen.kt` |
| Customize | Label style differed from the new scale | Low | Follows tokens automatically | `SetupControls.kt` |

## 3. Design system

**Colour.** Semantic tokens in `DesignTokens.kt`, for dark and light. Brand red is #D71920. Red means
selection and the main action. On the dark theme, accent text uses a lightened value to reach 4.5:1,
while filled controls keep the true red (3:1 for non-text).

**Type.** Display Large/Medium, Heading Large/Medium/Small, Title, Body Large/Medium/Small, Label
Large, mono Label Small, and `DotType.NumericDisplay`/`NumericSmall`.

**Spacing and shape.** 4, 8, 12, 16, 24, 32 dp; corners 8, 12, 16, 24 dp. `DotSpacing`, `DotShape`.

**Icons.** Material icons at 16, 20 and 24 dp (`DotIconSize`). Navigation uses outlined when idle and
filled when selected. Theme widgets and the icon pack use the same set.

**Components (`ui/components/DotComponents.kt`).** `DotButton` (primary, secondary, ghost, destructive,
loading), `DotCard` (standard, elevated, information, interactive), `DotChip`, `DotBadge`, `DotLabel`,
`DotStat`, `DotSkeleton`, `DotMessage`, `DotMatrixBackdrop`, `dotFieldColors`, `pressScale`.

Note: this brief's Display/Heading scale replaces the earlier "4 sizes, 2 weights" rule from the
mobile design skill. Weights are Regular, Medium and SemiBold.

## 4. Verification

See the release notes for the commit. Unit tests are the record of what passed; screens were checked
as release-build screenshots on a Nothing Phone (3a) Pro in dark mode and as local renders in light mode.

## 5. Not finished

- 14 Material `Button`s keep their themed colours rather than moving to `DotButton`; only new code uses it.
- `DotMessage`, `DotStat` and `DotSkeleton` exist but screens don't all use them yet.
- Some outlines remain where a shape must show on its own tone (selection rings, previews).
- Permission rows still use the red accent for their icons.
- Widget skin controls still show for theme widgets, where they do nothing.
- Not tested: small phones, tablets, landscape, large font scale, TalkBack.
- Widgets themselves keep their current drawing; this pass covers the app's screens.
