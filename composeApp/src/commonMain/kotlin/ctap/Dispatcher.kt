package ctap

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

fun Authenticator.routing(configuration: DispatchRoute.() -> Unit): DispatchRoute {
    dispatcherRoute.configuration()
    return dispatcherRoute
}

fun Authenticator.install(communicator: Communicator) {
    communicator.onDispatch { data -> dispatcherRoute.dispatch(data).data }
}

class DispatchRoute(
    val auth: Authenticator
) {
    val handlers = mutableMapOf<Byte, suspend AuthenticatorSession.() -> Unit>()
    val logger = LoggerFactory.getLogger(javaClass)!!
    fun dispatch(data: ByteArray): CtapResponse {
        val code = data[0]
        val handler = handlers[code] ?: return CtapResponseError(StatusCode.CTAP1_ERR_INVALID_COMMAND)
        logger.trace("Routing dispatcher to $code")
        val session = AuthenticatorSession(data.copyOfRange(1, data.size))
        runBlocking {
            handler(session)
        }
        return session.response
    }
}

fun DispatchRoute.dispatch(code: Byte, handler: suspend AuthenticatorSession.() -> Unit) {
    handlers[code] = handler
}

class AuthenticatorSession(
    val data: ByteArray
) {
    var response: CtapResponse = CtapResponseOk(byteArrayOf())
}

fun AuthenticatorSession.respond(response: CtapResponse) {
    this.response = response
}

fun AuthenticatorSession.respondOk(data: ByteArray) = respond(CtapResponseOk(data))
fun AuthenticatorSession.respondError(errorCode: Byte) = respond(CtapResponseError(errorCode))
