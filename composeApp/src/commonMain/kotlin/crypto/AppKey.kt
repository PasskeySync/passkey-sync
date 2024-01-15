package crypto

import ctap.ECCredentialPublicKey

expect fun generateAppKey(keyAlias: String): ECCredentialPublicKey
expect fun signMessage(keyAlias: String, message: ByteArray): ByteArray