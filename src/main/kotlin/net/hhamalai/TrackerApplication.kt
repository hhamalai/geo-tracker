@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package net.hhamalai

import kotlinx.coroutines.experimental.channels.consumeEach
import net.hhamalai.model.AppConfiguration
import net.hhamalai.model.Location
import net.hhamalai.model.MapConfiguration
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.features.*
import org.jetbrains.ktor.gson.*
import org.jetbrains.ktor.websocket.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.locations.*
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.sessions.*
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
    install(GsonSupport) {
        setPrettyPrinting()
    }

    routing {
        install(Sessions) {
            cookie<Session>("SESSION")
        }

        intercept(ApplicationCallPipeline.Infrastructure) {
            if (call.sessions.get<Session>() == null) {
                call.sessions.set(Session(nextNonce()))
            }
        }

        get("/v1/configuration") {
            val appConfig = AppConfiguration(
                    mapConfiguration = MapConfiguration(
                            defaultMapPosition = Location(
                                    latitude = 60.421858399999995f,
                                    longitude = 23.455625799999997f
                            ),
                            defaultZoomLevel = 8
                    )
            )
            call.respond(JsonResponse(appConfig))
        }

        get("/v1/locations") {
            val model = server.trackerDatabase.getMostRecents()
            call.respond(JsonResponse(model))
        }

        webSocket("/ws") {
            val session = call.sessions.get<Session>()
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

internal suspend fun receivedMessage(id: String, command: String) {
    when {
        command.startsWith("/user") -> {
            val newName = command.removePrefix("/user").trim()
            when {
                newName.isEmpty() -> server.sendTo(id, "server::help", "/user [newName]")
                else -> server.memberRenamed(id, newName)
            }
        }
        command.startsWith("/location") -> server.updateLocation(id, command.removePrefix("/location").trim())
        command.startsWith("/") -> server.sendTo(id, "server::help", "Unknown command ${command.takeWhile { !it.isWhitespace() }}")

        else -> server.message(id, command)
    }
}