package com.LingTH.fridge.sortandfilter

import Databases.ProductDao
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.LingTH.fridge.MainActivity2
import com.LingTH.fridge.sortandfilter.Setting.components.OptionSelector
import com.LingTH.fridge.sortandfilter.Setting.components.SettingsItem
import com.LingTH.fridge.sortandfilter.Setting.components.SingleOptionSelector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VisibleSelector {
    NONE,
    CATEGORY,
    Expired_in,
    Added,
    Added_photo,
    Expiration_Date,
    SortByName, SortByDate
}
// SandFscreen.kt
@Composable
fun SandFscreen(
    filterViewModel: FilterViewModel,
    navController: NavHostController,
) {

    var visibleSelector by remember { mutableStateOf(VisibleSelector.NONE) }
    val selectedCategory by filterViewModel.selectedCategory.collectAsState()
    val selectedExpiredIn by filterViewModel.selectedExpiredIn.collectAsState()
    val selectedAdded by filterViewModel.selectedAdded.collectAsState()
    val selectedAddedPhoto by filterViewModel.selectedAddedPhoto.collectAsState()
    val selectedExpirationDate by filterViewModel.selectedExpirationDate.collectAsState()
    val selectedSortByName by filterViewModel.selectedSortByName.collectAsState()
    val selectedSortByDate by filterViewModel.selectedSortByDate.collectAsState()
    val selectCategory = selectedCategory.joinToString(", ")
    val selectExpiredIn = selectedExpiredIn.joinToString(", ")
    val selectAdded = selectedAdded.joinToString(", ")
    val selectAddedPhoto = selectedAddedPhoto.joinToString(", ")
    val selectExpirationDate = selectedExpirationDate.joinToString(", ")
    val allProducts by filterViewModel.allProducts.collectAsState()

    val categories by filterViewModel.allCategories.collectAsState()
    val primaryCategories = allProducts.map { getPrimaryCategory(it.categories) }.distinct()

    val productsWithPhoto by filterViewModel.allProductsWithPhotos.collectAsState()
    val productsWithoutPhoto by filterViewModel.allProductsWithoutPhotos.collectAsState()
    val expirationDates by filterViewModel.allExpirationDates.collectAsState()
    val addedDates by filterViewModel.allAddedDates.collectAsState()

    val AddedPhotoOptions = listOfNotNull(
        if (productsWithPhoto.isNotEmpty()) "Added Photo" else null,
        if (productsWithoutPhoto.isNotEmpty()) "NO Photo" else null
    )

    val SortbyName = listOf(
        "Name (A-Z)",
        "Name (Z-A)",
    )
    val SortbyExpiration = listOf(
        "Expiration Date (Soonest)",
        "Expiration Date (Latest)",
    )
    val selectedSortOption by filterViewModel.selectedSortOption.collectAsState()

    val now = System.currentTimeMillis()

    val expiredInOptions = expirationDates
        .map { truncateToMidnight(it) }
        .distinct()
        .filter { it > now }
        .map { date ->
            val daysLeft = ((date - now) / (1000 * 60 * 60 * 24)).toInt()
            date to when {
                daysLeft >= 365 -> "Expired in ${daysLeft / 365} year(s)"
                daysLeft >= 30 -> "Expired in ${daysLeft / 30} month(s)"
                else -> "Expired in $daysLeft day(s)"
            }
        }


    val expiredInValues = expiredInOptions.map { it.first }
    val expiredInLabels = expiredInOptions.map { it.second }

    val expirationDateLabels = expirationDates
        .map { truncateToMidnight(it) }
        .distinct()
        .associateWith { timestamp ->
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }


    val validAddedDates = addedDates.filter { it > 0 }
    val AddedOptions = validAddedDates
        .map { truncateToMidnight(it) }
        .distinct()
        .filter { it <= now }
        .map { date ->
            val daysAgo = ((now - date) / (1000 * 60 * 60 * 24)).toInt()
            when {
                daysAgo >= 365 -> "Added ${daysAgo / 365} year(s) ago"
                daysAgo >= 30 -> "Added ${daysAgo / 30} month(s) ago"
                else -> "Added $daysAgo day(s) ago"
            }
        }


    val scrollState = rememberScrollState()

    BackHandler {

        navController.navigate("productList"){
            popUpTo("productList") { inclusive = true }
            launchSingleTop = true
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SectionHeader("Filter by")

        // Category
        SettingsItem("Category", selectCategory) {
            visibleSelector = toggleSelector(visibleSelector, VisibleSelector.CATEGORY)
        }
        AnimatedVisibility(visibleSelector == VisibleSelector.CATEGORY) {
            OptionSelector("Category:", primaryCategories , selectedCategory) { option ->
                val updated = if (option in selectedCategory) selectedCategory - option else selectedCategory + option
                filterViewModel.setSelectedCategory(updated)
            }
        }

        // Expired In
        SettingsItem("Expired in", selectedExpiredIn.firstOrNull()?.let { expiredInLabels.getOrNull(expiredInValues.indexOf(it)) } ?: "") {
            visibleSelector = toggleSelector(visibleSelector, VisibleSelector.Expired_in)
        }
        AnimatedVisibility(visibleSelector == VisibleSelector.Expired_in) {
            SingleOptionSelector(
                title = "Select Expiration Range:",
                options = expiredInLabels,
                selectedOption = selectedExpiredIn.firstOrNull()?.let { expiredInLabels.getOrNull(expiredInValues.indexOf(it)) } ?: "",
                onOptionToggle = { selectedLabel ->
                    val index = expiredInLabels.indexOf(selectedLabel)
                    val selected = if (index >= 0) listOf(expiredInValues[index]) else emptyList()
                    filterViewModel.setSelectedExpiredIn(selected)
                }
            )
        }

        // Added
        SettingsItem("Added", selectAdded) {
            visibleSelector = toggleSelector(visibleSelector, VisibleSelector.Added)
        }
        AnimatedVisibility(visibleSelector == VisibleSelector.Added) {
            SingleOptionSelector(
                title = "Select Added Date:",
                options = AddedOptions,
                selectedOption = selectedAdded.firstOrNull() ?: "",
                onOptionToggle = { label ->
                    filterViewModel.setSelectedAdded(label?.let { listOf(it) } ?: emptyList())
                }
            )
        }

        // Added Photo
        SettingsItem("Added Photo", selectAddedPhoto) {
            visibleSelector = toggleSelector(visibleSelector, VisibleSelector.Added_photo)
        }
        AnimatedVisibility(visibleSelector == VisibleSelector.Added_photo) {
            SingleOptionSelector(
                title = "Select Photo Option:",
                options = AddedPhotoOptions,
                selectedOption = selectedAddedPhoto.firstOrNull() ?: "",
                onOptionToggle = { label ->
                    filterViewModel.setSelectedAddedPhoto(label?.let { listOf(it) } ?: emptyList())
                }
            )
        }

        // Expiration Date
        SettingsItem("Expiration Date", selectedExpirationDate.firstOrNull()?.let { expirationDateLabels[it] } ?: "") {
            visibleSelector = toggleSelector(visibleSelector, VisibleSelector.Expiration_Date)
        }
        AnimatedVisibility(visibleSelector == VisibleSelector.Expiration_Date) {
            val labelList = expirationDateLabels.values.toList()
            val valueList = expirationDateLabels.keys.toList()
            SingleOptionSelector(
                title = "Select Expiration Date:",
                options = labelList,
                selectedOption = selectedExpirationDate.firstOrNull()?.let { expirationDateLabels[it] } ?: "",
                onOptionToggle = { label ->
                    val index = labelList.indexOf(label)
                    val selected = if (index >= 0) listOf(valueList[index]) else emptyList()
                    filterViewModel.setSelectedExpirationDate(selected)
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("Sort by")

// Sort by Name
        SettingsItem("Product", if (selectedSortByName.isBlank()) "" else selectedSortByName) {
            visibleSelector = toggleSelector(visibleSelector, VisibleSelector.SortByName)
        }
        AnimatedVisibility(visibleSelector == VisibleSelector.SortByName) {
            SingleOptionSelector(
                title = "Sort by Product Name:",
                options = SortbyName,
                selectedOption = selectedSortByName,
                onOptionToggle = { option ->
                    val newOption = if (option == selectedSortByName) "" else option ?: ""
                    filterViewModel.setSortByName(newOption)
                }
            )
        }

// Sort by Date
        SettingsItem("Date", if (selectedSortByDate.isBlank()) "" else selectedSortByDate) {
            visibleSelector = toggleSelector(visibleSelector, VisibleSelector.SortByDate)
        }
        AnimatedVisibility(visibleSelector == VisibleSelector.SortByDate) {
            SingleOptionSelector(
                title = "Sort by Expiration Date:",
                options = SortbyExpiration,
                selectedOption = selectedSortByDate,
                onOptionToggle = { option ->
                    val newOption = if (option == selectedSortByDate) "" else option ?: ""
                    filterViewModel.setSortByDate(newOption)
                }
            )
        }
        Spacer(modifier = Modifier.height(120.dp))


    }



}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
    )
}

// Utility function
private fun toggleSelector(current: VisibleSelector, target: VisibleSelector): VisibleSelector {
    return if (current == target) VisibleSelector.NONE else target
}
private fun truncateToMidnight(timestamp: Long): Long {
    val calendar = java.util.Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

fun getPrimaryCategory(categoriesString: String): String {
    val tagsToCheck = listOf("water", "snack", "food")
    val categoriesList = categoriesString.split(",").map { it.trim().lowercase() }
    return categoriesList.firstOrNull { category ->
        tagsToCheck.any { tag -> category.contains(tag) }
    } ?: categoriesList.firstOrNull() ?: ""
}
