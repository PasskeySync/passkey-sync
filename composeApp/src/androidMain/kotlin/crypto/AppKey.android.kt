package crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import ctap.ECCredentialPublicKey
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec


actual fun generateAppKey(keyAlias: String): ECCredentialPublicKey {
    val keyPairGenerator = KeyPairGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
    keyPairGenerator.initialize(
        KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .build())

    val ecPublicKey = keyPairGenerator.generateKeyPair().public as ECPublicKey
    return ECCredentialPublicKey(
        ecPublicKey.w.affineX.toByteArray(),
        ecPublicKey.w.affineY.toByteArray(),
    )
}

actual fun signMessage(keyAlias: String, message: ByteArray): ByteArray {
    val ks = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    val entry = ks.getEntry(keyAlias, null) as KeyStore.PrivateKeyEntry
    val ecdsaSign = Signature.getInstance("SHA256withECDSA")
    ecdsaSign.initSign(entry.privateKey)
    ecdsaSign.update(message)
    return ecdsaSign.sign()
}