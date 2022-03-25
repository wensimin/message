package github.wensimin.message.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import github.wensimin.message.utils.logD
import github.wensimin.message.MainActivity
import net.openid.appauth.*
import java.util.function.Consumer


class TokenManager(
    context: MainActivity,
    private var success: Runnable = Runnable {},
    private var error: Consumer<AuthorizationException?> = Consumer { e ->
        logD("oauth2 login error ${e?.errorDescription}")
    }
) {
    companion object {
        const val TOKEN_KEY = "TOKEN_KEY"
        private const val OAUTH_SERVER: String = "https://shali.fun:3000/authorization"
//        private const val OAUTH_SERVER: String = "http://192.168.0.201:81/authorization"

        private const val CLIENT_ID = "message-android"
        private const val CLIENT_SECRET = "message-android"

        // Client secret
        val clientAuthentication: ClientSecretBasic = ClientSecretBasic(CLIENT_SECRET)
        private val serviceConfiguration: AuthorizationServiceConfiguration =
            AuthorizationServiceConfiguration(
                Uri.parse("$OAUTH_SERVER/oauth2/authorize"),  // Authorization endpoint
                Uri.parse("$OAUTH_SERVER/oauth2/token") // Token endpoint
            )
        private var authRequest: AuthorizationRequest = AuthorizationRequest.Builder(
            serviceConfiguration,
            CLIENT_ID,  // Client ID
            ResponseTypeValues.CODE,
            Uri.parse("message://oauth2") // Redirect URI
        ).setScope("openid profile") //scope
            .build()
    }


    private var service: AuthorizationService = AuthorizationService(context)
    private val authState: AuthState = AuthState()
    private val launcher = context.registerForActivityResult(object :
        ActivityResultContract<AuthorizationRequest, Intent?>() {
        override fun createIntent(context: Context, input: AuthorizationRequest): Intent {
            return service.getAuthorizationRequestIntent(authRequest)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
            return intent
        }
    }) { result ->
        if (result == null) {
            error.accept(null)
            return@registerForActivityResult
        }
        val authResponse = AuthorizationResponse.fromIntent(result)
        val authException = AuthorizationException.fromIntent(result)
        authState.update(authResponse, authException)
        if (authResponse != null) {
            retrieveTokens(authResponse, success, error)
        } else {
            error.accept(authException)
        }
    }

    /**
     * 进行oauth2 login
     */
    fun login(success: Runnable, error: Consumer<AuthorizationException?> = this.error) {
        this.success = success
        this.error = error
        launcher.launch(authRequest)
    }

    /**
     * 使用code请求token并且save至authState
     */
    private fun retrieveTokens(
        response: AuthorizationResponse,
        success: Runnable,
        error: Consumer<AuthorizationException?>
    ) {
        val tokenRequest: TokenRequest = response.createTokenExchangeRequest()
        service.performTokenRequest(
            tokenRequest,
            clientAuthentication
        ) { tokenResponse, tokenException ->
            //save token
            authState.update(tokenResponse, tokenException)
            if (tokenException != null) {
                error.accept(tokenException)
            } else {
                //save token
                TokenStatus.setAuthState(authState)
                success.run()
            }
        }
    }

}