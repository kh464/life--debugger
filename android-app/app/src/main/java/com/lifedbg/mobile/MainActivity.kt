package com.lifedbg.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lifedbg.mobile.ui.navigation.LifeDbgAppRoot
import com.lifedbg.mobile.ui.theme.LifeDebuggerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeDebuggerTheme {
                LifeDbgAppRoot()
            }
        }
    }
}
