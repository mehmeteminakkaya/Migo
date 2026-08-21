package com.example.migo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.migo.ui.main.MainShell
import com.example.migo.ui.theme.MigoTheme

class MainShellPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MigoTheme {
                var session by remember { mutableStateOf(storeDemoSession()) }
                MainShell(
                    session = session,
                    onSessionChange = { session = it },
                    onReset = {}
                )
            }
        }
    }
}
