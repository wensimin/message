package github.wensimin.message.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import github.wensimin.message.Application.Companion.context
import github.wensimin.message.R


class FirebaseMessagingService : FirebaseMessagingService() {


    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        Log.d(TAG, "From: ${remoteMessage.from}")
        sendNotification(remoteMessage.data)
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * 通过data数据构建通知
     */
    private fun sendNotification(data: Map<String, String>) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .apply {
                setSmallIcon(R.drawable.notification)
                setContentTitle(data["title"])
                setContentText(data["body"])
                priority = NotificationCompat.PRIORITY_DEFAULT
                setAutoCancel(true)
                data["url"]?.let {
                    val notificationIntent = Intent(Intent.ACTION_VIEW)
                    notificationIntent.data = Uri.parse(it)
                    val pi = PendingIntent.getActivity(context, 0, notificationIntent, 0)
                    setContentIntent(pi)
                }
            }

        with(NotificationManagerCompat.from(this)) {
            // 这里id使用时间hash 未来需要识别消息时会使用id hash
            notify(SystemClock.uptimeMillis().hashCode(), builder.build())
        }
    }

    /**
     * 初始化channel
     */
    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableLights(true)
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }
    // [END on_new_token]


    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }


    companion object {
        private const val CHANNEL_ID = "normal"
        private const val TAG = "firebaseMsgService"
    }
}
