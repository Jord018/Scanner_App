package com.LingTH.fridge.sortandfilter.Setting

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Email(
    label: String,
    productName: String,
    onProductNameChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisible: () -> Unit
) {
    OutlinedTextField(
        value = productName,
        onValueChange = onProductNameChange,
        label = { Text(label) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}
