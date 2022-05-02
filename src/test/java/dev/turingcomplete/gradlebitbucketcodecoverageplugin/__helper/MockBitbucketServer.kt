package dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper

import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import java.net.InetSocketAddress


class MockBitbucketServer {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
  private val receivedRequests = mutableListOf<BitbucketApiRequest>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    server.createContext("/") { httpExchange ->
      assertThat(httpExchange.requestHeaders["Content-Type"]).containsExactly("application/json")

      receivedRequests.add(BitbucketApiRequest(httpExchange))
      httpExchange.sendResponseHeaders(200, 0)
      httpExchange.responseBody.close()
    }
    server.start()
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun stop() {
    server.stop(0)
  }

  fun getHost(): String = "http://localhost:${server.address.port}"

  fun getReceivedRequests(): List<BitbucketApiRequest> {
    return receivedRequests
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}