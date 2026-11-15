package com.example.ui.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.DotBadge
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotCard
import com.example.ui.components.DotCardStyle
import com.example.ui.components.DotChip
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.config.WidgetSetupState
import com.example.ui.config.pinWidgetToHomeScreen
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.launch

/**
 * The v2 widget detail page: a large preview across every size the widget supports, the real
 * description and badges, the access it needs, and one primary "Add to Home Screen" action.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetDetailsScreen(
    definition: WidgetDefinition,
    onBack: () -> Unit,
    onCustomize: (WidgetDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val accent by prefs.accent.collectAsState()
    val scope = rememberCoroutineScope()

    val sizes = remember(definition.id) { definition.sizes.toList() }
    val pagerState = rememberPagerState(pageCount = { sizes.size })
    val currentSize = sizes.getOrElse(pagerState.currentPage) { sizes.first() }

    val permissions = remember(definition.id) {
        definition.requiredPermissions.filter { it.isApplicable() }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = DotSpacing.xl)
            .testTag("widget_details_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DotSpacing.sm, vertical = DotSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("details_back")) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Explore",
                    tint = DotTheme.colors.textPrimary
                )
            }
            Text(
                text = definition.category.title,
                style = MaterialTheme.typography.labelLarge,
                color = DotTheme.colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            if (definition.lockSafe) {
                DotBadge("LOCK SCREEN READY", modifier = Modifier.padding(end = DotSpacing.md))
            }
        }

        DotCard(
            style = DotCardStyle.Elevated,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DotSpacing.lg)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Keep the square previews inside the card on phones and tablets alike.
                val previewWidth = minOf(maxWidth * 0.72f, maxHeight)
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val size = sizes[page]
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        WidgetBitmapPreview(
                            definition = definition,
                            spec = previewSpec(context, definition, size, accent),
                            modifier = Modifier.width(previewWidth)
                        )
                    }
                }
            }
            if (sizes.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = DotSpacing.md),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sizes.indices.forEach { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(if (selected) 18.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primary else DotTheme.colors.border)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(start = DotSpacing.lg, top = DotSpacing.lg, end = DotSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
        ) {
            Text(definition.title, style = DotType.Section, color = DotTheme.colors.textPrimary)
            Text(
                text = definition.description,
                style = MaterialTheme.typography.bodyMedium,
                color = DotTheme.colors.textSecondary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(DotSpacing.xs)
            ) {
                DotBadge(definition.category.title)
                DotBadge("Dot matrix")
                if (definition.lockSafe) DotBadge("Lock screen")
                if (definition.requiresNetwork) DotBadge("Live data")
                if (definition.options.isNotEmpty()) DotBadge("Options")
            }
        }

        if (sizes.size > 1) {
            Column(
                modifier = Modifier.padding(start = DotSpacing.lg, top = DotSpacing.xl, end = DotSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
            ) {
                Text("Size", style = DotType.CardTitle, color = DotTheme.colors.textPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
                ) {
                    sizes.forEachIndexed { index, size ->
                        DotChip(
                            label = "${size.label} ${size.cells}",
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            modifier = Modifier.testTag("details_size_${size.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (permissions.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = DotSpacing.lg, top = DotSpacing.xl, end = DotSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
            ) {
                Text("Needs access", style = DotType.CardTitle, color = DotTheme.colors.textPrimary)
                DotCard(style = DotCardStyle.Standard, padding = DotSpacing.md) {
                    permissions.forEachIndexed { index, permission ->
                        val granted = permission.isGranted(context)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = DotSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                permission.icon,
                                contentDescription = null,
                                tint = DotTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(DotSpacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    permission.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DotTheme.colors.textPrimary
                                )
                                Text(
                                    permission.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DotTheme.colors.textSecondary
                                )
                            }
                            Spacer(Modifier.width(DotSpacing.sm))
                            Text(
                                text = if (granted) "Granted" else "Required",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (granted) DotTheme.colors.success else DotTheme.colors.warning
                            )
                        }
                        if (index != permissions.lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(DotTheme.colors.divider)
                            )
                        }
                    }
                }
            }
        }

        DotCard(
            style = DotCardStyle.Information,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DotSpacing.lg, vertical = DotSpacing.xl)
        ) {
            Text(
                text = "Add a widget: long-press your home screen, then Widgets, then Dot Widgets.",
                style = MaterialTheme.typography.bodySmall,
                color = DotTheme.colors.textSecondary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DotSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
        ) {
            DotButton(
                text = "Add to Home Screen",
                onClick = {
                    scope.launch {
                        pinWidgetToHomeScreen(
                            context,
                            definition,
                            WidgetSetupState.defaults(definition, prefs, lockedSize = currentSize)
                        )
                    }
                },
                icon = Icons.Default.Add,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("details_add_home")
            )
            DotButton(
                text = "Customize",
                onClick = { onCustomize(definition) },
                style = DotButtonStyle.Secondary,
                icon = Icons.Default.Tune,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("details_customize")
            )
        }
    }
}

@Composable
private fun previewSpec(
    context: android.content.Context,
    definition: WidgetDefinition,
    size: SizeBucket,
    accent: AccentOption
): WidgetSpec {
    val dims = remember(definition.id, size) { SizeResolver.resolveDimensions(context, size) }
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
