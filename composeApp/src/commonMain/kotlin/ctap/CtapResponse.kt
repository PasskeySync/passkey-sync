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
}
