package net.gotev.uploadservice.observer.task

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig.namespace
import net.gotev.uploadservice.UploadServiceConfig.placeholdersProcessor
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadNotificationStatusConfig
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.extensions.validateNotificationChannel
import net.gotev.uploadservice.network.ServerResponse

class NotificationHandler(private val service: UploadService) : UploadTaskObserver {

    // Check here if error appeares
    private const val CHANNEL_GROUP = "EBM_NOTIFICATION_CH_GROUP"
    private val notificationCreationTimeMillis by lazy { System.currentTimeMillis() }

    private val notificationManager by lazy {
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }


    private fun NotificationCompat.Builder.notify(uploadId: String, notificationId: Int) {
        build().apply {
            if (service.holdForegroundNotification(uploadId, this)) {
                notificationManager.cancel(notificationId)
            } else {
                notificationManager.notify(notificationId, this)
            }
        }
    }

    private fun NotificationCompat.Builder.setCommonParameters(
        statusConfig: UploadNotificationStatusConfig,
        info: UploadInfo
    ): NotificationCompat.Builder {
        return setGroup(CHANNEL_GROUP)
            .setContentTitle(placeholdersProcessor.processPlaceholders(statusConfig.title, info))
            .setContentText(placeholdersProcessor.processPlaceholders(statusConfig.message, info))
            .setSmallIcon(statusConfig.iconResourceID)
            .setLargeIcon(statusConfig.largeIcon)
            .setColor(statusConfig.iconColorResourceID)
    }

    private fun ongoingNotification(
        notificationConfig: UploadNotificationConfig,
        info: UploadInfo
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(service, notificationConfig.notificationChannelId)
            .setWhen(notificationCreationTimeMillis)
            .setOngoing(true)
    }

    private fun NotificationCompat.Builder.setDeleteIntentIfPresent(
        intent: PendingIntent?
    ): NotificationCompat.Builder {
        return intent?.let { setDeleteIntent(it) } ?: this
    }

    override fun onStart(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    ) {
        notificationManager.validateNotificationChannel(notificationConfig.notificationChannelId)

        ongoingNotification(notificationConfig, info)
            .notify(info.uploadId, notificationId)
    }

    override fun onProgress(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    ) {
    }

    override fun onSuccess(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig,
        response: ServerResponse
    ) {
        
    }

    override fun onError(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig,
        exception: Throwable
    ) {
        val statusConfig = if (exception is UserCancelledUploadException) {
            notificationConfig.cancelled
        } else {
            notificationConfig.error
        }
    }

    override fun onCompleted(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    ) {
    }
}
