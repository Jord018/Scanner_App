package com.LingTH.fridge.sortandfilter.Setting.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptionSelector(
    title: String,
    options: List<String>,
    selectedOptions: List<String>,
    onOptionToggle: (String) -> Unit
) {
    SelectorPanel(title) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selectedOptions.contains(option),
                    onClick = { onOptionToggle(option) },
                    label = { Text(option) },
                    leadingIcon = selectedCheckIcon(selectedOptions.contains(option))
                )
            }
        }
    }
}
