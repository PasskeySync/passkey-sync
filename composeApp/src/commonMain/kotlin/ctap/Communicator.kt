package ctap

/**
 * Communicator is a class that bridges the authenticator and the transport layer.
 */
class Communicator {

    var dispatcher: suspend (ByteArray) -> ByteArray = {
        throw Exception("No dispatcher installed")
    }
    suspend fun dispatch(data: ByteArray): ByteArray {
        return dispatcher(data)
    }
    fun onDispatch(dispatcher: suspend (ByteArray) -> ByteArray) {
        this.dispatcher = dispatcher
    }
}

