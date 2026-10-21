package com.example.ui.settings

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.core.theme.AccentOption
import com.example.data.weather.WeatherPlace
import com.example.data.weather.WeatherRepository
import com.example.ui.permissions.AppPermission
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

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(20.dp))
            .testTag("weather_location_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = accent.color)
                Text("WEATHER LOCATION", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = buildString {
                    append(place.name.uppercase())
                    if (place.region.isNotBlank()) append(" • ${place.region.uppercase()}")
                    if (!chosen) append(" (FROM TIME ZONE)")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = accent.color
            )

            Button(
                onClick = {
                    if (AppPermission.Location.isGranted(context)) useDeviceLocation()
                    else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                enabled = !busy,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = com.example.ui.theme.contentColorOn(accent.color)),
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("use_my_location")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Text("  USE MY LOCATION", fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Or search a city") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                trailingIcon = { TextButton(onClick = { search() }, enabled = !busy) { Text("SEARCH", color = accent.color) } },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent.color, cursorColor = accent.color, focusedLabelColor = accent.color),
                modifier = Modifier.fillMaxWidth().testTag("city_search")
            )

            results.forEach { result ->
                Text(
                    text = "${result.name}${if (result.region.isNotBlank()) " — ${result.region}" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { choose(result) }
                        .padding(vertical = 8.dp)
                )
            }

            if (chosen) {
                TextButton(onClick = { choose(null) }) {
                    Text("USE TIME ZONE INSTEAD", color = Color(0xFF8A8A8A))
                }
            }
        }
    }
}
