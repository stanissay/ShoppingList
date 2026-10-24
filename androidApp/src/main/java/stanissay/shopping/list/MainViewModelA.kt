/*
 * Shopping List For Android and Wear Os
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package stanissay.shopping.list

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import stanissay.shopping.shared.AppEventManager
import stanissay.shopping.shared.R
import stanissay.shopping.shared.SharedViewModel
import stanissay.shopping.shared.StringProvider
import stanissay.shopping.shared.UiEvent

class MainViewModelA(app: Application, val strings: StringProvider) : AndroidViewModel(app) {
    val sharedViewModel = SharedViewModel(strings)

    fun sendUiEvent(event: UiEvent) { AppEventManager.sendEvent(event) }

    fun login() {
        sharedViewModel.loadAll()
        viewModelScope.launch {
            sendUiEvent(UiEvent.ShowSnackbar("${strings.getString(R.string.hello)}, ${sharedViewModel.userName}"))
        }
    }
    fun logout() {
        sharedViewModel.logout()
        viewModelScope.launch {
            sendUiEvent(UiEvent.ShowSnackbar("${strings.getString(R.string.goodbye)}, ${sharedViewModel.userName}"))
        }
    }

    fun updateItemsOrder() {
        viewModelScope.launch {
            sharedViewModel.updateItemsOrder()
        }
    }
    fun updateCategoriesOrder() {
        viewModelScope.launch {
            sharedViewModel.updateCategoriesOrder()
        }
    }

    init {
        viewModelScope.launch {
            sharedViewModel.messages.collect { message ->
                sendUiEvent(UiEvent.ShowToast(message))
            }
        }
        sharedViewModel.init()
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        super.onCleared()
        sharedViewModel.clear()
    }
}