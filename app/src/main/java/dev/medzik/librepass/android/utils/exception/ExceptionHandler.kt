package dev.medzik.librepass.android.utils.exception

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import dev.medzik.libcrypto.EncryptException
import dev.medzik.librepass.android.BuildConfig
import dev.medzik.librepass.android.R
import dev.medzik.librepass.client.errors.ApiException
import dev.medzik.librepass.client.errors.ClientException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Log exception if debugging is enabled.
 */
fun Exception.debugLog() {
    if (BuildConfig.DEBUG) {
        printStackTrace()
    }
}

/**
 * Handle exceptions. Show snackbar with error message.
 * @param context The context to get string resources
 * @param snackbar The snackbar host state
 */
@OptIn(DelicateCoroutinesApi::class)
fun Exception.handle(context: Context, snackbar: SnackbarHostState) {
    // log exception trace if debugging is enabled
    debugLog()

    when (this) {
        // handle encrypt exception
        is EncryptException -> {
            GlobalScope.launch {
                snackbar.showSnackbar(context.getString(R.string.Error_EncryptionError))
            }
        }

        // handle client exception (network error)
        is ClientException -> {
            GlobalScope.launch {
                snackbar.showSnackbar(context.getString(R.string.Error_NetworkError))
            }
        }

        // handle api exceptions
        is ApiException -> {
            GlobalScope.launch {
                snackbar.showSnackbar(getTranslatedErrorMessage(context))
            }
        }

        // handle other exceptions
        else -> {
            GlobalScope.launch {
                snackbar.showSnackbar(context.getString(R.string.Error_UnknownError))
            }
        }
    }
}
