package ctap

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import states.AuthenticatorState

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
    val handlersBlocking = mutableMapOf<Byte, Boolean>()
    val logger = LoggerFactory.getLogger(javaClass)!!
    fun dispatch(data: ByteArray): CtapResponse {
        val code = data[0]
        val handler = handlers[code] ?: return CtapResponseError(StatusCode.CTAP1_ERR_INVALID_COMMAND)
        val blocking = handlersBlocking[code] ?: false
        logger.trace("Routing dispatcher to $code")
        val session = AuthenticatorSession(data.copyOfRange(1, data.size))
        if (blocking && auth.state.state != AuthenticatorState.State.IDLE) {
            logger.info("AuthenticatorMakeCredential: pending for user action")
            return CtapResponseError(StatusCode.CTAP2_ERR_USER_ACTION_PENDING)
        }
        if (blocking) auth.state.state = AuthenticatorState.State.PROCESSING
        runBlocking {
            handler(session)
        }
        if (blocking) auth.state.state = AuthenticatorState.State.IDLE
        return session.response
    }
}

fun DispatchRoute.dispatch(
    code: Byte,
    blocking: Boolean = false,
    handler: suspend AuthenticatorSession.() -> Unit,
) {
    handlers[code] = handler
    handlersBlocking[code] = blocking
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
