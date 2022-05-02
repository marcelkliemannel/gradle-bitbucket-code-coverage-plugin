package dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper

import com.sun.net.httpserver.HttpExchange

data class BitbucketApiRequest(val requestPath: String,
                               val requestMethod: String,
                               val requestBody: String,
                               val authorizationHeader: String? = null) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  constructor(httpExchange: HttpExchange) : this(httpExchange.requestURI.toString(),
                                                 httpExchange.requestMethod,
                                                 httpExchange.requestBody.readAllBytes().decodeToString(),
                                                 httpExchange.requestHeaders["Authorization"]?.takeIf { it.size == 1 }?.get(0))

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}