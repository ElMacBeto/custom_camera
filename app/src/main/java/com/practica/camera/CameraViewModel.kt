package com.practica.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel:ViewModel() {

    private val _statusVisibility=MutableLiveData<Boolean>()
    val statusVisibility: LiveData<Boolean> get() = _statusVisibility

    fun changeStatusVisibility(status:Boolean){
        _statusVisibility.value=status
    }



}