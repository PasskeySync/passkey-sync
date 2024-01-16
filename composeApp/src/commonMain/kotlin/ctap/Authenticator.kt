package ctap

import crypto.generateAppKey
import crypto.signMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.ByteString
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import states.AuthenticatorState
import java.security.MessageDigest
import kotlin.experimental.or

class Authenticator(
    val aaGuid: ByteArray,
    val state: AuthenticatorState,
    module: Authenticator.() -> Unit = {},
) {
    val dispatcherRoute = DispatchRoute(this)
    init {
        routing {
            authenticatorApi()
        }
        module()
    }
}


@OptIn(ExperimentalSerializationApi::class)
fun DispatchRoute.authenticatorApi() {
    @Serializable
    data class MakeCredentialRequest(
        @SerialName("1") @ByteString val clientDataHash: ByteArray,
        @SerialName("2") val rp: PublicKeyCredentialRpEntity,
        @SerialName("3") val user: PublicKeyCredentialUserEntity,
        @SerialName("4") val pubKeyCredParams: List<PublicKeyCredentialParameters>,
        @SerialName("5") val excludeList: List<PublicKeyCredentialDescriptor> = emptyList(),
    )

    @Serializable
    data class MakeCredentialResponse(
        @SerialName("1") val fmt: String,
        @SerialName("2") @ByteString val authData: ByteArray,
        @SerialName("3") val attStmt: AttestationStatement,
    )

    dispatch(AuthenticatorApiCode.AUTHENTICATOR_MAKE_CREDENTIAL) {
        val request = Cbor { ignoreUnknownKeys = true }
            .decodeFromByteArray<MakeCredentialRequest>(data)
        logger.info("AuthenticatorMakeCredential request: $request")

        val supportedAlgo = PublicKeyCredentialParameters(
            "public-key",
            COSEAlgorithmIdentifiers.ES256
        )
        if (request.pubKeyCredParams.contains(supportedAlgo)) {
            logger.info("AuthenticatorMakeCredential: supported algorithm")
        } else {
            logger.info("AuthenticatorMakeCredential: unsupported algorithm")
            return@dispatch respondError(StatusCode.CTAP2_ERR_UNSUPPORTED_ALGORITHM)
        }

        //TODO: request for user verification (PIN)
        //TODO: check excludeList

        val rpIdHash = MessageDigest.getInstance("SHA-256")
            .digest(request.rp.id.toByteArray())
        val alias = "${request.rp.id}/${request.user.id}"
        val publicKey = generateAppKey(alias)

        fun createFlags(rk: Boolean, userPresent: Boolean, userVerified: Boolean): Byte {
            var flags = 0x00.toByte()
            if (rk) flags = flags or 0x40.toByte()
            if (userPresent) flags = flags or 0x01.toByte()
            if (userVerified) flags = flags or 0x04.toByte()
            return flags
        }

        val flags = createFlags(rk = true, userPresent = true, userVerified = true)

        val authenticatorData = AuthenticatorData(
            rpIdHash,
            flags,
            auth.state.signCount,
            AttestedCredentialData(auth.aaGuid, request.user.id, publicKey),
        )
        val dataToSign = byteArrayOf(*authenticatorData.serialize(), *request.clientDataHash)
        val attestationStatement = AttestationStatement(
            COSEAlgorithmIdentifiers.ES256,
            signMessage(alias, dataToSign),
        )
        val response = MakeCredentialResponse(
            "packed",
            authenticatorData.serialize(),
            attestationStatement,
        )
        logger.info("AuthenticatorMakeCredential response: $response")
        respondOk(Cbor { encodeDefaults = true }.encodeToByteArray(response))
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