package com.evora.technologies.office

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.evora.technologies.office.bootstrap.BootstrapRoute
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EvoraTheme {
                BootstrapRoute()
            }
        }
    }
}
