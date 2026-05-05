package app.calsnap.android.data.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.calsnap.android.MainActivity
import app.calsnap.android.R
import app.calsnap.android.data.preferences.ReminderConfig
import java.util.Calendar

object ReminderScheduler {
    private const val CHANNEL_ID = "calsnap_reminders"
    private const val PREFS = "calsnap_reminder_schedule"
    private const val ACTION_REMINDER = "app.calsnap.android.REMINDER"
    private const val EXTRA_TYPE = "type"
    private const val BREAKFAST = "breakfast"
    private const val LUNCH = "lunch"
    private const val DINNER = "dinner"
    private const val REQ_BREAKFAST = 1001
    private const val REQ_LUNCH = 1002
    private const val REQ_DINNER = 1003
    private const val REQ_OLD_WATER = 1004

    fun apply(context: Context, config: ReminderConfig) {
        save(context, config)
        createChannel(context)
        cancel(context)
        if (!config.enabled) return
        scheduleEnabled(context, config)
    }

    fun restore(context: Context) {
        apply(context, load(context))
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        listOf(REQ_BREAKFAST, REQ_LUNCH, REQ_DINNER, REQ_OLD_WATER).forEach {
            alarm.cancel(pendingIntent(context, it, "cancel"))
        }
    }

    fun show(context: Context, type: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val (title, body) = when (type) {
            BREAKFAST -> "🌅 CalSnap" to context.getString(R.string.reminder_breakfast_body)
            LUNCH -> "☀️ CalSnap" to context.getString(R.string.reminder_lunch_body)
            DINNER -> "🌙 CalSnap" to context.getString(R.string.reminder_dinner_body)
            "enabled" -> "🍎 CalSnap" to context.getString(R.string.reminders_enabled_toast)
            else -> return
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(type.hashCode(), notification)
    }

    fun fire(context: Context, type: String) {
        show(context, type)
        val config = load(context)
        if (config.enabled && isEnabled(config, type)) {
            scheduleDaily(context, type, timeFor(config, type), requestCodeFor(type))
        }
    }

    private fun scheduleEnabled(context: Context, config: ReminderConfig) {
        if (config.breakfastOn) scheduleDaily(context, BREAKFAST, config.breakfastTime, REQ_BREAKFAST)
        if (config.lunchOn) scheduleDaily(context, LUNCH, config.lunchTime, REQ_LUNCH)
        if (config.dinnerOn) scheduleDaily(context, DINNER, config.dinnerTime, REQ_DINNER)
    }

    private fun scheduleDaily(context: Context, type: String, time: String, requestCode: Int) {
        val (hour, minute) = parseTime(time)
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger,
            pendingIntent(context, requestCode, type),
        )
    }

    private fun pendingIntent(context: Context, requestCode: Int, type: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION_REMINDER)
                .putExtra(EXTRA_TYPE, type),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun isEnabled(config: ReminderConfig, type: String): Boolean = when (type) {
        BREAKFAST -> config.breakfastOn
        LUNCH -> config.lunchOn
        DINNER -> config.dinnerOn
        else -> false
    }

    private fun timeFor(config: ReminderConfig, type: String): String = when (type) {
        BREAKFAST -> config.breakfastTime
        LUNCH -> config.lunchTime
        DINNER -> config.dinnerTime
        else -> "08:30"
    }

    private fun requestCodeFor(type: String): Int = when (type) {
        BREAKFAST -> REQ_BREAKFAST
        LUNCH -> REQ_LUNCH
        DINNER -> REQ_DINNER
        else -> REQ_BREAKFAST
    }

    private fun parseTime(time: String): Pair<Int, Int> {
        val parts = time.split(':')
        return (parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8) to
            (parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30)
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminders_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
    }

    private fun save(context: Context, config: ReminderConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", config.enabled)
            .putString("breakfast", config.breakfastTime)
            .putString("lunch", config.lunchTime)
            .putString("dinner", config.dinnerTime)
            .putBoolean("breakfastOn", config.breakfastOn)
            .putBoolean("lunchOn", config.lunchOn)
            .putBoolean("dinnerOn", config.dinnerOn)
            .apply()
    }

    private fun load(context: Context): ReminderConfig {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ReminderConfig(
            enabled = prefs.getBoolean("enabled", false),
            breakfastTime = prefs.getString("breakfast", "08:30") ?: "08:30",
            lunchTime = prefs.getString("lunch", "13:00") ?: "13:00",
            dinnerTime = prefs.getString("dinner", "19:00") ?: "19:00",
            breakfastOn = prefs.getBoolean("breakfastOn", true),
            lunchOn = prefs.getBoolean("lunchOn", true),
            dinnerOn = prefs.getBoolean("dinnerOn", true),
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.fire(context, intent.getStringExtra("type").orEmpty())
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ReminderScheduler.restore(context)
        }
    }
}
