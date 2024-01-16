package crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import ctap.ECCredentialPublicKey
import ctap.PublicKeyCredentialUserEntity
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

fun normalizeKey(key: ByteArray): ByteArray {
    return if (key[0] == 0.toByte()) {
        key.slice(1 until key.size).toByteArray()
    } else {
        key
    }
}

actual fun generateAppKey(
    rpId: String,
    credId: ByteArray,
    user: PublicKeyCredentialUserEntity,
): ECCredentialPublicKey {
    val keyPairGenerator = KeyPairGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
    keyPairGenerator.initialize(
        KeyGenParameterSpec.Builder(
            rpId,
            KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .build())

    val ecPublicKey = keyPairGenerator.generateKeyPair().public as ECPublicKey
    return ECCredentialPublicKey(
        x = normalizeKey(ecPublicKey.w.affineX.toByteArray()),
        y = normalizeKey(ecPublicKey.w.affineY.toByteArray()),
    )
}

actual fun getExistingCredentials(rpId: String): Set<Pair<ByteArray, PublicKeyCredentialUserEntity>> {
    TODO("Not yet implemented")
}

actual fun getExistingRps(): Set<String> {
    TODO("Not yet implemented")
}

actual fun signMessage(
    rpId: String,
    credId: ByteArray,
    messageGetter: () -> ByteArray,
): ByteArray {
    val ks = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    val entry = ks.getEntry(rpId, null) as KeyStore.PrivateKeyEntry
    val ecdsaSign = Signature.getInstance("SHA256withECDSA")
    ecdsaSign.initSign(entry.privateKey)
    ecdsaSign.update(messageGetter())
    return ecdsaSign.sign()
}

actual fun getSignCount(rpId: String, credId: ByteArray): Int {
    TODO("Not yet implemented")
}