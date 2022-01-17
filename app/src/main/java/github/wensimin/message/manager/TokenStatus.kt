package github.wensimin.message.manager

import androidx.preference.PreferenceManager
import com.auth0.android.jwt.JWT
import github.wensimin.message.Application
import github.wensimin.message.rest.pojo.Auths
import net.openid.appauth.AuthState


/**
 * token object
 */
object TokenStatus {

    private var authState: AuthState? = null
    private val preferences = PreferenceManager.getDefaultSharedPreferences(Application.context)
    private var authArray: Array<String>? = null
    fun getAuthState(): AuthState? {
        if (authState != null) {
            return authState
        }
        val tokenJson = preferences.getString(TokenManager.TOKEN_KEY, null) ?: return null
        initAuthState(AuthState.jsonDeserialize(tokenJson))
        return authState
    }

    fun setAuthState(authState: AuthState) {
        initAuthState(authState)
        preferences.edit().putString(TokenManager.TOKEN_KEY, authState.jsonSerializeString())
            .apply()
    }

    /**
     * 初始化authState 同时保存 auths
     */
    private fun initAuthState(authState: AuthState) {
        TokenStatus.authState = authState
        TokenStatus.authState?.accessToken?.let { token ->
            authArray = JWT(token).claims["auth"]?.asArray(String::class.java)
        }
    }

    /**
     * 检查是否有指定权限
     */
    fun hasAuth(auth: Auths): Boolean {
        return authArray?.contains(auth.name) ?: false
    }

    /**
     * 清空auth
     */
    fun clearAuth() {
        preferences.edit().remove(TokenManager.TOKEN_KEY).apply()
        authState = null
        authArray = null
    }

}