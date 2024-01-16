package crypto

import ctap.ECCredentialPublicKey
import ctap.PublicKeyCredentialUserEntity
import kotlin.random.Random

expect fun getSignCount(rpId: String, credId: ByteArray): Int

expect fun generateAppKey(
    rpId: String,
    credId: ByteArray,
    user: PublicKeyCredentialUserEntity,
): ECCredentialPublicKey

typealias CredentialPair = Pair<ByteArray, PublicKeyCredentialUserEntity>

expect fun getExistingCredentials(rpId: String): Set<CredentialPair>

expect fun getExistingRps(): Set<String>

expect fun signMessage(
    rpId: String,
    credId: ByteArray,
    messageGetter: () -> ByteArray,
): ByteArray

fun generateCredentialId(): ByteArray {
    return Random.nextBytes(32)
}
