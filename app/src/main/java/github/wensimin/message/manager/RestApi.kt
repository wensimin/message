package github.wensimin.message.manager

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import github.wensimin.message.Application
import github.wensimin.message.rest.exception.AuthException
import github.wensimin.message.rest.exception.SystemException
import github.wensimin.message.rest.pojo.ErrorType
import github.wensimin.message.rest.pojo.RestError
import github.wensimin.message.rest.pojo.RestResponse
import github.wensimin.message.utils.logE
import github.wensimin.message.utils.toastShow
import org.springframework.http.*
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.io.InputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 *  restTemplate 简单包装
 */
object RestApi {
    private val context: android.app.Application = Application.context
    private val converters: MutableList<HttpMessageConverter<*>> = ArrayList()
    private val globalErrorHandler: ResponseErrorHandler
    private val clientHttpRequestFactory: SimpleClientHttpRequestFactory
    private val jsonMapper: ObjectMapper

    //    private const val RESOURCE_SERVER: String = "https://boliboli.xyz:3000/message-rs"
    private const val RESOURCE_SERVER: String = "http://192.168.0.201:8080/message-rs/"

    init {
        converters.apply {
            add(StringHttpMessageConverter(Charset.defaultCharset()))
            add(FormHttpMessageConverter())
            add(MappingJackson2HttpMessageConverter().apply {
                //时间格式
                objectMapper.dateFormat =
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply {
                        timeZone = TimeZone.getTimeZone("GMT+8")
                    }
                // 忽略多余json
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 使用kotlin模块,通过构建参数来给值
                objectMapper.registerKotlinModule()
                // 存储一个mapper引用用于转化page
                jsonMapper = objectMapper
            })
        }
        clientHttpRequestFactory = SimpleClientHttpRequestFactory().apply {
            setReadTimeout(5000)
            setConnectTimeout(3000)
        }
        // 错误监听
        globalErrorHandler = object : ResponseErrorHandler {
            override fun hasError(response: ClientHttpResponse): Boolean {
                return response.statusCode != HttpStatus.OK
            }

            override fun handleError(response: ClientHttpResponse) {
                when (response.statusCode) {
                    HttpStatus.UNAUTHORIZED, HttpStatus.BAD_REQUEST -> {
                        throw AuthException()
                    }
                    else -> {
                        throwError(response.body)
                    }
                }
            }
        }
    }

    /**
     * 从body 解析json error
     */
    private fun throwError(body: InputStream?) {
        val restError = jsonMapper.readValue(body, RestError::class.java)
        //处理刷新token过期
        throw if (restError.error == "invalid_grant") AuthException() else SystemException(
            restError.error ?: ErrorType.ERROR.name,
            restError.message ?: "未知错误"
        )
    }


    /**
     * 错误处理,目前仅处理auth,其他错误全部按未知处理
     */
    private fun errorHandler(e: Exception) {
        if (e is AuthException) {
            TokenStatus.clearAuth()
            context.toastShow("登录已过期,重新打开app登录")
        } else {
            //TODO 错误信息处理
            logE(e.message ?: "未知错误")
        }
    }

    private fun getAuthHeader(): HttpHeaders {
        return HttpHeaders().apply {
            val authState = TokenStatus.getAuthState() ?: throw AuthException()
            val accessToken = authState.requestAccessToken(TokenManager.clientAuthentication)
            this["Authorization"] = "Bearer $accessToken"
        }
    }


    fun buildTemplate(auth: Boolean = true, jsonBody: Boolean = true): RestTemplate {
        return RestTemplate().apply {
            messageConverters = converters
            errorHandler = globalErrorHandler
            requestFactory = clientHttpRequestFactory
            interceptors = interceptors ?: mutableListOf()
            interceptors.add(ClientHttpRequestInterceptor { request, body, execution ->
                request.headers.apply {
                    try {
                        // 非get 使用json body
                        if (request.method != HttpMethod.GET && jsonBody) contentType =
                            MediaType.APPLICATION_JSON
                        if (auth) this.putAll(getAuthHeader())
                    } catch (e: Exception) {
                        //处理一轮错误
                        errorHandler(e)
                    }
                }
                execution.execute(request, body)
            })
        }
    }

    fun <O> exchange(
        endpoint: String,
        method: HttpMethod = HttpMethod.GET,
        body: Any? = null,
        responseType: Class<O>
    ): RestResponse<O> {
        return try {
            val res = buildTemplate().exchange(
                RESOURCE_SERVER + endpoint,
                method,
                HttpEntity(body),
                responseType
            ).body
            return RestResponse(res)
        } catch (e: Exception) {
            errorHandler(e)
            RestResponse(null, e)
        }
    }

}