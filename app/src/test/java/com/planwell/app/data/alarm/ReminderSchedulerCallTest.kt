package com.planwell.app.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.planwell.app.domain.model.ScheduledCall
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerCallTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun scheduleCall_usesExactAlarmWhenAllowed() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val shadow: ShadowAlarmManager = shadowOf(alarmManager)
        val scheduler = ReminderScheduler(context, alarmManager)
        val triggerAt = System.currentTimeMillis() + 120_000
        val call = ScheduledCall(
            id = 42L,
            displayName = "Ada",
            phoneNumber = "+15551234567",
            scheduledAtMs = triggerAt,
        )

        scheduler.scheduleCall(call)

        val next = shadow.nextScheduledAlarm
        requireNotNull(next)
        assertEquals(triggerAt, next.triggerAtTime)

        val intent = savedIntent(next.operation)
        assertEquals(CallReminderReceiver::class.java.name, intent.component?.className)
        assertEquals(42L, intent.getLongExtra(CallReminderIntents.EXTRA_CALL_ID, -1))
        assertEquals("+15551234567", intent.getStringExtra(CallReminderIntents.EXTRA_PHONE))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun scheduleCall_usesWindowWhenExactAlarmsDenied() {
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        every { alarmManager.canScheduleExactAlarms() } returns false

        val scheduler = ReminderScheduler(context, alarmManager)
        val triggerAt = System.currentTimeMillis() + 60_000
        scheduler.scheduleCall(
            ScheduledCall(
                id = 5L,
                displayName = "Bob",
                phoneNumber = "+15550001111",
                scheduledAtMs = triggerAt,
            ),
        )

        verify(exactly = 1) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES / 4,
                any(),
            )
        }
        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun cancelCall_cancelsMatchingPendingIntent() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val shadow: ShadowAlarmManager = shadowOf(alarmManager)
        val scheduler = ReminderScheduler(context, alarmManager)
        val call = ScheduledCall(
            id = 11L,
            displayName = "Cara",
            phoneNumber = "+15559876543",
            scheduledAtMs = System.currentTimeMillis() + 90_000,
        )

        scheduler.scheduleCall(call)
        requireNotNull(shadow.nextScheduledAlarm)
        scheduler.cancelCall(11L)
        assertNull(shadow.nextScheduledAlarm)
    }

    private fun savedIntent(pendingIntent: PendingIntent?): Intent {
        requireNotNull(pendingIntent)
        return shadowOf(pendingIntent).savedIntent
    }
}
