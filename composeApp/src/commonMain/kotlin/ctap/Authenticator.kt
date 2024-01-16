package ctap

import crypto.generateAppKey
import crypto.generateCredentialId
import crypto.getExistingCredentials
import crypto.getSignCount
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

    fun createFlags(register: Boolean, userPresent: Boolean, userVerified: Boolean): Byte {
        var flags = 0x00.toByte()
        if (register) flags = flags or 0x40.toByte()
        if (userPresent) flags = flags or 0x01.toByte()
        if (userVerified) flags = flags or 0x04.toByte()
        return flags
    }

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

        val existingCredentials = getExistingCredentials(request.rp.id)
        request.excludeList.forEach { exclude ->
            if (existingCredentials.any { (credId, _) -> credId.contentEquals(exclude.id) }) {
                logger.info("AuthenticatorMakeCredential: exclude list contains existing credential")
                return@dispatch respondError(StatusCode.CTAP2_ERR_CREDENTIAL_EXCLUDED)
            }
        }

        val credId = generateCredentialId()
        val rpIdHash = MessageDigest.getInstance("SHA-256")
            .digest(request.rp.id.toByteArray())
        val publicKey = generateAppKey(request.rp.id, credId, request.user)

        val flags = createFlags(register = true, userPresent = true, userVerified = true)

        val authenticatorData = AuthenticatorData(
            rpIdHash,
            flags,
            getSignCount(request.rp.id, credId),
            AttestedCredentialData(auth.aaGuid, credId, publicKey),
        )
        val attestationStatement = AttestationStatement(
            COSEAlgorithmIdentifiers.ES256,
            signMessage(request.rp.id, credId) {
                byteArrayOf(*authenticatorData.serialize(), *request.clientDataHash)
            }
        )
        val response = MakeCredentialResponse(
            "packed",
            authenticatorData.serialize(),
            attestationStatement,
        )
        logger.info("AuthenticatorMakeCredential response: $response")
        respondOk(Cbor { encodeDefaults = true }.encodeToByteArray(response))
    }

    @Serializable
    data class GetAssertionRequest(
        @SerialName("1") val rpId: String,
        @SerialName("2") @ByteString val clientDataHash: ByteArray,
        @SerialName("3") val allowList: List<PublicKeyCredentialDescriptor> = emptyList(),
    )

    @Serializable
    data class GetAssertionResponse(
        @SerialName("1") val credential: PublicKeyCredentialDescriptor,
        @SerialName("2") @ByteString val authenticatorData: ByteArray,
        @SerialName("3") @ByteString val signature: ByteArray,
        @SerialName("4") val user: PublicKeyCredentialUserEntity,
    )
    dispatch(AuthenticatorApiCode.AUTHENTICATOR_GET_ASSERTION) {
        val request = Cbor { ignoreUnknownKeys = true }
            .decodeFromByteArray<GetAssertionRequest>(data)

        logger.info("AuthenticatorGetAssertion request: $request")
        val existingCredentials = getExistingCredentials(request.rpId)
        if (existingCredentials.isEmpty()) {
            logger.info("AuthenticatorGetAssertion: no credentials")
            return@dispatch respondError(StatusCode.CTAP2_ERR_NO_CREDENTIALS)
        }
        val (credId, user) = if (request.allowList.isEmpty()) {
            existingCredentials.first()// TODO: let user choose a credential
        } else {
            existingCredentials.firstOrNull { (credId, _) ->
                request.allowList.any { it.id.contentEquals(credId) }
            } ?: return@dispatch respondError(StatusCode.CTAP2_ERR_INVALID_CREDENTIAL)
        }

        val rpIdHash = MessageDigest.getInstance("SHA-256").digest(request.rpId.toByteArray())

        val flags = createFlags(register = false, userPresent = true, userVerified = true)
        val authenticatorData = AuthenticatorData(
            rpIdHash,
            flags,
            getSignCount(request.rpId, credId),
            null,
        )

        val signature = signMessage(request.rpId, credId) {
            byteArrayOf(*authenticatorData.serialize(), *request.clientDataHash)
        }

        val response = GetAssertionResponse(
            PublicKeyCredentialDescriptor("public-key", credId),
            authenticatorData.serialize(),
            signature,
            user,
        )
        logger.info("AuthenticatorGetAssertion response: $response")
        respondOk(Cbor { encodeDefaults = true }.encodeToByteArray(response))
    }

    dispatch(AuthenticatorApiCode.AUTHENTICATOR_GET_INFO) {

    }
}


object AuthenticatorApiCode {
    const val AUTHENTICATOR_MAKE_CREDENTIAL = 0x01.toByte()
    const val AUTHENTICATOR_GET_ASSERTION = 0x02.toByte()
    const val AUTHENTICATOR_GET_INFO = 0x04.toByte()
}