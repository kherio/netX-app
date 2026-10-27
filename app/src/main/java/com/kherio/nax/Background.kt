package com.kherio.nax

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

object Notifier {
    private const val CHANNEL = "nax_alerts"

    fun notify(ctx: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Alertas de red", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(id, n)
    }
}

object Checker {
    /** Compara el estado actual con el de la última vez y avisa de los cambios. */
    fun process(ctx: Context, p: Prefs, devices: List<Device>) {
        val active = devices.filter { it.status != Status.ARCHIVED }
        val macs = active.map { it.mac }.toSet()
        val downNow = active.filter { it.status == Status.DOWN }.map { it.mac }.toSet()

        if (!p.baselineDone) {
            // Primera pasada: solo memorizamos, para no avisar de todo lo existente.
            p.knownMacs = macs
            p.downMacs = downNow
            p.baselineDone = true
            return
        }

        if (p.notifyNew) {
            val fresh = active.filter { it.mac !in p.knownMacs }
            if (fresh.isNotEmpty()) {
                val text = fresh.joinToString("\n") { "${it.title} · ${it.ip.ifBlank { it.mac }}" }
                Notifier.notify(ctx, 1001, "Dispositivo nuevo en la red (${fresh.size})", text)
            }
        }
        if (p.notifyDown) {
            val newlyDown = active.filter { it.mac in downNow && it.mac !in p.downMacs }
            if (newlyDown.isNotEmpty()) {
                val text = newlyDown.joinToString("\n") { "${it.title} · ${it.ip.ifBlank { it.mac }}" }
                Notifier.notify(ctx, 1002, "Dispositivo caído (${newlyDown.size})", text)
            }
        }
        p.knownMacs = p.knownMacs + macs
        p.downMacs = downNow
    }
}

class CheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val p = Prefs(applicationContext)
        if (!p.configured) return Result.success()
        return try {
            Checker.process(applicationContext, p, NaxApi(p.baseUrl, p.token).devices())
            Result.success()
        } catch (e: Exception) {
            // Sin red o servidor caído: se reintenta en el siguiente ciclo.
            Result.success()
        }
    }
}

object Scheduler {
    private const val NAME = "nax_check"

    fun apply(ctx: Context) {
        val p = Prefs(ctx)
        val wm = WorkManager.getInstance(ctx)
        if (!p.configured || !(p.notifyNew || p.notifyDown)) {
            wm.cancelUniqueWork(NAME)
            return
        }
        val req = PeriodicWorkRequestBuilder<CheckWorker>(p.intervalMin.toLong().coerceAtLeast(15), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}
