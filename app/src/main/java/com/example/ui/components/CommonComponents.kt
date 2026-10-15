package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.bitmap.SizeBucket
import com.example.core.theme.AccentOption

@Composable
fun AccentSelectorRow(
    selected: AccentOption,
    onSelect: (AccentOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentOption.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(option.color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelect(option) }
                    .testTag("accent_option_${option.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (option == AccentOption.PURE) Color.Black else Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun SizeBadge(
    size: SizeBucket,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(4.dp))
            .background(Color(0x1A8A8A8A))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = size.cells,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
