package crypto

import ctap.ECCredentialPublicKey
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

const val filePath = "appKey.dat"

val keys: MutableMap<String, PrivateKey> by lazy {
    val file = File(filePath)
    if (file.exists()) {
        FileInputStream(filePath).use {
            ObjectInputStream(it).readObject() as MutableMap<String, PrivateKey>
        }
    } else {
        file.createNewFile()
        mutableMapOf()
    }
}

fun savePrivateKey(keyAlias: String, privateKey: PrivateKey) {
    keys[keyAlias] = privateKey
    FileOutputStream(filePath).use {
        ObjectOutputStream(it).writeObject(keys)
    }
}

fun getPrivateKey(keyAlias: String): PrivateKey {
    return keys[keyAlias] ?: throw Exception("Key not found")
}

actual fun generateAppKey(keyAlias: String): ECCredentialPublicKey {
    val keyPairGenerator = KeyPairGenerator.getInstance("EC")
    keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
    val keyPair = keyPairGenerator.generateKeyPair()
    savePrivateKey(keyAlias, keyPair.private)
    val ecPublicKey = keyPair.public as ECPublicKey
    return ECCredentialPublicKey(
        ecPublicKey.w.affineX.toByteArray(),
        ecPublicKey.w.affineY.toByteArray(),
    )
}

actual fun signMessage(keyAlias: String, message: ByteArray): ByteArray {
    val privateKey = getPrivateKey(keyAlias)
    val ecdsaSign = Signature.getInstance("SHA256withECDSA")
    ecdsaSign.initSign(privateKey)
    ecdsaSign.update(message)
    return ecdsaSign.sign()
}