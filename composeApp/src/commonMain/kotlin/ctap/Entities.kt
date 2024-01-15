package ctap

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.ByteString
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray

@Serializable
data class PublicKeyCredentialRpEntity(
    val id: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PublicKeyCredentialUserEntity(
    @ByteString val id: ByteArray,
    val displayName: String,
)

typealias COSEAlgorithmIdentifier = Int
@Serializable
data class PublicKeyCredentialParameters(
    val type: String,
    val alg: COSEAlgorithmIdentifier,
)

object COSEAlgorithmIdentifiers {
    const val ES256 = -7
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PublicKeyCredentialDescriptor(
    val type: String,
    @ByteString val id: ByteArray,
)

fun Int.toByteArray(): ByteArray {
    return byteArrayOf(
        (this shr 24).toByte(),
        (this shr 16).toByte(),
        (this shr 8).toByte(),
        this.toByte(),
    )
}
fun Short.toByteArray(): ByteArray {
    return byteArrayOf(
        (this.toInt() shr 8).toByte(),
        this.toByte(),
    )
}

data class AuthenticatorData(
    val rpIdHash: ByteArray,
    val flags: Byte,
    val signCount: Int,
    val attestedCredentialData: AttestedCredentialData?,
)
fun AuthenticatorData.serialize(): ByteArray {
    val attestedCredentialData = attestedCredentialData?.serialize() ?: byteArrayOf()
    return byteArrayOf(
        *rpIdHash,
        flags,
        *signCount.toByteArray(),
        *attestedCredentialData
    )
}
data class AttestedCredentialData(
    val aaguid: ByteArray,
    val credentialId: ByteArray,
    val credentialPublicKey: ECCredentialPublicKey,
)

@OptIn(ExperimentalSerializationApi::class)
fun AttestedCredentialData.serialize(): ByteArray {
    return byteArrayOf(
        *aaguid,
        *credentialId.size.toShort().toByteArray(),
        *credentialId,
        *Cbor.encodeToByteArray(credentialPublicKey)
    )
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ECCredentialPublicKey(
    @ByteString val x: ByteArray,
    @ByteString val y: ByteArray,
)


@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AttestationStatement(
    val alg: COSEAlgorithmIdentifier,
    @ByteString val sig: ByteArray,
)
