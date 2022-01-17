package github.wensimin.message.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

/**
 * 使用主线程makeText
 */
fun Context.toastShow(message: String, time: Int = Toast.LENGTH_LONG) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, message, time).show()
    }
}

fun Any.logD(message: String) = Log.d(this::class.simpleName, message)
fun Any.logW(message: String) = Log.w(this::class.simpleName, message)
fun Any.logE(message: String) = Log.e(this::class.simpleName, message)
fun Any.logI(message: String) = Log.i(this::class.simpleName, message)
fun Any.logV(message: String) = Log.v(this::class.simpleName, message)
