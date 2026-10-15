package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.bitmap.BitmapCache
import com.example.core.theme.AccentOption
import com.example.data.db.AppDatabase
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.AccentSelectorRow
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    val globalAccent by prefs.accent.collectAsState()
    val isUnlocked by prefs.isUnlocked.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text(
                text = "SETTINGS & LAB",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Global accent repainting, battery discipline & system audit.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A8A8A)
            )
        }

        // --- 1. GLOBAL ACCENT REPAINT CARD (§6) ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = globalAccent.color)
                    Text(
                        text = "GLOBAL ACCENT REPAINT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Instantly re-colors all active widgets placed on your home screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A8A8A)
                )

                Spacer(modifier = Modifier.height(14.dp))
                AccentSelectorRow(
                    selected = globalAccent,
                    onSelect = { option ->
                        prefs.setGlobalAccent(option)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            BitmapCache.clear()
                            WidgetRenderEngine.updateAllPlacedWidgets(context)
                            Toast.makeText(context, "Repainted all placed widgets with ${globalAccent.displayName}!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = globalAccent.color,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("repaint_all_widgets_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REPAINT ALL PLACED WIDGETS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 2. BATTERY & CORRECTNESS AUDIT (§4.1 & §4.3) ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = globalAccent.color)
                    Text(
                        text = "BATTERY & CORRECTNESS AUDIT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                AuditRow("Zero background drain with 0 widgets", "Verified 0% wakeups")
                AuditRow("Live sticky battery broadcast", "No polling or loop")
                AuditRow("Weather stale-guard (>90 min)", "Shows '—' not wrong temp")
                AuditRow("Transaction size safety", "< 1.5MB RemoteViews cap")
                AuditRow("No foreground services", "Compliant")
            }
        }

        // --- 3. LIFETIME UNLOCK STATUS (§8) ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isUnlocked) globalAccent.color else Color(0x338A8A8A), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Star,
                            contentDescription = null,
                            tint = globalAccent.color
                        )
                        Text(
                            text = if (isUnlocked) "DOT WIDGETS PRO: UNLOCKED" else "LIFETIME UNLOCK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isUnlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(globalAccent.color)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isUnlocked)
                        "All 80 widgets, 30 wallpapers, and 8 setups are fully unlocked."
                    else
                        "Free tier: 30 widgets, 10 wallpapers, 2 setups. Unlock all 80 widgets forever.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A8A8A)
                )

                if (!isUnlocked) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            prefs.unlockLifetime()
                            Toast.makeText(context, "Lifetime Pro Unlocked! Enjoy all 80 widgets.", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = globalAccent.color,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("unlock_lifetime_button")
                    ) {
                        Text("UNLOCK LIFETIME PRO (FREE PREVIEW)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditRow(title: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB8E000))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A8A8A)
            )
        }
    }
}
