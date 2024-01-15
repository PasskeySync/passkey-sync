import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import views.App
import websocket.server

fun main() = application {
    server.start()
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