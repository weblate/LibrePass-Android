package dev.medzik.librepass.android.ui.screens.vault

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import dev.medzik.android.components.navigate
import dev.medzik.android.components.rememberMutableBoolean
import dev.medzik.librepass.android.data.CipherTable
import dev.medzik.librepass.android.data.getRepository
import dev.medzik.librepass.android.ui.Argument
import dev.medzik.librepass.android.ui.Screen
import dev.medzik.librepass.android.ui.components.CipherCard
import dev.medzik.librepass.android.utils.SecretStore.getUserSecrets
import dev.medzik.librepass.android.utils.showErrorToast
import dev.medzik.librepass.client.Server
import dev.medzik.librepass.client.api.CipherClient
import dev.medzik.librepass.types.cipher.Cipher
import dev.medzik.librepass.types.cipher.CipherType
import dev.medzik.librepass.types.cipher.data.CipherLoginData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun VaultScreen(navController: NavController) {
    val context = LocalContext.current

    val userSecrets =
        context.getUserSecrets()
            ?: return

    val scope = rememberCoroutineScope()

    // states
    var refreshing by rememberMutableBoolean()
    var ciphers by remember { mutableStateOf(listOf<Cipher>()) }

    // database repository
    val repository = context.getRepository()
    val credentials = repository.credentials.get()!!

    val cipherClient =
        CipherClient(
            apiKey = credentials.apiKey,
            apiUrl = credentials.apiUrl ?: Server.PRODUCTION
        )

    // get ciphers from local repository and update UI
    fun updateLocalCiphers() {
        val dbCiphers = repository.cipher.getAll(credentials.userId)

        // decrypt ciphers
        val decryptedCiphers =
            dbCiphers.map {
                try {
                    Cipher(it.encryptedCipher, userSecrets.secretKey)
                } catch (e: Exception) {
                    Cipher(
                        id = it.encryptedCipher.id,
                        owner = it.encryptedCipher.owner,
                        type = CipherType.Login,
                        loginData =
                            CipherLoginData(
                                name = "Encryption error"
                            )
                    )
                }
            }

        // sort ciphers by name and update UI
        ciphers =
            decryptedCiphers.sortedBy {
                when (it.type) {
                    CipherType.Login -> {
                        it.loginData!!.name
                    }
                    CipherType.SecureNote -> {
                        it.secureNoteData!!.title
                    }
                    CipherType.Card -> {
                        it.cardData!!.name
                    }
                }
            }
    }

    // Update ciphers from API and local database and update UI
    fun updateCiphers() =
        scope.launch(Dispatchers.IO) {
            // set loading state to true
            refreshing = true

            try {
                // caching
                val cachedCiphers = repository.cipher.getAllIDs(credentials.userId)
                val lastSync = repository.credentials.get()!!.lastSync

                if (lastSync != null) {
                    // update last sync date
                    repository.credentials.update(credentials.copy(lastSync = Date().time / 1000))

                    // get ciphers from API
                    val syncResponse = cipherClient.sync(Date(lastSync * 1000))

                    // delete ciphers from the local database that are not in API response
                    for (cipher in cachedCiphers) {
                        if (cipher !in syncResponse.ids) {
                            repository.cipher.delete(cipher)
                        }
                    }

                    // update ciphers in the local database
                    for (cipher in syncResponse.ciphers) {
                        repository.cipher.insert(
                            CipherTable(
                                id = cipher.id,
                                owner = cipher.owner,
                                encryptedCipher = cipher
                            )
                        )
                    }
                } else {
                    // update last sync date
                    repository.credentials.update(credentials.copy(lastSync = Date().time / 1000))

                    // get all ciphers from API
                    val ciphersResponse = cipherClient.getAll()

                    // insert ciphers into the local database
                    for (cipher in ciphersResponse) {
                        repository.cipher.insert(
                            CipherTable(
                                id = cipher.id,
                                owner = cipher.owner,
                                encryptedCipher = cipher
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.showErrorToast(context)
            } finally {
                // get cipher from local repository and update UI
                updateLocalCiphers()

                // set loading state to false
                refreshing = false
            }
        }

    // load ciphers from cache on start
    LaunchedEffect(scope) {
        // get ciphers from the local database and update UI
        updateLocalCiphers()

        // update ciphers from API and update UI
        // and show loading indicator while updating
        // after local ciphers are loaded to prevent empty screen
        updateCiphers()
    }

    // Google has not yet added pull refresh to material3 for reasons unknown to me.
    @Suppress("DEPRECATION")
    SwipeRefresh(
        state = rememberSwipeRefreshState(refreshing),
        onRefresh = { updateCiphers() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(ciphers.size) { index ->
                CipherCard(
                    cipher = ciphers[index],
                    onClick = { cipher ->
                        navController.navigate(
                            screen = Screen.CipherView,
                            args = arrayOf(Argument.CipherId to cipher.id.toString())
                        )
                    },
                    onEdit = { cipher ->
                        navController.navigate(
                            screen = Screen.CipherEdit,
                            args = arrayOf(Argument.CipherId to cipher.id.toString())
                        )
                    },
                    onDelete = { cipher ->
                        scope.launch(Dispatchers.IO) {
                            try {
                                cipherClient.delete(cipher.id)
                                repository.cipher.delete(cipher.id)

                                ciphers = ciphers.filter { it.id != cipher.id }
                            } catch (e: Exception) {
                                e.showErrorToast(context)
                            }
                        }
                    }
                )
            }

            item {
                Spacer(
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }
}
