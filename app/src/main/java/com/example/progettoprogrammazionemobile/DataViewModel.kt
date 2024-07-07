// DataViewModel.kt
package com.example.progettoprogrammazionemobile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Calendar

class DataViewModel : ViewModel() {
    private val _username = MutableLiveData<String>()
    val username: LiveData<String> get() = _username

    private val _fixedEntries = MutableLiveData<Double>()
    val fixedEntries: LiveData<Double> get() = _fixedEntries

    private val _fixedOut = MutableLiveData<Double>()
    val fixedOut: LiveData<Double> get() = _fixedOut

    private val _balance = MutableLiveData<Double>()
    val balance: LiveData<Double> get() = _balance

    fun setUsername(name: String) {
        _username.value = name
    }

    fun setFixedEntries(entries: Double) {
        _fixedEntries.value = entries
    }

    fun setFixedOut(out: Double) {
        _fixedOut.value = out
    }

    fun setBalance(newBalance: Double) {
        _balance.value = newBalance
    }

    fun updateBalance() {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        if (dayOfMonth == 10) {
            _balance.value = (_balance.value ?: 0.0) + (_fixedEntries.value ?: 0.0)
        } else if (dayOfMonth == 11) {
            _balance.value = (_balance.value ?: 0.0) - (_fixedOut.value ?: 0.0)
        }
    }

}
