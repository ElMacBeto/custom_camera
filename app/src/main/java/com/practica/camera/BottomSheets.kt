package com.practica.camera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.practica.camera.databinding.ActivityBottomSheetsBinding


const val DATA_FLAG = "data"

class BottomSheets : AppCompatActivity() {

    private lateinit var binding: ActivityBottomSheetsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomSheetsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fab.setOnClickListener{
            val modalBottomSheet = ModalBottomSheet()
            modalBottomSheet.show(supportFragmentManager, ModalBottomSheet.TAG)
        }
    }

}