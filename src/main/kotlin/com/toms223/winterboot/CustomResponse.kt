package com.toms223.winterboot


import org.http4k.core.Headers
import org.http4k.core.cookie.Cookie

class CustomResponse(val cookies: List<Cookie> = emptyList(), val headers: Headers = emptyList()) {
    var body: Any? = null
    companion object {
        operator fun invoke(lambda: () -> Any): CustomResponse {
            val customResponse = CustomResponse()
            customResponse.body = lambda()
            return customResponse
        }
    }

    operator fun invoke(lambda: () -> Any): CustomResponse {
        val customResponse = this
        customResponse.body = lambda()
        return customResponse
    }
}
