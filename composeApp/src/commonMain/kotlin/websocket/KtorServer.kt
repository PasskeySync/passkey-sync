package websocket
import ctap.Communicator
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close


val webSocketCommunicator by lazy { Communicator() }

val ktorServer by lazy {
    embeddedServer(Netty, 11107, watchPaths = emptyList()) {
        install(WebSockets)
        routing {
            get("/") {
                call.respondText("All good here in Ktor!")
            }
            webSocketRoute()
        }
    }
}

fun Route.webSocketRoute() {
    webSocket("/initial") {
        for (frame in incoming) {
            frame as? Frame.Binary ?: continue
        }
        TODO("Init the cable protocol as specified in 11.5")
    }

    webSocket("/cable/connect/{routingID}/{tunnelID}") {
        val routingID = call.parameters["routingID"]
            ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No routing ID"))
        val tunnelID = call.parameters["tunnelID"]
            ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No tunnel ID"))
        TODO("Finish the implementation of the cable protocol as specified in 11.5")
    }

    webSocket("/local") {
        for (frame in incoming) {
            frame as? Frame.Binary ?: continue
            val response = webSocketCommunicator.dispatch(frame.data)
            send(Frame.Binary(true, response))
        }
    }
}