package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.theme.AccentOption
import com.example.data.home.HomeAssistantRepository
import com.example.data.sports.SportsRepository
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotCard
import com.example.ui.components.DotLabel
import com.example.ui.components.dotFieldColors
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.launch

/** Optional servers the Smart Home and Sports widgets read from. Nothing here is required. */
@Composable
fun ConnectionsCard(accent: AccentOption) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var haUrl by remember { mutableStateOf(HomeAssistantRepository.baseUrl(context)) }
    var haToken by remember { mutableStateOf(HomeAssistantRepository.token(context)) }
    var haConnected by remember { mutableStateOf(HomeAssistantRepository.isConfigured(context)) }
    var feedUrl by remember { mutableStateOf(SportsRepository.feedUrl(context)) }

    fun redraw() = scope.launch { WidgetRenderEngine.updateAllPlacedWidgets(context.applicationContext, force = true) }

    DotCard(modifier = Modifier.fillMaxWidth().testTag("connections_card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                DotLabel("Connections")
                Text("Optional servers", style = DotType.CardTitle, color = DotTheme.colors.textPrimary)
            }
        }
        Text(
            "Nothing here is required. Add a server only if you use the widgets that read from it.",
            style = MaterialTheme.typography.bodySmall,
            color = DotTheme.colors.textSecondary
        )

        Spacer(Modifier.height(DotSpacing.md))
        Text("Home Assistant", style = MaterialTheme.typography.titleMedium, color = DotTheme.colors.textPrimary)
        Text(
            "For the Smart Home widgets. Your token is stored encrypted on this phone, is sent only to your server, and is never backed up.",
            style = MaterialTheme.typography.bodySmall,
            color = DotTheme.colors.textSecondary
        )
        Spacer(Modifier.height(DotSpacing.sm))
        OutlinedTextField(
            colors = dotFieldColors(),
            value = haUrl,
            onValueChange = { haUrl = it },
            label = { Text("Server URL, e.g. http://homeassistant.local:8123") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth().testTag("ha_url")
        )
        Spacer(Modifier.height(DotSpacing.sm))
        OutlinedTextField(
            colors = dotFieldColors(),
            value = haToken,
            onValueChange = { haToken = it },
            label = { Text("Long-lived access token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("ha_token")
        )
        if (haUrl.trim().startsWith("http://")) {
            Text(
                "http:// is unencrypted: only use it on your home network.",
                style = MaterialTheme.typography.bodySmall,
                color = DotTheme.colors.warning
            )
        }
        Spacer(Modifier.height(DotSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            DotButton(
                text = "Save",
                onClick = {
                    val url = haUrl.trim()
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        Toast.makeText(context, "The URL must start with http:// or https://", Toast.LENGTH_LONG).show()
                    } else if (haToken.isBlank()) {
                        Toast.makeText(context, "Paste a long-lived access token from your Home Assistant profile", Toast.LENGTH_LONG).show()
                    } else {
                        HomeAssistantRepository.setServer(context, url, haToken)
                        haConnected = true
                        redraw()
                        Toast.makeText(context, "Home Assistant saved", Toast.LENGTH_SHORT).show()
                    }
                },
                style = DotButtonStyle.Secondary,
                modifier = Modifier.weight(1f).testTag("ha_save")
            )
            if (haConnected) {
                DotButton(
                    text = "Disconnect",
                    onClick = {
                        HomeAssistantRepository.clearServer(context)
                        haUrl = ""
                        haToken = ""
                        haConnected = false
                        redraw()
                    },
                    style = DotButtonStyle.Ghost,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(DotSpacing.md))
        Text("Sports feed", style = MaterialTheme.typography.titleMedium, color = DotTheme.colors.textPrimary)
        Text(
            "For the Team Score, Fixture, Table, Form, Cricket and Tennis widgets. Add a URL returning JSON with team, opponent, league, homeScore, awayScore, live, kickoffMs and form fields. An optional table array can include team, points and played.",
            style = MaterialTheme.typography.bodySmall,
            color = DotTheme.colors.textSecondary
        )
        Spacer(Modifier.height(DotSpacing.sm))
        OutlinedTextField(
            colors = dotFieldColors(),
            value = feedUrl,
            onValueChange = { feedUrl = it },
            label = { Text("Feed URL (https://…)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth().testTag("sports_feed_url")
        )
        Spacer(Modifier.height(DotSpacing.sm))
        DotButton(
            text = "Save feed",
            onClick = {
                SportsRepository.setFeedUrl(context, feedUrl)
                redraw()
                Toast.makeText(context, if (feedUrl.isBlank()) "Sports feed removed" else "Sports feed saved", Toast.LENGTH_SHORT).show()
            },
            style = DotButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
