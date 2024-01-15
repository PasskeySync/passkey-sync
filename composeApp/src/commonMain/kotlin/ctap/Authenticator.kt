package ctap

interface Authenticator {





}


val authenticator: Authenticator = AuthenticatorImpl
object AuthenticatorImpl : Authenticator {
    init {
        routing {
            dispatchApi()
        }
    }
}

fun DispatchRoute.dispatchApi() {

    dispatch(AuthenticatorApiCode.AUTHENTICATOR_MAKE_CREDENTIAL) {


    }

    dispatch(AuthenticatorApiCode.AUTHENTICATOR_GET_ASSERTION) {

    }

    dispatch(AuthenticatorApiCode.AUTHENTICATOR_GET_INFO) {

    }
}


object AuthenticatorApiCode {
    const val AUTHENTICATOR_MAKE_CREDENTIAL = 0x01.toByte()
    const val AUTHENTICATOR_GET_ASSERTION = 0x02.toByte()
    const val AUTHENTICATOR_GET_INFO = 0x04.toByte()
}