package com.planwell.app.data.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.planwell.app.ui.MainActivity
import com.planwell.app.ui.call.CallDialTrampolineActivity

/**
 * Builds intents / PendingIntents for scheduled-call alarms and notification actions.
 * Kept separate from the receiver so unit tests can assert construction without posting.
 */
object CallReminderIntents {
    const val ACTION_SNOOZE = "com.planwell.app.action.SNOOZE_CALL"
    const val EXTRA_CALL_ID = "extra_call_id"
    const val EXTRA_PHONE = "extra_phone"
    const val EXTRA_DISPLAY_NAME = "extra_display_name"

    /** Offset so call PendingIntent request codes never collide with reminder ids. */
    const val CALL_REQUEST_CODE_OFFSET = 1_000_000_000

    const val SNOOZE_DURATION_MS = 15 * 60 * 1000L

    fun alarmIntent(
        context: Context,
        callId: Long,
        phoneNumber: String,
        displayName: String,
    ): Intent =
        Intent(context, CallReminderReceiver::class.java).apply {
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_PHONE, phoneNumber)
            putExtra(EXTRA_DISPLAY_NAME, displayName)
        }

    fun snoozeIntent(
        context: Context,
        callId: Long,
        phoneNumber: String,
        displayName: String,
    ): Intent =
        Intent(context, CallReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_PHONE, phoneNumber)
            putExtra(EXTRA_DISPLAY_NAME, displayName)
        }

    fun dialTrampolineIntent(
        context: Context,
        callId: Long,
        phoneNumber: String,
        displayName: String,
    ): Intent =
        Intent(context, CallDialTrampolineActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_PHONE, phoneNumber)
            putExtra(EXTRA_DISPLAY_NAME, displayName)
        }

    fun contentIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    fun alarmPendingIntent(
        context: Context,
        callId: Long,
        phoneNumber: String,
        displayName: String,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(callId),
            alarmIntent(context, callId, phoneNumber, displayName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun callNowPendingIntent(
        context: Context,
        callId: Long,
        phoneNumber: String,
        displayName: String,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode(callId, kind = 1),
            dialTrampolineIntent(context, callId, phoneNumber, displayName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun snoozePendingIntent(
        context: Context,
        callId: Long,
        phoneNumber: String,
        displayName: String,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(callId, kind = 2),
            snoozeIntent(context, callId, phoneNumber, displayName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun fullScreenPendingIntent(
        context: Context,
        callId: Long,
        phoneNumber: String,
        displayName: String,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode(callId, kind = 3),
            dialTrampolineIntent(context, callId, phoneNumber, displayName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun contentPendingIntent(context: Context, callId: Long): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode(callId, kind = 4),
            contentIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun requestCode(callId: Long, kind: Int = 0): Int =
        (CALL_REQUEST_CODE_OFFSET + callId * 10 + kind).toInt()

    fun snoozeTriggerAt(nowMs: Long = System.currentTimeMillis()): Long =
        nowMs + SNOOZE_DURATION_MS
}
