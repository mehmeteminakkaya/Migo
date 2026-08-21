package com.example.migo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.migo.data.model.UserProfile
import com.example.migo.ui.profile.ProfileSetupScreen
import com.example.migo.ui.theme.MigoTheme

class ProfileSetupPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MigoTheme {
                ProfileSetupScreen(
                    initialProfile = UserProfile(
                        fullName = "Mehmet Akkaya",
                        university = "İSTİNYE ÜNİVERSİTESİ (İSTANBUL)",
                        universityId = "preview-university",
                        department = "Bilgisayar Mühendisliği",
                        programId = "preview-program",
                        classLevel = ""
                    ),
                    onBackClick = ::finish,
                    onContinueClick = {}
                )
            }
        }
    }
}
