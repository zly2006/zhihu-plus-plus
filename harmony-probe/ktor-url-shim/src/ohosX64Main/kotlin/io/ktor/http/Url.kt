package io.ktor.http

class URLProtocol(val name: String, val defaultPort: Int = -1)

class Parameters(private val values: Map<String, String>) {
    operator fun get(name: String): String? = values[name]
}

class Url(urlString: String) {
    val protocol: URLProtocol
    val host: String
    val segments: List<String>
    val parameters: Parameters

    init {
        val schemeEnd = urlString.indexOf("://")
        if (schemeEnd <= 0) error("Invalid URL: $urlString")
        protocol = URLProtocol(urlString.take(schemeEnd))
        val rest = urlString.substring(schemeEnd + 3)
        val hostEnd = rest.indexOfFirst { it == '/' || it == '?' }
        host = if (hostEnd < 0) rest else rest.take(hostEnd)
        val afterHost = if (hostEnd < 0) "" else rest.substring(hostEnd + 1)
        segments = afterHost.substringBefore('?').split('/').filter { it.isNotEmpty() }
        val query = afterHost.substringAfter('?', "")
        parameters = Parameters(
            if (query.isEmpty()) {
                emptyMap()
            } else {
                query.split('&').mapNotNull { pair ->
                    val idx = pair.indexOf('=')
                    if (idx <= 0) null else pair.take(idx) to pair.substring(idx + 1)
                }.toMap()
            },
        )
    }
}
