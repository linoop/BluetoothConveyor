package com.linoop.btconveyorlib

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.technowavegroup.printerlib.BTUtil

class MainActivity : AppCompatActivity() {
    private lateinit var btUtil: BTUtil
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}