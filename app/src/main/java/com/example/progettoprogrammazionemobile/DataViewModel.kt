// DataViewModel.kt
package com.example.progettoprogrammazionemobile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Calendar

class DataViewModel : ViewModel() {
    private val _username = MutableLiveData<String>()
    val username: LiveData<String> get() = _username

    private val _fixedEntries = MutableLiveData<String>()
    val fixedEntries: LiveData<String> get() = _fixedEntries

    private val _fixedOut = MutableLiveData<String>()
    val fixedOut: LiveData<String> get() = _fixedOut

    private val _balance = MutableLiveData<Double>()
    val balance: LiveData<Double> get() = _balance

    fun setUsername(name: String) {
        _username.value = name
    }

    fun setFixedEntries(entries: String) {
        _fixedEntries.value = entries
    }

    fun setFixedOut(out: String) {
        _fixedOut.value = out
    }

    fun setBalance(newBalance: Double) {
        _balance.value = newBalance
    }


}
