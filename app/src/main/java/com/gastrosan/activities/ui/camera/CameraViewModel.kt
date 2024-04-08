package com.gastrosan.activities.ui.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "Aqui se tomarán las fotos de las facturas"
    }
    val text: LiveData<String> = _text
}