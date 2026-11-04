package com.planwell.app.data.alarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.planwell.app.ui.call.CallDialTrampolineActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CallReminderIntentsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun alarmIntent_targetsReceiverWithExtras() {
        val intent = CallReminderIntents.alarmIntent(
            context = context,
            callId = 7L,
            phoneNumber = "+15551234567",
            displayName = "Ada",
        )

        assertEquals(CallReminderReceiver::class.java.name, intent.component?.className)
        assertEquals(7L, intent.getLongExtra(CallReminderIntents.EXTRA_CALL_ID, -1))
        assertEquals("+15551234567", intent.getStringExtra(CallReminderIntents.EXTRA_PHONE))
        assertEquals("Ada", intent.getStringExtra(CallReminderIntents.EXTRA_DISPLAY_NAME))
    }

    @Test
    fun snoozeIntent_usesSnoozeActionAndFifteenMinuteOffset() {
        val intent = CallReminderIntents.snoozeIntent(
            context = context,
            callId = 3L,
            phoneNumber = "+15550001111",
            displayName = "Bob",
        )

        assertEquals(CallReminderIntents.ACTION_SNOOZE, intent.action)
        assertEquals(CallReminderReceiver::class.java.name, intent.component?.className)

        val now = 1_700_000_000_000L
        assertEquals(now + 15 * 60 * 1000L, CallReminderIntents.snoozeTriggerAt(now))
        assertEquals(15 * 60 * 1000L, CallReminderIntents.SNOOZE_DURATION_MS)
    }

    @Test
    fun dialTrampolineIntent_carriesPhoneForCallNow() {
        val intent = CallReminderIntents.dialTrampolineIntent(
            context = context,
            callId = 9L,
            phoneNumber = "+15559876543",
            displayName = "Cara",
        )

        assertEquals(CallDialTrampolineActivity::class.java.name, intent.component?.className)
        assertEquals("+15559876543", intent.getStringExtra(CallReminderIntents.EXTRA_PHONE))
        assertEquals(9L, intent.getLongExtra(CallReminderIntents.EXTRA_CALL_ID, -1))
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun requestCodes_areNamespacedAwayFromSmallReminderIds() {
        val code = CallReminderIntents.requestCode(1L)
        assertTrue(code >= CallReminderIntents.CALL_REQUEST_CODE_OFFSET)
        assertEquals(
            CallReminderIntents.requestCode(1L, kind = 1),
            CallReminderIntents.requestCode(1L) + 1,
        )
    }
}
