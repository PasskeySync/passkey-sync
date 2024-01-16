package crypto

import ctap.ECCredentialPublicKey
import ctap.PublicKeyCredentialUserEntity
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

const val filePath = "appKey.dat"

object KeyPairSerializer : KSerializer<KeyPair> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("KeyPair") {
            element("bytes", String.serializer().descriptor)
        }

    override fun deserialize(decoder: Decoder): KeyPair {
        val bytes = decoder.decodeString()
        ObjectInputStream(bytes.decodeBase64Bytes().inputStream()).use {
            return it.readObject() as KeyPair
        }
    }

    override fun serialize(encoder: Encoder, value: KeyPair) {
        val bytes = ByteArrayOutputStream().use {
            ObjectOutputStream(it).use {
                it.writeObject(value)
            }
            it.toByteArray()
        }
        encoder.encodeString(bytes.encodeBase64())
    }

}

@Serializable
data class CredentialInfo(
    val user: PublicKeyCredentialUserEntity,
    @Serializable(with = KeyPairSerializer::class) val keyPair: KeyPair,
    val signCount: Int,
)

typealias CredentialInfoMap = MutableMap<String, CredentialInfo>

@OptIn(ExperimentalSerializationApi::class)
val credentialInfo: MutableMap<String, CredentialInfoMap> by lazy {
    val file = File(filePath)
    val map = if (file.exists()) {
        FileInputStream(filePath).use {
            Cbor.decodeFromByteArray<MutableMap<String, CredentialInfoMap>>(it.readBytes())
        }
    } else {
        file.createNewFile()
        mutableMapOf()
    }
    map
}

@OptIn(ExperimentalSerializationApi::class)
private fun saveResult() {
    FileOutputStream(filePath).use {
        it.write(Cbor.encodeToByteArray(credentialInfo))
    }
}

private fun normalizeKey(key: ByteArray): ByteArray {
    return if (key[0] == 0.toByte()) {
        key.slice(1 until key.size).toByteArray()
    } else {
        key
    }
}

@OptIn(ExperimentalStdlibApi::class)
actual fun generateAppKey(
    rpId: String,
    credId: ByteArray,
    user: PublicKeyCredentialUserEntity,
): ECCredentialPublicKey {
    val keyPairGenerator = KeyPairGenerator.getInstance("EC")
    keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
    val keyPair = keyPairGenerator.generateKeyPair()
    credentialInfo.getOrPut(rpId) { mutableMapOf() }[credId.encodeBase64()] = CredentialInfo(
        user = user,
        keyPair = keyPair,
        signCount = 0,
    )
    saveResult()
    val ecPublicKey = keyPair.public as ECPublicKey
    return ECCredentialPublicKey(
        x = normalizeKey(ecPublicKey.w.affineX.toByteArray()),
        y = normalizeKey(ecPublicKey.w.affineY.toByteArray()),
    )
}

actual fun getExistingCredentials(rpId: String): Set<CredentialPair> {
    return credentialInfo.getOrPut(rpId) { mutableMapOf() }.map { (credId, credInfo) ->
        credId.decodeBase64Bytes() to credInfo.user
    }.toSet()
}

actual fun getExistingRps(): Set<String> {
    return credentialInfo.keys
}

actual fun signMessage(
    rpId: String,
    credId: ByteArray,
    messageGetter: () -> ByteArray,
): ByteArray {
    val credInfo = credentialInfo[rpId]!![credId.encodeBase64()]!!
    credentialInfo[rpId]!![credId.encodeBase64()] =
        credInfo.copy(signCount = credInfo.signCount + 1)
    saveResult()
    val privateKey = credInfo.keyPair.private as PrivateKey
    val ecdsaSign = Signature.getInstance("SHA256withECDSA")
    ecdsaSign.initSign(privateKey)
    ecdsaSign.update(messageGetter())
    return ecdsaSign.sign()
}

@OptIn(ExperimentalStdlibApi::class)
actual fun getSignCount(rpId: String, credId: ByteArray): Int {
    return credentialInfo[rpId]!![credId.encodeBase64()]!!.signCount
}