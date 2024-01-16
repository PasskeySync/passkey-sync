package views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import states.AuthenticatorState
import java.util.Optional

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App(authenticatorState: AuthenticatorState) {
    MaterialTheme {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (authenticatorState.state) {
                AuthenticatorState.State.IDLE -> {
                    Text(
                        "PasskeySync",
                        fontSize = typography.h4.fontSize,
                        color = colors.primary,
                    )
                    Text("Cross-platform Authenticator")
                }

                AuthenticatorState.State.WAITING_FOR_USER_VERIFICATION -> {
                    val (_, user) = authenticatorState.selectedCredential.get()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "Are you sure to ${
                                if (authenticatorState.isRegister) "bind PasskeySync to" else "authenticate"
                            }"
                        )
                        UserCard(
                            user.displayName, user.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            authenticatorState.state = AuthenticatorState.State.VERIFICATION_SUCCESS
                        }
                        Text("@ ${authenticatorState.rpId.get()}")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    authenticatorState.state =
                                        AuthenticatorState.State.VERIFICATION_SUCCESS
                                }
                            ) {
                                Text("Confirm")
                            }
                            Button(
                                colors = buttonColors(
                                    backgroundColor = colors.error,
                                ),
                                onClick = {
                                    authenticatorState.state =
                                        AuthenticatorState.State.VERIFICATION_FAILED
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }

                AuthenticatorState.State.WAITING_FOR_CHOOSE_CREDENTIAL -> {
                    Text("Choose a credential to login")
                    // get screen height
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.sizeIn(maxHeight = 400.dp),
                    ) {
                        for ((id, user) in authenticatorState.credentials) {
                            item {
                                UserCard(
                                    user.displayName,
                                    user.name,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    authenticatorState.selectedCredential = Optional.of(id to user)
                                    authenticatorState.state =
                                        AuthenticatorState.State.CHOOSE_FINISHED
                                }
                            }
                        }
                    }
                    Button(
                        colors = buttonColors(
                            backgroundColor = colors.error,
                        ),
                        onClick = {
                            authenticatorState.state = AuthenticatorState.State.CHOOSE_CANCELED
                        }
                    ) {
                        Text("Cancel")
                    }
                }

                else -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = colors.secondary,
                        backgroundColor = colors.secondaryVariant,
                    )
                    Text("Processing")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UserCard(
    name: String,
    email: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        elevation = 6.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Text(
                name,
                fontSize = typography.h5.fontSize
            )
            Text(email)
        }
    }
}