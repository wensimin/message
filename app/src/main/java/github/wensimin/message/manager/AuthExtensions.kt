package github.wensimin.message.manager

import github.wensimin.message.rest.exception.AuthException
import net.openid.appauth.AuthState
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException


/**
 * 进行auth的扩展方法
 * 增加同步获取刷新token的方法
 */

/**
 * 同步请求,不使用原有方法
 */
fun AuthState.requestAccessToken(clientSecretBasic: ClientSecretBasic): String {
    if (!needsTokenRefresh) {
        return this.accessToken!!
    }
    return try {
        val tokenResponse = asyncRefreshTokenRequest(createTokenRefreshRequest(), clientSecretBasic)
        this.update(tokenResponse, null)
        TokenStatus.setAuthState(this)
        tokenResponse.accessToken!!
    } catch (e: HttpClientErrorException) {
        // 对400错误码做包装授权错误
        if (e.statusCode == HttpStatus.UNAUTHORIZED) {
            throw AuthException()
        }
        throw e
    }
}

/**
 * 用rest template 同步请求token
 */
fun asyncRefreshTokenRequest(
    tokenRequest: TokenRequest,
    clientSecretBasic: ClientSecretBasic
): TokenResponse {
    val restTemplate = RestApi.buildTemplate(auth = false, jsonBody = false)
    val requestHeaders = clientSecretBasic.getRequestHeaders(tokenRequest.clientId)
    val httpHeaders = HttpHeaders()
    requestHeaders.forEach { (k, v) -> httpHeaders.add(k, v) }
    httpHeaders.contentType = MediaType.APPLICATION_FORM_URLENCODED
    val body: MultiValueMap<String, String> = LinkedMultiValueMap()
    tokenRequest.requestParameters.forEach { (k, v) -> body.add(k, v) }
    val entity = HttpEntity(body, httpHeaders)
    val response: ResponseEntity<String> =
        restTemplate.postForEntity(
            tokenRequest.configuration.tokenEndpoint.toString(),
            entity,
            String::class.java
        )
    return TokenResponse.Builder(tokenRequest).fromResponseJsonString(response.body).build()
}

