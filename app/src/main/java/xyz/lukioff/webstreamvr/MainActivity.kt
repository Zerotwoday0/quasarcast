package xyz.lukioff.webstreamvr

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import xyz.lukioff.webstreamvr.ui.MainScreen
import xyz.lukioff.webstreamvr.ui.theme.QuasarCastTheme

class MainActivity : ComponentActivity() {

    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            StreamService.start(this, result.resultCode, data)
        }
    }

    private fun requestCaptureAndStart() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleAction(intent)
        setContent {
            QuasarCastTheme {
                val status by StreamService.status.collectAsState()
                var settings by remember { mutableStateOf(StreamSettings.load(this)) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        status = status,
                        settings = settings,
                        onSettingsChange = {
                            settings = it
                            StreamSettings.save(this, it)
                            StreamService.apply(this)
                        },
                        onStart = { requestCaptureAndStart() },
                        onStop = { StreamService.stop(this) },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAction(intent)
    }

    // Lets `adb shell am start -n org.quasarcast.app/.MainActivity -a xyz.lukioff.webstreamvr.START`
    // (or .STOP) control streaming without touching the panel.
    private fun handleAction(intent: Intent?) {
        when (intent?.action) {
            StreamService.ACTION_START -> requestCaptureAndStart()
            StreamService.ACTION_STOP -> StreamService.stop(this)
        }
    }
}
