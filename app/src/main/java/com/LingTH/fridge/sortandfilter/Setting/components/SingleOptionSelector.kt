package com.LingTH.fridge.sortandfilter.Setting.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleOptionSelector(
    title: String,
    options: List<String>,
    selectedOption: String, // เปลี่ยนชื่อเพื่อสื่อความหมายว่าเลือกได้แค่ 1 อัน
    onOptionToggle: (String?) -> Unit // nullable เพื่อยกเลิกการเลือก
) {
    SelectorPanel(title) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            onOptionToggle(null) // ยกเลิกการเลือก
                        } else {
                            onOptionToggle(option)
                        }
                    },
                    label = { Text(option) },
                    leadingIcon = selectedCheckIcon(isSelected)
                )
            }
        }
    }
}

// Shared container for the chip pickers that expand under a SettingsItem
@Composable
internal fun SelectorPanel(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

internal fun selectedCheckIcon(selected: Boolean): (@Composable () -> Unit)? =
    if (selected) {
        {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        }
    } else null
