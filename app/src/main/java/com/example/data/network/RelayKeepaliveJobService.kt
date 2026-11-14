package com.example.data.network

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.example.data.model.SendMessagePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Lightweight persisted background job that sends a transient keepalive signal
 * to the configured Serverless Relay / MongoDB instance and deletes it immediately.
 * Prevents MongoDB Atlas 60-day inactivity auto-suspension even if the app isn't opened.
 */
class RelayKeepaliveJobService : JobService() {

    private val jobScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        val appContext = applicationContext
        jobScope.launch {
            try {
                sendInstantDeletePulse(appContext)
            } catch (_: Exception) {
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true

    companion object {
        private const val JOB_ID = 7401
        private const val PREFS_NAME = "clestra_relay_prefs"
        private const val KEY_RELAY_URL = "relay_url"
        private const val KEY_NODE_ID = "node_id"
        private const val KEY_LAST_PULSE_TS = "last_pulse_ts"
        private const val DEFAULT_RELAY_URL = "https://clestra-relay.vercel.app"
        private const val PULSE_INTERVAL_MS = 12L * 60L * 60L * 1000L
        private const val MIN_FOREGROUND_DEBOUNCE_MS = 4L * 60L * 60L * 1000L

        fun saveRelayConfig(context: Context, url: String, nodeId: String? = null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                if (url.isNotBlank()) putString(KEY_RELAY_URL, url.trim())
                if (!nodeId.isNullOrBlank()) putString(KEY_NODE_ID, nodeId.trim())
                apply()
            }
        }

        fun getRelayUrl(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_RELAY_URL, DEFAULT_RELAY_URL)?.ifBlank { DEFAULT_RELAY_URL }
                ?: DEFAULT_RELAY_URL
        }

        fun scheduleKeepalive(context: Context) {
            try {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                    ?: return
                if (scheduler.getPendingJob(JOB_ID) != null) return

                val component = ComponentName(context, RelayKeepaliveJobService::class.java)
                val builder = JobInfo.Builder(JOB_ID, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(PULSE_INTERVAL_MS)
                    .setPersisted(true)

                scheduler.schedule(builder.build())
            } catch (_: Exception) {
            }
        }

        suspend fun sendInstantDeletePulseIfDue(context: Context, force: Boolean = false) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val last = prefs.getLong(KEY_LAST_PULSE_TS, 0L)
            if (!force && (now - last) < MIN_FOREGROUND_DEBOUNCE_MS) return
            if (sendInstantDeletePulse(context)) {
                prefs.edit().putLong(KEY_LAST_PULSE_TS, now).apply()
            }
        }

        suspend fun sendInstantDeletePulse(context: Context): Boolean {
            if (!NotificationHelper.isDeviceOnline(context)) return false
            val baseUrl = getRelayUrl(context)
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) return false

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nodeId = prefs.getString(KEY_NODE_ID, null)?.ifBlank { null } ?: "CLS-NODE"
            val pulseRecipient = "CLS-PULSE-${UUID.randomUUID().toString().take(12)}"

            return try {
                val service = NetworkClient.createRelayService(baseUrl)
                val payload = SendMessagePayload(
                    senderId = nodeId,
                    recipientId = pulseRecipient,
                    compressedContent = "PULSE",
                    timestamp = System.currentTimeMillis(),
                    pulse = true
                )
                val response = service.sendOfflineMessage(payload)
                // If server didn't delete inline on POST, immediately sync/delete the pulse doc so MongoDB stays at 0 bytes
                if (response.status != "pulse_deleted") {
                    try {
                        service.syncOfflineMessages(pulseRecipient)
                    } catch (_: Exception) {
                    }
                }
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
