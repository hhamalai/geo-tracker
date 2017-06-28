package net.hhamalai

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.testing.*
import org.jetbrains.ktor.util.*
import org.jetbrains.ktor.websocket.Frame
import org.jetbrains.ktor.websocket.readText
import org.jetbrains.ktor.websocket.webSocket
import java.time.Duration

class TestTrackerApplication {

    /*@Test fun testRequest() = withTestApplication(Application::main) {
        with (handleRequest(HttpMethod.Get, "/")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("<h1>Tracker</h1>", response.content)
        }
    }

    @Test
    fun testMessageReceivedDispatch()  = runBlocking<Unit> {
        receivedMessage("_", "{\"lat\": 20.0, \"lon\": 40.0, \"vel\": 120.5}")
    }

    @Test
    fun testHello() {
        withTestApplication {
            application.routing {
                webSocket("/ws") {
                    handle { frame ->
                        if (!frame.frameType.controlFrame) {
                            println("heck")
                            send(frame.copy())
                            close()
                        }
                    }
                }
            }

            handleWebSocket("/ws") {
                println("foo")
                bodyBytes = hex("""
                    0x81 0x05 0x48 0x65 0x6c 0x6c 0x6f
                """.trimHex())
            }.let { call ->
                println("bar")
                call.awaitWebSocket(Duration.ofSeconds(10))
                assertEquals("810548656c6c6f", hex(call.response.byteContent!!))
            }
        }
    }
    private fun String.trimHex() = replace("\\s+".toRegex(), "").replace("0x", "")
*/
}
