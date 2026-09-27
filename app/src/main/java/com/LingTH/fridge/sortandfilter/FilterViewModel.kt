package com.LingTH.fridge.sortandfilter

import Databases.ProductDao
import Databases.ProductData
import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

class FilterViewModel(private val dao: ProductDao) : ViewModel() {

    // --- ข้อมูลตั้งต้น ---
    val allCategories = dao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts = dao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProductsWithPhotos = dao.getWithPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProductsWithoutPhotos = dao.getWithoutPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpirationDates = dao.getAllExpirationDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAddedDates = dao.getAllAddedDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Filter criteria ---
    var selectedCategory = MutableStateFlow<List<String>>(emptyList())
        private set

    var selectedExpiredIn = MutableStateFlow<List<Long>>(emptyList()) // <-- เปลี่ยนจาก String เป็น Long
        private set

    var selectedAdded = MutableStateFlow<List<String>>(emptyList())
        private set

    var selectedAddedPhoto = MutableStateFlow<List<String>>(emptyList())
        private set

    var selectedExpirationDate = MutableStateFlow<List<Long>>(emptyList()) // <-- เปลี่ยนจาก String เป็น Long
        private set

    var productName = MutableStateFlow("")
        private set

    // --- Sort criteria ---
    private val _selectedSortOption = MutableStateFlow("")
    val selectedSortOption: StateFlow<String> = _selectedSortOption

    // --- Setters ---
    fun setSelectedCategory(value: List<String>) { selectedCategory.value = value }
    fun setSelectedExpiredIn(value: List<Long>) { selectedExpiredIn.value = value }
    fun setSelectedAdded(value: List<String>) { selectedAdded.value = value }
    fun setSelectedAddedPhoto(value: List<String>) { selectedAddedPhoto.value = value }
    fun setSelectedExpirationDate(value: List<Long>) { selectedExpirationDate.value = value }

    fun setSearchText(text: String) {
        productName.value = text
    }

    private val _selectedSortByName = MutableStateFlow("")
    val selectedSortByName: StateFlow<String> = _selectedSortByName

    fun setSortByName(option: String) {
        _selectedSortByName.value = option
        _selectedSortOption.value = option
        filterProducts()
    }

    private val _selectedSortByDate = MutableStateFlow("")
    val selectedSortByDate: StateFlow<String> = _selectedSortByDate

    fun setSortByDate(option: String) {
        _selectedSortByDate.value = option
        _selectedSortOption.value = option
        filterProducts()
    }


    fun getFilteredByPhoto(option: String, dao: ProductDao): Flow<List<ProductData>> {
        return when (option) {
            "Added Photo" -> allProductsWithPhotos
            "NO Photo" -> allProductsWithoutPhotos
            else -> dao.getAllProducts()
        }
    }

    val filteredProducts = MutableStateFlow<List<ProductData>>(emptyList())

    init {
        viewModelScope.launch {
            launch { allProducts.collect { filterProducts() } }
            launch { selectedCategory.collect { filterProducts() } }
            launch { selectedExpiredIn.collect { filterProducts() } }
            launch { selectedAdded.collect { filterProducts() } }
            launch { selectedAddedPhoto.collect { filterProducts() } }
            launch { selectedExpirationDate.collect { filterProducts() } }
            launch { productName.collect { filterProducts() } }
            launch { selectedSortOption.collect { filterProducts() } }
        }
    }

    @SuppressLint("NewApi")
    private fun filterProducts() {
        val currentProducts = allProducts.value
        val now = System.currentTimeMillis()

        val filtered = currentProducts.filter { product ->
            val expirationDate = product.expiration_date
            val addDay = product.add_day

            val matchesCategory = selectedCategory.value.isEmpty() || selectedCategory.value.any { selected ->
                val tags = product.categories.split(",")
                    .map { it.trim().lowercase() }

                Log.d("Filter", "🔍 Selected: $selected | Tags: $tags")

                tags.any { tag ->
                    tag.contains(selected.lowercase())
                }
            }


            val matchesExpiredIn = selectedExpiredIn.value.isEmpty() || (
                    expirationDate != null && selectedExpiredIn.value.any { targetTimestamp ->
                        val daysLeft = (expirationDate - now) / (1000 * 60 * 60 * 24)
                        val targetDaysLeft = (targetTimestamp - now) / (1000 * 60 * 60 * 24)
                        daysLeft == targetDaysLeft
                    }
                    )


            val matchesAdded = selectedAdded.value.isEmpty() || (addDay != null && run {
                val daysAgo = ChronoUnit.DAYS.between(
                    Instant.ofEpochMilli(addDay).atZone(ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now()
                )
                selectedAdded.value.contains("Added $daysAgo day(s) ago")
            })

            val matchesPhoto = selectedAddedPhoto.value.isEmpty() || (
                    selectedAddedPhoto.value.contains("Added Photo") && product.image_url.isNotBlank() ||
                            selectedAddedPhoto.value.contains("NO Photo") && product.image_url.isBlank()
                    )

            val matchesExpirationDate = selectedExpirationDate.value.isEmpty() || (
                    expirationDate != null && selectedExpirationDate.value.contains(expirationDate)
                    )

            val matchesSearch = productName.value.isEmpty() ||
                    product.product_name.contains(productName.value, ignoreCase = true) ||
                    product.categories.contains(productName.value, ignoreCase = true)

            matchesCategory && matchesExpiredIn && matchesAdded &&
                    matchesPhoto && matchesExpirationDate && matchesSearch
        }


        val sorted = when (_selectedSortOption.value) {
            "Name (A-Z)" -> filtered.sortedBy { it.product_name.lowercase() }
            "Name (Z-A)" -> filtered.sortedByDescending { it.product_name.lowercase() }

            // Products without an expiration date go last in both orders
            "Expiration Date (Soonest)" -> filtered.sortedBy { it.expiration_date ?: Long.MAX_VALUE }
            "Expiration Date (Latest)" -> filtered.sortedByDescending { it.expiration_date ?: Long.MIN_VALUE }

            else -> filtered
        }


        filteredProducts.value = sorted
    }
}
