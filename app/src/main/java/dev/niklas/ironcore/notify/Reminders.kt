package dev.niklas.ironcore.notify

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
import androidx.core.content.ContextCompat
import dev.niklas.ironcore.MainActivity
import dev.niklas.ironcore.R
import dev.niklas.ironcore.data.IronDb
import dev.niklas.ironcore.domain.Rewards
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Local reminders (no push service): a training-day reminder if the directive isn't started,
 * and an evening "streak at risk" alert if nothing was logged today. Inexact alarms, rescheduled
 * daily and after reboot.
 */
object Reminders {
    const val PREFS = "reminders"
    private const val CHANNEL = "reminders"
    const val TYPE_DIRECTIVE = "directive"
    const val TYPE_STREAK = "streak"

    data class Config(val directive: Boolean, val directiveHour: Int, val streak: Boolean, val streakHour: Int)

    fun config(ctx: Context): Config {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Config(p.getBoolean("directive", false), p.getInt("directiveHour", 18), p.getBoolean("streak", false), p.getInt("streakHour", 21))
    }

    fun save(ctx: Context, c: Config) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("directive", c.directive).putInt("directiveHour", c.directiveHour)
            .putBoolean("streak", c.streak).putInt("streakHour", c.streakHour).apply()
        schedule(ctx)
    }

    private fun pending(ctx: Context, type: String) = PendingIntent.getBroadcast(
        ctx, type.hashCode(), Intent(ctx, ReminderReceiver::class.java).setAction(type),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun nextAt(hour: Int): Long {
        val now = LocalDateTime.now()
        var t = now.toLocalDate().atTime(hour, 0)
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val c = config(ctx)
        for ((type, on, hour) in listOf(Triple(TYPE_DIRECTIVE, c.directive, c.directiveHour), Triple(TYPE_STREAK, c.streak, c.streakHour))) {
            val pi = pending(ctx, type)
            am.cancel(pi)
            if (on) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt(hour), pi)
        }
    }

    fun canNotify(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notify(ctx: Context, id: Int, title: String, text: String) {
        if (!canNotify(ctx)) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(0xFFFF1744.toInt())
            .setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) {}
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val action = intent.action ?: return
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) return@launch
                val dao = IronDb.get(ctx).dao()
                val prof = dao.profileNow() ?: return@launch
                val pl = dao.playerNow() ?: return@launch
                val today = LocalDate.now()
                val day = dao.dayNow(today.toEpochDay())
                val quests = dao.keysOnDay(today.toEpochDay()).size
                when (action) {
                    Reminders.TYPE_DIRECTIVE -> {
                        val training = prof.trainingDaysMask and (1 shl (today.dayOfWeek.value - 1)) != 0
                        if (training && quests == 0)
                            Reminders.notify(ctx, 1, "DAILY DIRECTIVE PENDING", "Your protocol is waiting, ${prof.handle}. One quest keeps the streak alive.")
                    }
                    Reminders.TYPE_STREAK -> {
                        val streak = Rewards.effectiveStreak(pl.lastActiveEpochDay, pl.streak, today.toEpochDay())
                        if (quests == 0 && day?.restCheckIn != true && streak > 0)
                            Reminders.notify(ctx, 2, "STREAK AT RISK // $streak DAYS", "Nothing logged today. One quest or a rest check-in before midnight keeps it alive.")
                    }
                }
            } finally {
                Reminders.schedule(ctx)   // next day / after reboot or update
                done.finish()
            }
        }
    }
}
