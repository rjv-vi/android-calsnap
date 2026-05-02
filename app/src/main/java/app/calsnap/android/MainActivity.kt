package app.calsnap.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.calsnap.android.presentation.screens.original.OriginalCalSnapScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The original CalSnap Android app is a fullscreen PWA/TWA shell.
        // Loading the canonical PWA keeps UI, animations and features exact.
        setContent {
            OriginalCalSnapScreen()
        }
    }
}
