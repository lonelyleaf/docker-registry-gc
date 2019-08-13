package xyz.lonelyleaf.docker.registry.gc

import okhttp3.Response
import java.lang.RuntimeException

class HttpRequestFailException(
        override val message: String,
        val response: Response
) : RuntimeException(message)