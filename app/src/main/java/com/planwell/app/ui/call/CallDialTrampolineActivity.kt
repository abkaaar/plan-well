package com.planwell.app.ui.call

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.planwell.app.data.alarm.CallReminderIntents
import dagger.hilt.android.AndroidEntryPoint

/**
 * Starts ACTION_CALL from a foreground activity so OEMs that block dialing from
 * background broadcasts still place the call. Falls back to ACTION_DIAL when
 * CALL_PHONE is missing or revoked.
 */
@AndroidEntryPoint
class CallDialTrampolineActivity : ComponentActivity() {

    private var phoneNumber: String = ""

    private val callPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        startDial(preferCall = granted)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phoneNumber = intent.getStringExtra(CallReminderIntents.EXTRA_PHONE).orEmpty()
        if (phoneNumber.isBlank()) {
            finish()
            return
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCallPermission) {
            startDial(preferCall = true)
            finish()
            return
        }

        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    private fun startDial(preferCall: Boolean) {
        val tel = Uri.fromParts("tel", phoneNumber, null)
        val intent = if (preferCall) {
            Intent(Intent.ACTION_CALL, tel)
        } else {
            Intent(Intent.ACTION_DIAL, tel)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
            .onFailure {
                if (preferCall) {
                    runCatching {
                        startActivity(
                            Intent(Intent.ACTION_DIAL, tel).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }
            }
    }
}
