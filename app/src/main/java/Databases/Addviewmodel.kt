package Databases

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.LingTH.fridge.Barcode.getProductData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Addviewmodel(private val productDao: ProductDao) : ViewModel() {

    var productData by mutableStateOf<ProductData?>(null)
    var isManualEntry by mutableStateOf(false)
    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted = _saveCompleted.asStateFlow()
    // โหลดข้อมูลจาก barcode
    fun loadProduct(barcode: String) {
        viewModelScope.launch {
            val data = getProductData(barcode)
            if (data != null) {
                productData = data
            } else {
                // ให้ผู้ใช้กรอกเอง
                isManualEntry = true
                productData = ProductData(
                    barcode = barcode,
                    product_name = "",
                    categories = "",
                    image_url = "",
                    expiration_date = null,
                    add_day = null,
                    notes = ""
                )
            }
        }
    }

    fun updateProduct(
        id: Int,
        barcode: String,
        name: String,
        categories: String,
        imageUrl: String,
        add_day: Long,
        expie_day: Long,
        notes: String,
        onUpdated: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updatedProduct = ProductData(
                    id = id, // ✅ ใช้ id เดิม
                    barcode = barcode,
                    product_name = name,
                    categories = categories,
                    image_url = imageUrl,
                    add_day = add_day,
                    expiration_date = expie_day,
                    notes = notes
                )
                productDao.updateProduct(updatedProduct)
                onUpdated()
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
    }



    // บันทึกข้อมูลเข้า Room
    fun saveProduct(
        barcode: String,
        name: String,
        categories: String,
        imageUrl: String,
        add_day: Long,
        expie_day: Long,
        notes: String,
        onSaved: () -> Unit,
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (productData == null) {
                productData = ProductData(
                    barcode = barcode,
                    product_name = name,
                    categories = categories,
                    image_url = imageUrl,
                    expiration_date = expie_day,
                    add_day = add_day,
                    notes = notes
                )
            } else {
                productData = productData?.copy(
                    barcode = barcode,
                    product_name = name,
                    categories = categories,
                    image_url = imageUrl,
                    expiration_date = expie_day,
                    add_day = add_day,
                    notes = notes
                )
            }

            try {
                productData?.let {
                    // IGNORE on conflict returns -1 instead of throwing
                    if (productDao.insertProduct(it) == -1L) {
                        Log.e("SaveProductError", "Product id ${it.id} already exists, nothing saved")
                        onError()
                        return@launch
                    }
                    _saveCompleted.value = true // ✅ Trigger event
                    onSaved()
                }
            } catch (e: Exception) {
                Log.e("SaveProductError", "Error inserting product", e)
                onError()
            }
        }
    }

    fun resetSaveFlag() {
        _saveCompleted.value = false
    }
    fun deleteProductById(productId: Int) {
        viewModelScope.launch {
            productDao.deleteById(productId)
        }
    }

}

