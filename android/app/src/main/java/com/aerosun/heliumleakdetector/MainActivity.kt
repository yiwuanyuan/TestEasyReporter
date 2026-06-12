package com.aerosun.heliumleakdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aerosun.heliumleakdetector.ui.navigation.HeliumNavHost
import com.aerosun.heliumleakdetector.ui.theme.HeliumLeakDetectorTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 单 Activity 架构入口。
 * 所有界面由 Compose Navigation 管理，无 Fragment。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeliumLeakDetectorTheme {
                HeliumNavHost()
            }
        }
    }
}
