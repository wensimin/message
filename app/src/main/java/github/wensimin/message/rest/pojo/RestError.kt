package github.wensimin.message.rest.pojo

/**
 * rest Error
 */
data class RestError(
    /**
     *  error code
     */
    val error: String? = null,
    val message: String? = null
)