package com.lanfund.app.ui.fund

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lanfund.app.data.repository.FundRepository

/**
 * FundViewModel工厂
 */
class FundViewModelFactory(
    private val repository: FundRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FundViewModel::class.java)) {
            return FundViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
