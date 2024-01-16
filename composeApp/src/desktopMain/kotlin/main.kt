import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ctap.Authenticator
import ctap.PublicKeyCredentialUserEntity
import ctap.install
import states.AuthenticatorState
import states.rememberAuthenticatorState
import views.App
import websocket.ktorServer
import websocket.webSocketCommunicator
import java.util.Optional

@OptIn(ExperimentalStdlibApi::class)
val AA_GUID = "4B69B9D6EF0D9769C44210A00702277A".hexToByteArray()

fun main() = application {
    ktorServer.start()
    val authenticatorState = rememberAuthenticatorState()
    val authenticator = Authenticator(
        aaGuid = AA_GUID,
        state = authenticatorState,
    ) {
        install(webSocketCommunicator)
    }

    Window(
        state = rememberWindowState(
            width = 400.dp,
            height = 800.dp,
        ),
        onCloseRequest = ::exitApplication,
        title = "PasskeySync"
    ) {
        App(authenticatorState)
    }
}

@Preview
@Composable
fun VerificationPreview() {
    App(
        rememberAuthenticatorState(
            state = AuthenticatorState.State.WAITING_FOR_USER_VERIFICATION,
            selectedCredential = Optional.of(
                "aaa".toByteArray() to PublicKeyCredentialUserEntity(
                    "test_id".toByteArray(),
                    "12112628@mail.sustech.edu.cn",
                    "LittleEtx"
                )
            ),
            rpId = Optional.of("localhost")
        )
    )
}

@Preview
@Composable
fun SelectionPreview() {
    App(
        rememberAuthenticatorState(
            state = AuthenticatorState.State.WAITING_FOR_CHOOSE_CREDENTIAL,
            credentials = setOf(
                "aaa".toByteArray() to PublicKeyCredentialUserEntity(
                    "test_id1".toByteArray(),
                    "12112628@mail.sustech.edu.cn",
                    "LittleEtx",
                ),
                "bbb".toByteArray() to PublicKeyCredentialUserEntity(
                    "test_id2".toByteArray(),
                    "002@qq.com",
                    "Triangle",
                ),
            ),
            rpId = Optional.of("localhost")
        )
    )
}