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

    private val _fixedOut = MutableLiveData<Double>()

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


}
