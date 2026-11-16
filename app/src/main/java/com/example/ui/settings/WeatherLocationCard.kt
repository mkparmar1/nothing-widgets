package com.example.ui.settings

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.core.theme.AccentOption
import com.example.data.weather.WeatherPlace
import com.example.data.weather.WeatherRepository
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotCard
import com.example.ui.components.DotLabel
import com.example.ui.components.dotFieldColors
import com.example.ui.permissions.AppPermission
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import kotlinx.coroutines.launch

/** Where weather and sunrise widgets get their data: device location, a searched city, or the time-zone guess. */
@Composable
fun WeatherLocationCard(accent: AccentOption) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var place by remember { mutableStateOf(WeatherRepository.currentPlace(context)) }
    var chosen by remember { mutableStateOf(WeatherRepository.hasChosenPlace(context)) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherPlace>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }

    fun choose(p: WeatherPlace?) {
        WeatherRepository.setPlace(context, p)
        place = WeatherRepository.currentPlace(context)
        chosen = p != null
        results = emptyList()
        query = ""
    }

    fun useDeviceLocation() {
        busy = true
        scope.launch {
            val p = WeatherRepository.placeFromDevice(context)
            busy = false
            if (p != null) choose(p) else Toast.makeText(context, "Couldn't get your location. Turn on Location and try again.", Toast.LENGTH_LONG).show()
        }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useDeviceLocation()
    }

    fun search() {
        if (query.isBlank()) return
        busy = true
        scope.launch {
            results = WeatherRepository.searchPlaces(query)
            busy = false
            if (results.isEmpty()) Toast.makeText(context, "No cities found for \"$query\"", Toast.LENGTH_SHORT).show()
        }
    }

    DotCard(modifier = Modifier.fillMaxWidth().testTag("weather_location_card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                DotLabel("Weather")
                Text("Location", style = DotType.CardTitle, color = DotTheme.colors.textPrimary)
            }
        }
        Spacer(Modifier.height(DotSpacing.md))
        Text(
            text = buildString {
                append(place.name)
                if (place.region.isNotBlank()) append(" • ${place.region.uppercase()}")
                if (!chosen) append(" (from time zone)")
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(DotSpacing.sm))
        DotButton(
            text = "Use my location",
            onClick = {
                if (AppPermission.Location.isGranted(context)) useDeviceLocation()
                else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            enabled = !busy,
            icon = Icons.Default.MyLocation,
            style = DotButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth().testTag("use_my_location")
        )
        Spacer(Modifier.height(DotSpacing.sm))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Or search a city") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
            trailingIcon = {
                TextButton(onClick = { search() }, enabled = !busy) {
                    Text("Search", color = MaterialTheme.colorScheme.primary)
                }
            },
            colors = dotFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("city_search")
        )

        results.forEach { result ->
            Text(
                text = "${result.name}${if (result.region.isNotBlank()) " — ${result.region}" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = DotTheme.colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { choose(result) }
                    .padding(horizontal = DotSpacing.sm, vertical = DotSpacing.sm)
                    .testTag("city_result_${result.name.lowercase().replace(" ", "_")}")
            )
        }

        if (chosen) {
            DotButton(
                text = "Use time zone instead",
                onClick = { choose(null) },
                style = DotButtonStyle.Ghost,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
