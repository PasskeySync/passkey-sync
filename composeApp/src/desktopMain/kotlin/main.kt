import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ctap.Authenticator
import ctap.install
import states.rememberAuthenticatorState
import views.App
import websocket.ktorServer
import websocket.webSocketCommunicator

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
        App()
    }
}

@Preview
@Composable
fun AppDesktopPreview() {
    App()
}