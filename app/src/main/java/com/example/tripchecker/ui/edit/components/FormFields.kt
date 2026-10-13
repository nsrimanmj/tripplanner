package com.example.tripchecker.ui.edit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType

/** Outlined single-line text field with the label, placeholder and supporting-text conventions of the edit form. */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, maxLines = 1) },
        placeholder = placeholder?.let { { Text(it, maxLines = 1) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = textStyle,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
    )
}

/** Read-only field that opens a picker dialog when tapped; [trailingIcon] hints at calendar or clock. */
@Composable
fun PickerField(
    value: String,
    label: String,
    placeholder: String,
    trailingIcon: Painter,
    onClickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, maxLines = 1) },
            placeholder = { Text(placeholder, maxLines = 1) },
            supportingText = supportingText?.let { { Text(it) } },
            trailingIcon = { Icon(trailingIcon, contentDescription = null) },
            isError = isError,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick),
        )
    }
}
