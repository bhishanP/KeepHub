package com.keephub.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.keephub.app.ui.theme.KeepHubTheme
import com.keephub.app.nav.KeepHubNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private fun sharedText(): String? =
        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain")
            intent?.getStringExtra(Intent.EXTRA_TEXT) else null

    private fun openReviewRequested(): Boolean =
        intent?.getBooleanExtra("extra_open_review", false) == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ notifications permission (quick, minimal)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        val prefill = sharedText()
        val startReview = openReviewRequested()

        setContent {
            KeepHubTheme {
                Surface { KeepHubNavHost(startPrefill = prefill, startToReview = startReview) }
            }
        }
    }
}
