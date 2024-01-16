package ctap

import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.ByteString
import java.io.ByteArrayOutputStream

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

fun AttestedCredentialData.serialize(): ByteArray {
    return byteArrayOf(
        *aaguid,
        *credentialId.size.toShort().toByteArray(),
        *credentialId,
        *credentialPublicKey.serialize(),
    )
}

@OptIn(ExperimentalSerializationApi::class)
data class ECCredentialPublicKey(
    val kty: Int = 2,
    val alg: COSEAlgorithmIdentifier = COSEAlgorithmIdentifiers.ES256,
    val curve: Int = 1,
    @ByteString val x: ByteArray,
    @ByteString val y: ByteArray,
)

val logger = KtorSimpleLogger("ctap")
fun ECCredentialPublicKey.serialize(): ByteArray {
    logger.info("Serializing ECCredentialPublicKey $this")
    val out = ByteArrayOutputStream()
    val gen = CBORFactory().createGenerator(out)
    gen.writeStartObject(5)
    gen.writeFieldId(1)
    gen.writeNumber(kty)
    gen.writeFieldId(3)
    gen.writeNumber(alg)
    gen.writeFieldId(-1)
    gen.writeNumber(curve)
    gen.writeFieldId(-2)
    gen.writeBinary(x)
    gen.writeFieldId(-3)
    gen.writeBinary(y)
    gen.close()
    return out.toByteArray()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AttestationStatement(
    val alg: COSEAlgorithmIdentifier,
    @ByteString val sig: ByteArray,
)
