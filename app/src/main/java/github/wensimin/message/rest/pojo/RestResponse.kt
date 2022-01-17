package github.wensimin.message.rest.pojo

data class RestResponse<O>(val data: O? = null, val error: Exception? = null)