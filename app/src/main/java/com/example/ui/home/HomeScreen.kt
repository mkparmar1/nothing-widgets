package com.example.ui.home

import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.prefs.PreferencesManager
import com.example.data.setups.SetupRepository
import com.example.data.wallpapers.WallpaperManagerHelper
import com.example.ui.browse.CatalogueHighlights
import com.example.ui.components.DotBrand
import com.example.ui.components.DotMark
import com.example.ui.components.DotSetupCard
import com.example.ui.components.HeroCard
import com.example.ui.components.QuickAccessTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.components.WidgetCard
import com.example.ui.config.WidgetConfigSheet
import com.example.ui.theme.DotIconSize
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class HeroPage(
    val title: String,
    val subtitle: String,
    val cta: String,
    val previewId: String,
    val onOpen: () -> Unit
)

@Composable
fun HomeScreen(
    onExplore: () -> Unit,
    onBuilder: () -> Unit,
    onSetups: () -> Unit,
    onWallpapers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val accent by prefs.accent.collectAsState()
    val scope = rememberCoroutineScope()
    var configuring by remember { mutableStateOf<WidgetDefinition?>(null) }

    val reduceMotion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }

    val pages = listOf(
        HeroPage(
            title = "Make your home screen smarter",
            subtitle = "233 dot-matrix widgets, ready in one tap.",
            cta = "Explore Widgets",
            previewId = "clock.glyph.ring",
            onOpen = onExplore
        ),
        HeroPage(
            title = "Build a widget that is yours",
            subtitle = "Compose time, date, battery and more on a dot grid.",
            cta = "Open Builder",
            previewId = "clock.dotdigital.s",
            onOpen = onBuilder
        ),
        HeroPage(
            title = "One tap, a whole look",
            subtitle = "Wallpaper, accent and widgets, matched for you.",
            cta = "Browse Setups",
            previewId = "sensor.glyph.lights",
            onOpen = onSetups
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(5_000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % pages.size)
        }
    }

    val featured = remember { CatalogueHighlights.EXCLUSIVE.mapNotNull(WidgetRegistry::getById) }
    val popularSetup = remember { SetupRepository.SETUPS.firstOrNull() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = DotSpacing.xl)
    ) {
        HomeTopBar(accent = accent.color)

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = DotSpacing.lg),
            pageSpacing = DotSpacing.md,
            modifier = Modifier.fillMaxWidth().height(232.dp)
        ) { page ->
            val item = pages[page]
            val definition = remember(item.previewId) { WidgetRegistry.getById(item.previewId) }
            HeroCard(
                title = item.title,
                subtitle = item.subtitle,
                ctaLabel = item.cta,
                onCta = item.onOpen,
                modifier = Modifier.fillMaxSize(),
                preview = definition?.let { def ->
                    {
                        HeroPreview(definition = def, accent = accent)
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = DotSpacing.md),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (selected) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (selected) accent.color else DotTheme.colors.border)
                )
            }
        }

        Spacer(Modifier.height(DotSpacing.xl))
        SectionHeader(
            title = "Quick access",
            modifier = Modifier.padding(horizontal = DotSpacing.lg)
        )
        Spacer(Modifier.height(DotSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DotSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
        ) {
            QuickAccessTile(Icons.Outlined.Widgets, "Widgets", onExplore, Modifier.weight(1f), selected = true)
            QuickAccessTile(Icons.Outlined.Tune, "Builder", onBuilder, Modifier.weight(1f))
            QuickAccessTile(Icons.Outlined.AutoAwesome, "Setups", onSetups, Modifier.weight(1f))
            QuickAccessTile(Icons.Outlined.Wallpaper, "Wallpapers", onWallpapers, Modifier.weight(1f))
        }

        Spacer(Modifier.height(DotSpacing.xl))
        SectionHeader(
            title = "Featured widgets",
            subtitle = "Only in Dot Widgets",
            onSeeAll = onExplore,
            modifier = Modifier.padding(horizontal = DotSpacing.lg)
        )
        Spacer(Modifier.height(DotSpacing.md))
        LazyRow(
            contentPadding = PaddingValues(horizontal = DotSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(DotSpacing.md)
        ) {
            items(featured, key = { it.id }) { definition ->
                WidgetCard(
                    definition = definition,
                    onClick = { configuring = definition },
                    modifier = Modifier.width(196.dp),
                    accent = accent,
                    exclusive = definition.id in CatalogueHighlights.EXCLUSIVE
                )
            }
        }

        if (popularSetup != null) {
            Spacer(Modifier.height(DotSpacing.xl))
            SectionHeader(
                title = "Popular setups",
                onSeeAll = onSetups,
                modifier = Modifier.padding(horizontal = DotSpacing.lg)
            )
            Spacer(Modifier.height(DotSpacing.md))
            DotSetupCard(
                title = popularSetup.title,
                caption = popularSetup.tagline,
                accent = popularSetup.accent.color,
                onApply = {
                    scope.launch {
                        prefs.setGlobalAccent(popularSetup.accent)
                        WallpaperManagerHelper.applyWallpaper(
                            context,
                            WallpaperManagerHelper.forAccent(popularSetup.accent, popularSetup.wallpaperIndex)
                        )
                        Toast.makeText(context, "Applied '${popularSetup.title}'", Toast.LENGTH_SHORT).show()
                    }
                },
                onOpen = onSetups,
                modifier = Modifier.padding(horizontal = DotSpacing.lg),
                previews = {
                    popularSetup.widgetIds.take(3).forEach { id ->
                        WidgetRegistry.getById(id)?.let { def ->
                            Box(Modifier.width(72.dp)) {
                                WidgetBitmapPreview(definition = def, spec = miniSpec(def, accent))
                            }
                        }
                    }
                }
            )
        }
    }

    configuring?.let { definition ->
        WidgetConfigSheet(definition = definition, onDismiss = { configuring = null })
    }
}

@Composable
private fun HomeTopBar(accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DotSpacing.lg, vertical = DotSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DotLogo()
        Spacer(Modifier.width(DotSpacing.sm))
        Text("Dot Widgets", style = DotType.Section, color = DotTheme.colors.textPrimary)
        Spacer(Modifier.width(2.dp))
        Box(
            Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(accent)
        )
    }
}

/** Dot-matrix "D." mark, the app's logo, matching the launcher icon. */
@Composable
private fun DotLogo() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(DotShape.button)
            .background(DotTheme.colors.surfaceSecondary)
    ) {
        DotMark(
            modifier = Modifier.size(18.dp),
            dotColor = DotTheme.colors.textPrimary,
            accentColor = DotBrand.red
        )
    }
}

@Composable
private fun HeroPreview(definition: WidgetDefinition, accent: AccentOption) {
    WidgetBitmapPreview(
        definition = definition,
        spec = miniSpec(definition, accent),
        modifier = Modifier.width(132.dp)
    )
}

@Composable
private fun miniSpec(definition: WidgetDefinition, accent: AccentOption): WidgetSpec {
    val context = LocalContext.current
    val size = definition.sizes.first()
    val dims = remember(size) { SizeResolver.resolveDimensions(context, size) }
    return remember(definition.id, size, accent) {
        WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = size,
            accent = accent,
            bgMode = BackgroundMode.SOLID,
            themeMode = ThemeMode.AUTO,
            isDark = true,
            cornerRadiusDp = 20,
            showLabel = true,
            tapAction = definition.defaultTap
        )
    }
}
