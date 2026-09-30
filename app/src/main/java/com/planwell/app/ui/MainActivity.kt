package com.planwell.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.planwell.app.ui.theme.PlanWellTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var deepLinkTaskId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkTaskId = readTaskId(intent)
        setContent {
            PlanWellTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlanWellNavGraph(
                        deepLinkTaskId = deepLinkTaskId,
                        onDeepLinkHandled = { deepLinkTaskId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkTaskId = readTaskId(intent)
    }

    private fun readTaskId(intent: Intent?): Long? {
        val id = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        return id.takeIf { it > 0 }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
