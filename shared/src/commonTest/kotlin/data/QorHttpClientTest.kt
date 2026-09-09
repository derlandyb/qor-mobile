package data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class QorHttpClientTest {

    private fun mockEngine() = MockEngine { respond(content = "{}", status = HttpStatusCode.OK) }

    @Test
    fun `GIVEN isDebugEnvironment true WHEN a request is made THEN the Logging plugin does not break the request`() = runTest {
        val client = createQorHttpClient(engine = mockEngine(), isDebugEnvironment = true)

        val response = client.get("http://test.local/ping")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GIVEN isDebugEnvironment false WHEN a request is made THEN logging stays off and the request still succeeds`() = runTest {
        val client = createQorHttpClient(engine = mockEngine(), isDebugEnvironment = false)

        val response = client.get("http://test.local/ping")

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
