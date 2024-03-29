package ctap

sealed class CtapResponse(
    val data: ByteArray
)

class CtapResponseOk(
    data: ByteArray
) : CtapResponse(byteArrayOf(StatusCode.CTAP2_OK) + data)


class CtapResponseError(
    errorCode: Byte
) : CtapResponse(byteArrayOf(errorCode))


object StatusCode {
    const val CTAP2_OK = 0x00.toByte()
    const val CTAP1_ERR_INVALID_COMMAND = 0x01.toByte()
    const val CTAP1_ERR_INVALID_PARAMETER = 0x02.toByte()
    const val CTAP2_ERR_UNSUPPORTED_ALGORITHM = 0x26.toByte()
    const val CTAP2_ERR_CREDENTIAL_EXCLUDED = 0x19.toByte()
    const val CTAP2_ERR_NO_CREDENTIALS = 0x2E.toByte()
    const val CTAP2_ERR_INVALID_CREDENTIAL = 0x22.toByte()
    const val CTAP2_ERR_USER_ACTION_PENDING = 0x23.toByte()
    const val CTAP2_ERR_OPERATION_DENIED = 0x27.toByte()
}
