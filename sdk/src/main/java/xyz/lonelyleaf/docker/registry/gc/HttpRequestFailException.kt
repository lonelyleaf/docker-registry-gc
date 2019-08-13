package xyz.lonelyleaf.docker.registry.gc

import java.lang.RuntimeException
import java.net.http.HttpResponse

class HttpRequestFailException(
        override val message: String,
        val response: HttpResponse<*>
) : RuntimeException(message)