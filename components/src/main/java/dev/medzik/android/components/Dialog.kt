package dev.medzik.android.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun rememberDialogState() = remember { DialogState() }

/** A visibility controller for a dialog */
class DialogState {
    var isVisible by mutableStateOf(false)

    /** Show the dialog. */
    fun show() {
        isVisible = true
    }

    /** Hide the dialog. */
    fun hide() {
        isVisible = false
    }
}

/**
 * A composable function for displaying a basic dialog.
 * @param state the state that controls visibility of the dialog
 * @param content the content of the dialog to display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseDialog(
    state: DialogState,
    content: @Composable () -> Unit
) {
    if (state.isVisible) {
        AlertDialog(onDismissRequest = { state.hide() }) {
            Surface(
                shape = AlertDialogDefaults.shape,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun BaseDialogPreview() {
    val state = rememberDialogState()
    state.show()

    Surface {
        BaseDialog(state) {
            Column {
                Text(
                    text = "Example Dialog",
                    fontWeight = FontWeight.Black,
                    modifier =
                        Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 8.dp)
                )

                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 24.dp)
                ) {
                    Text("Some text")
                }
            }
        }
    }
}

/**
 * A composable function for displaying a picker dialog with a list of items.
 * @param state the state that controls visibility of the dialog
 * @param title the title to display at the top of the dialog
 * @param items the list of items to display in the picker
 * @param onSelected a callback function invoked when the item is selected
 * @param content composable lambda that defines the visual representation of each item in the picker
 */
@Composable
fun <T> PickerDialog(
    state: DialogState,
    title: String,
    items: List<T>,
    onSelected: (T) -> Unit,
    content: @Composable (T) -> Unit
) {
    BaseDialog(state) {
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                modifier =
                    Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp)
            )

            items.forEach { item ->
                Box(
                    modifier =
                        Modifier
                            .clickable {
                                onSelected(item)
                                state.hide()
                            }
                ) {
                    Row(
                        modifier =
                            Modifier
                                .padding(horizontal = 24.dp)
                    ) {
                        content(item)
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun PickerDialogPreview() {
    val state = rememberDialogState()
    state.show()

    val items = listOf("First", "Second", "Third")

    Surface {
        PickerDialog(
            state,
            title = "Example Picker Dialog",
            items,
            onSelected = {}
        ) {
            Text(
                modifier =
                    Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(),
                text = it
            )
        }
    }
}
