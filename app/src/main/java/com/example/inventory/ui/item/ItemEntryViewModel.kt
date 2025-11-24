/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.inventory.ui.item

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.inventory.EncryptionUtil
import com.example.inventory.data.Item
import com.example.inventory.data.ItemsRepository
import com.example.inventory.data.SettingsRepository
import com.google.gson.Gson
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.NumberFormat

/**
 * ViewModel to validate and insert items in the Room database.
 */
class ItemEntryViewModel(
    private val itemsRepository: ItemsRepository,
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    /**
     * Holds current item ui state
     */
    var itemUiState by mutableStateOf(ItemUiState())
        private set

    private val gson = Gson()
    private val encryptionUtil = EncryptionUtil()

    init {
        val settings = settingsRepository.getSettings()
        val initialDetails = if (settings.useDefaultQuantity) {
            ItemDetails(quantity = settings.defaultQuantity.toString(), source = "manual")
        } else {
            ItemDetails(source = "manual")
        }
        itemUiState = ItemUiState(itemDetails = initialDetails, isEntryValid = validateInput(initialDetails))
    }

    /**
     * Updates the [itemUiState] with the value provided in the argument. This method also triggers
     * a validation for input values.
     */
    fun updateUiState(itemDetails: ItemDetails) {
        itemUiState =
            ItemUiState(itemDetails = itemDetails, isEntryValid = validateInput(itemDetails))
    }

    /**
     * Inserts an [Item] in the Room database
     */
    suspend fun saveItem() {
        if (validateInput()) {
            val item = itemUiState.itemDetails.toItem(source = "manual")
            itemsRepository.insertItem(item)
        }
    }

    suspend fun loadFromEncryptedFile(fileUri: Uri) {
        try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                val encryptedString = inputStream.readBytes().toString(Charsets.UTF_8)
                val decryptedJson = encryptionUtil.decrypt(encryptedString)
                val item = gson.fromJson(decryptedJson, Item::class.java)?.copy(source = "file")
                item?.let { itemsRepository.insertItem(it) }
            }
        } catch (e: Exception) {
            Log.e("LoadItem", "Failed to load from encrypted file", e)
        }
    }

    private fun validateInput(uiState: ItemDetails = itemUiState.itemDetails): Boolean {
        return with(uiState) {
            name.isNotBlank() && price.isNotBlank() && quantity.isNotBlank() &&
                    supplierName.isNotBlank() &&
                    supplierEmail.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(supplierEmail).matches() &&
                    supplierPhone.isNotBlank() && supplierPhone.matches(Regex("^[+]?[0-9]{7,}$"))
        }
    }
}

/**
 * Represents Ui State for an Item.
 */
data class ItemUiState(
    val itemDetails: ItemDetails = ItemDetails(),
    val isEntryValid: Boolean = false
)

data class ItemDetails(
    val id: Int = 0,
    val name: String = "",
    val price: String = "",
    val quantity: String = "",
    val supplierName: String = "",
    val supplierEmail: String = "",
    val supplierPhone: String = "",
    val source: String = "manual"
)

/**
 * Extension function to convert [ItemUiState] to [Item]. If the value of [ItemDetails.price] is
 * not a valid [Double], then the price will be set to 0.0. Similarly if the value of
 * [ItemUiState] is not a valid [Int], then the quantity will be set to 0
 */
fun ItemDetails.toItem(source: String? = null): Item = Item(
    id = id,
    name = name,
    price = price.toDoubleOrNull() ?: 0.0,
    quantity = quantity.toIntOrNull() ?: 0,
    supplierName = supplierName,
    supplierEmail = supplierEmail,
    supplierPhone = supplierPhone,
    source = source ?: this.source
)

fun Item.formatedPrice(): String {
    return NumberFormat.getCurrencyInstance().format(price)
}

/**
 * Extension function to convert [Item] to [ItemUiState]
 */
fun Item.toItemUiState(isEntryValid: Boolean = false): ItemUiState = ItemUiState(
    itemDetails = this.toItemDetails(),
    isEntryValid = isEntryValid
)

/**
 * Extension function to convert [Item] to [ItemDetails]
 */
fun Item.toItemDetails(): ItemDetails = ItemDetails(
    id = id,
    name = name,
    price = price.toString(),
    quantity = quantity.toString(),
    supplierName = supplierName,
    supplierEmail = supplierEmail,
    supplierPhone = supplierPhone,
    source = source
)