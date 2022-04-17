package dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import java.net.InetSocketAddress


class MockBitbucketServer {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
  private val receivedRequests = mutableListOf<ReceivedRequest>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    server.createContext("/") { httpExchange ->
      receivedRequests.add(ReceivedRequest(httpExchange))
      httpExchange.sendResponseHeaders(200, 0)
      httpExchange.responseBody.close()
    }
    server.start()
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun stop() {
    println("stop")
    server.stop(0)
  }

  fun getHost(): String = "http://localhost:${server.address.port}"

  fun getSingleReceivedRequest(): ReceivedRequest {
    assertThat(receivedRequests.size).overridingErrorMessage("Expected exactly one received request")
            .isEqualTo(1)

    return receivedRequests[0]
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class ReceivedRequest(val requestPath: String, val requestMethod: String, val requestBody: String, val headers: Map<String, List<String>>) {

    constructor(httpExchange: HttpExchange) : this(httpExchange.requestURI.toString(),
                                                   httpExchange.requestMethod,
                                                   httpExchange.requestBody.readAllBytes().decodeToString(),
                                                   httpExchange.requestHeaders.toMap())
  }
}