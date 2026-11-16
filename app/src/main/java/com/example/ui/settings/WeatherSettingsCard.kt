package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.theme.AccentOption
import com.example.data.weather.WeatherData
import com.example.data.weather.WeatherRepository
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotCard
import com.example.ui.components.DotChip
import com.example.ui.components.DotLabel
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import kotlinx.coroutines.launch

/**
 * Weather settings merged with the detail view: on/off, current conditions (with city chips),
 * refresh cadence and a one-tap connection test. Location search stays in [WeatherLocationCard].
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun WeatherSettingsCard(accent: AccentOption) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var network by remember { mutableStateOf(WeatherRepository.isNetworkEnabled(context)) }
    var minutes by remember { mutableStateOf(WeatherRepository.refreshMinutes(context)) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<WeatherData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val colors = DotTheme.colors
    val myPlace = remember { WeatherRepository.currentPlace(context).name }
    val cities = remember(myPlace) {
        listOf<String?>(null) + listOf("Mumbai", "London", "New York", "Tokyo").filter { it != myPlace }
    }

    fun load() {
        loading = true
        val city = selectedCity
        if (!network) {
            data = WeatherRepository.getWeatherInstant(context, city)
            loading = false
            return
        }
        scope.launch {
            data = WeatherRepository.getWeather(context, city)
            loading = false
        }
    }

    LaunchedEffect(network, selectedCity) { load() }

    DotCard(modifier = Modifier.fillMaxWidth().testTag("weather_settings_card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
            Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                DotLabel("Weather")
                Text("Conditions & updates", style = DotType.CardTitle, color = colors.textPrimary)
            }
            Switch(
                checked = network,
                onCheckedChange = {
                    network = it
                    WeatherRepository.setNetworkEnabled(context, it)
                    if (!it) testResult = null
                },
                modifier = Modifier.testTag("weather_network_switch")
            )
        }
        Text(
            if (network) "Weather widgets refresh from Open-Meteo. Turn off to keep the last cached reading."
            else "Network refresh is off. Widgets show the last cached reading until you turn it back on.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )

        Spacer(Modifier.height(DotSpacing.md))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
        ) {
            cities.forEach { city ->
                DotChip(
                    label = city ?: myPlace,
                    selected = selectedCity == city,
                    onClick = { selectedCity = city },
                    modifier = Modifier.testTag(
                        "weather_city_" + (city?.lowercase()?.replace(" ", "_") ?: "my")
                    )
                )
            }
        }

        Spacer(Modifier.height(DotSpacing.md))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DotShape.large)
                .background(colors.surfaceSecondary)
                .padding(DotSpacing.lg)
        ) {
            val current = data
            when {
                loading && current == null -> Text("Fetching…", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                current == null -> Text("No weather data yet.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                else -> Column(verticalArrangement = Arrangement.spacedBy(DotSpacing.xs)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (current.isStale) "—°C" else "${current.temperatureC}°C",
                            style = DotType.Hero,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        DotLabel(if (current.isStale) "Cached" else "Live")
                    }
                    Text(
                        "${current.cityName} · ${current.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        "High ${current.highC}°  Low ${current.lowC}°  ·  Humidity ${current.humidityPct}%  ·  Wind ${current.windKmh} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(DotSpacing.md))
        Text("Update interval", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(
            "How often widgets may fetch new weather in the background.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(DotSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
            listOf(15, 30, 60).forEach { option ->
                DotChip(
                    label = "$option min",
                    selected = minutes == option,
                    onClick = {
                        minutes = option
                        WeatherRepository.setRefreshMinutes(context, option)
                    },
                    modifier = Modifier.testTag("weather_interval_$option")
                )
            }
        }

        Spacer(Modifier.height(DotSpacing.md))
        DotButton(
            text = "Test connection",
            onClick = {
                testing = true
                testResult = null
                scope.launch {
                    val result = WeatherRepository.getWeather(context, selectedCity)
                    testing = false
                    testResult = if (result.isOffline || result.isStale) {
                        "Couldn't reach Open-Meteo. Check your connection and try again."
                    } else {
                        "Connected — ${result.cityName}, ${result.temperatureC}°C."
                    }
                }
            },
            icon = Icons.Default.Refresh,
            style = DotButtonStyle.Secondary,
            enabled = network,
            loading = testing,
            modifier = Modifier.fillMaxWidth().testTag("weather_test_connection")
        )
        testResult?.let {
            Spacer(Modifier.height(DotSpacing.sm))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (it.startsWith("Connected")) colors.success else colors.warning,
                modifier = Modifier.testTag("weather_test_result")
            )
        }
    }
}
