package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.theme.AccentOption
import com.example.ui.components.headerIconTint
import com.example.ui.components.tonalButtonColors
import com.example.data.home.HomeAssistantRepository
import com.example.data.sports.SportsRepository
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

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(20.dp))
            .testTag("connections_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Link, contentDescription = null, tint = headerIconTint())
                Text("CONNECTIONS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Text("HOME ASSISTANT", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                "For the Smart Home widgets. Your token is stored encrypted on this phone, is sent only to your server, and is never backed up.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            OutlinedTextField(
                value = haUrl,
                onValueChange = { haUrl = it },
                label = { Text("Server URL, e.g. http://homeassistant.local:8123") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth().testTag("ha_url")
            )
            OutlinedTextField(
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
                    color = Color(0xFFE0A030)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
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
                    colors = tonalButtonColors(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("ha_save")
                ) { Text("SAVE") }
                if (haConnected) {
                    TextButton(onClick = {
                        HomeAssistantRepository.clearServer(context)
                        haUrl = ""; haToken = ""; haConnected = false
                        redraw()
                    }) { Text("DISCONNECT", color = MaterialTheme.colorScheme.primary) }
                }
            }

            Text("SPORTS FEED", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                "For the Team Score, Fixture, Table, Form, Cricket and Tennis widgets. Dot Widgets has no built-in score service: add a URL that returns your team's data as JSON.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            OutlinedTextField(
                value = feedUrl,
                onValueChange = { feedUrl = it },
                label = { Text("Feed URL (https://…)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth().testTag("sports_feed_url")
            )
            Button(
                onClick = {
                    SportsRepository.setFeedUrl(context, feedUrl)
                    redraw()
                    Toast.makeText(context, if (feedUrl.isBlank()) "Sports feed removed" else "Sports feed saved", Toast.LENGTH_SHORT).show()
                },
                colors = tonalButtonColors(MaterialTheme.colorScheme.primary)
            ) { Text("SAVE FEED") }
        }
    }
}
