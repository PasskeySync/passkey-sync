package states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

interface AuthenticatorState {
    var signCount: Int
}


@Composable
fun rememberAuthenticatorState(
    signCount: Int = 0,
): AuthenticatorState = rememberSaveable(saver = AuthenticatorStateImpl.Saver()) {
    AuthenticatorStateImpl(
        signCount = signCount
    )
}

private class AuthenticatorStateImpl(
    signCount: Int,
) : AuthenticatorState {
    override var signCount by mutableStateOf(signCount)
    companion object {
        fun Saver() = listSaver<AuthenticatorState, Any>(
            save = { listOf(it.signCount) },
            restore = {
                AuthenticatorStateImpl(
                    signCount = it[0] as Int
                )
            }
        )
    }
}