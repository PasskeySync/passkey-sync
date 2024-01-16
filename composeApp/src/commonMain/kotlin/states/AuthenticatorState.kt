package states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import crypto.CredentialPair
import java.util.Optional

interface AuthenticatorState {
    enum class State {
        IDLE,
        PROCESSING,
        WAITING_FOR_USER_VERIFICATION,
        VERIFICATION_SUCCESS,
        VERIFICATION_FAILED,
        WAITING_FOR_CHOOSE_CREDENTIAL,
        CHOOSE_FINISHED,
        CHOOSE_CANCELED,
    }

    var state: State
    var rpId: Optional<String>
    var credentials: Set<CredentialPair>
    var selectedCredential: Optional<CredentialPair>
    var isRegister: Boolean
}

@Composable
fun rememberAuthenticatorState(
    state: AuthenticatorState.State = AuthenticatorState.State.IDLE,
    credentials: Set<CredentialPair> = emptySet(),
    selectedCredential: Optional<CredentialPair> = Optional.empty(),
    rpId: Optional<String> = Optional.empty(),
): AuthenticatorState = rememberSaveable {
    AuthenticatorStateImpl(
        state = state,
        credentials = credentials,
        selectedCredential = selectedCredential,
        rpId = rpId,
    )
}

private class AuthenticatorStateImpl(
    state: AuthenticatorState.State,
    credentials: Set<CredentialPair>,
    selectedCredential: Optional<CredentialPair>,
    rpId: Optional<String>,
    isRegister: Boolean = false,
) : AuthenticatorState {
    override var state by mutableStateOf(state)
    override var credentials by mutableStateOf(credentials)
    override var selectedCredential by mutableStateOf(selectedCredential)
    override var rpId by mutableStateOf(rpId)
    override var isRegister by mutableStateOf(isRegister)
}