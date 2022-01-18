package github.wensimin.message

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import github.wensimin.message.manager.RestApi
import github.wensimin.message.manager.TokenManager
import github.wensimin.message.manager.TokenStatus
import github.wensimin.message.pojo.Topic
import github.wensimin.message.ui.adapter.TopicListAdapter
import github.wensimin.message.utils.logD
import github.wensimin.message.utils.logI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.springframework.http.HttpMethod

class MainActivity : AppCompatActivity() {
    private lateinit var tokenManager: TokenManager
    val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tokenManager = TokenManager(this)
        val loginButton: Button = findViewById(R.id.login)
        loginButton.setOnClickListener {
            tokenManager.login({
                logI("login success")
                onResume()
            })
        }
        updateToken()
    }

    private fun updateToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            Log.d("FCM", token.toString())
            scope.launch(Dispatchers.IO) {
                val res = RestApi.exchange(
                    endpoint = "token",
                    method = HttpMethod.PUT,
                    body = mapOf("id" to token, "deviceMessage" to getSystemDetail()),
                    responseType = String::class.java
                )
                logI("token update ${res.error ?: "success"} value :$token")
            }
        })
    }


    private fun getSystemDetail(): String {
        return "Brand: ${Build.BRAND} \n" +
                "Model: ${Build.MODEL} \n" +
                "ID: ${Build.ID} \n" +
                "SDK: ${Build.VERSION.SDK_INT} \n" +
                "Manufacture: ${Build.MANUFACTURER} \n" +
                "Brand: ${Build.BRAND} \n" +
                "User: ${Build.USER} \n" +
                "Type: ${Build.TYPE} \n" +
                "Base: ${Build.VERSION_CODES.BASE} \n" +
                "Incremental: ${Build.VERSION.INCREMENTAL} \n" +
                "Board: ${Build.BOARD} \n" +
                "Host: ${Build.HOST} \n" +
                "FingerPrint: ${Build.FINGERPRINT} \n" +
                "Version Code: ${Build.VERSION.RELEASE}"
    }

    override fun onResume() {
        logD("resume")
        val online = TokenStatus.getAuthState() != null
        val onlineLay: View = findViewById(R.id.online)
        val offLineLay: View = findViewById(R.id.offline)
        onlineLay.visibility = if (online) View.VISIBLE else View.GONE
        offLineLay.visibility = if (!online) View.VISIBLE else View.GONE
        if (online) loadData()
        super.onResume()
    }

    fun loadData() {
        val topicList: ListView = findViewById(R.id.topicList)
        scope.launch(Dispatchers.IO) {
            val topics =
                RestApi.exchange(endpoint = "topic", responseType = Array<Topic>::class.java)
            topics.data?.let {
                this@MainActivity.runOnUiThread {
                    topicList.adapter = TopicListAdapter(this@MainActivity, it.toList())
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}