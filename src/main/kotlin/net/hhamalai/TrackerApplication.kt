@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package net.hhamalai

import com.google.gson.GsonBuilder
import kotlinx.coroutines.experimental.channels.consumeEach
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.features.*
import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.websocket.*
import org.jetbrains.ktor.logging.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.locations.*
import org.jetbrains.ktor.request.acceptItems
import org.jetbrains.ktor.request.host
import org.jetbrains.ktor.request.port
import org.jetbrains.ktor.sessions.session
import org.jetbrains.ktor.sessions.sessionOrNull
import org.jetbrains.ktor.sessions.withCookieByValue
import org.jetbrains.ktor.sessions.withSessions
import org.jetbrains.ktor.transform.transform
import org.jetbrains.ktor.util.nextNonce
import java.time.Duration

private val server = TrackerServer()
class JsonResponse(val data: Any)

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(Locations)
    install(WebSockets) {
        pingPeriod = Duration.ofMinutes(1)
    }
    routing {
        withSessions<Session> {
            withCookieByValue()
        }

        val gson = GsonBuilder().create()

        intercept(ApplicationCallPipeline.Infrastructure) {
            if (call.sessionOrNull<Session>() == null) {
                call.session(Session(nextNonce()))
            }
            if (call.request.acceptItems().any { it.value == "application/json" }) {
                call.transform.register<JsonResponse> { value ->
                    TextContent(gson.toJson(value.data), ContentType.Application.Json)
                }
            }
        }

        get("/v1/locations") {
            val model = server.trackerDatabase.getMostRecents()
            call.respond(JsonResponse(model))
        }

        webSocket("/ws") {
            val session = call.sessionOrNull<Session>()
            if (session == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                return@webSocket
            }

            server.memberJoin(session.id, this)

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        receivedMessage(session.id, frame.readText())
                    }
                }
            } finally {
                server.memberLeft(session.id, this)
            }
        }



        static {
            defaultResource("index.html", "web")
            resources("web")
        }
    }
}

data class Session(val id: String)

private fun <T : Any> ApplicationCall.redirectUrl(t: T, secure: Boolean = true): String {
    val hostPort = request.host()!! + request.port().let { port -> if (port == 80) "" else ":$port" }
    val protocol = when {
        secure -> "https"
        else -> "http"
    }
    return "$protocol://$hostPort${application.feature(Locations).href(t)}"
}

internal suspend fun receivedMessage(id: String, command: String) {
    when {
        command.startsWith("/user") -> {
            val newName = command.removePrefix("/user").trim()
            when {
                newName.isEmpty() -> server.sendTo(id, "server::help", "/user [newName]")
                newName.length > 50 -> server.sendTo(id, "server::help", "new name is too long: 50 characters limit")
                else -> server.memberRenamed(id, newName)
            }
        }
        command.startsWith("/location") -> server.updateLocation(id, command.removePrefix("/location").trim())
        command.startsWith("/") -> server.sendTo(id, "server::help", "Unknown command ${command.takeWhile { !it.isWhitespace() }}")

        else -> server.message(id, command)
    }
}