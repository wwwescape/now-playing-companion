package com.wwwescape.nowplayingcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wwwescape.nowplayingcompanion.ui.CompanionTheme
import com.wwwescape.nowplayingcompanion.ui.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompanionTheme {
                HomeScreen()
            }
        }
    }
}
