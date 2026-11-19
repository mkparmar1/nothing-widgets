package com.example.ui.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.DotButton
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme

private const val TOUR_PREFS = "dot_widgets_onboarding"
private const val KEY_TOUR_SEEN = "tour_seen"

enum class TourTarget { HOME, EXPLORE, MY_WIDGETS, BUILDER, SETUPS, SETTINGS }

data class TourStep(
    val title: String,
    val body: String,
    val target: TourTarget,
    val icon: ImageVector
)

val APP_TOUR_STEPS: List<TourStep> = listOf(
    TourStep(
        title = "Home, at a glance",
        body = "Live previews and quick access to Builder, Setups and your recent widgets. Tap a widget to open it.",
        target = TourTarget.HOME,
        icon = Icons.Default.Home
    ),
    TourStep(
        title = "Explore 220+ widgets",
        body = "Browse dot-matrix clocks, weather, battery, calendar and more. Tap any widget to preview it, then add it to your home screen.",
        target = TourTarget.EXPLORE,
        icon = Icons.Default.Widgets
    ),
    TourStep(
        title = "Your widgets",
        body = "Everything you've placed or saved. Add it to your home screen, then use the menu to edit, duplicate or delete.",
        target = TourTarget.MY_WIDGETS,
        icon = Icons.Default.AutoAwesome
    ),
    TourStep(
        title = "Build your own",
        body = "Mix time, battery, weather and text. The live preview stays pinned while you adjust size, color and layout.",
        target = TourTarget.BUILDER,
        icon = Icons.Default.Add
    ),
    TourStep(
        title = "One-tap setups",
        body = "Apply a matching wallpaper and a curated widget set in a single tap, then tweak anything you like.",
        target = TourTarget.SETUPS,
        icon = Icons.Default.Wallpaper
    ),
    TourStep(
        title = "Make it yours",
        body = "Choose your accent, light or dark, and review permissions. Replay this tour any time from Settings, Help.",
        target = TourTarget.SETTINGS,
        icon = Icons.Default.Tune
    )
)

fun hasSeenAppTour(context: Context): Boolean =
    context.getSharedPreferences(TOUR_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_TOUR_SEEN, false)

fun markAppTourSeen(context: Context) {
    context.getSharedPreferences(TOUR_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_TOUR_SEEN, true)
        .apply()
}

@Composable
fun AppTourOverlay(
    step: TourStep,
    index: Int,
    count: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = DotTheme.colors
    val scrim = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(interactionSource = scrim, indication = null, onClick = {})
            .testTag("app_tour")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(DotSpacing.lg)
                .clip(DotShape.hero)
                .background(colors.surface)
                .padding(DotSpacing.xl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(DotShape.button)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(DotSpacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "STEP ${index + 1} OF $count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                }
            }
            Spacer(Modifier.height(DotSpacing.md))
            Text(
                text = step.body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(DotSpacing.lg))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(count) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == index) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= index) MaterialTheme.colorScheme.primary else colors.border
                            )
                    )
                }
            }
            Spacer(Modifier.height(DotSpacing.lg))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSkip, modifier = Modifier.testTag("app_tour_skip")) {
                    Text("Skip", color = colors.textSecondary)
                }
                Spacer(Modifier.weight(1f))
                if (index > 0) {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("app_tour_back")) {
                        Text("Back", color = colors.textPrimary)
                    }
                    Spacer(Modifier.width(DotSpacing.xs))
                }
                DotButton(
                    text = if (index == count - 1) "Finish" else "Next",
                    onClick = onNext,
                    modifier = Modifier.testTag("app_tour_next")
                )
            }
        }
    }
}
