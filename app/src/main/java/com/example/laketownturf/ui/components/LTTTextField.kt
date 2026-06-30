package com.example.laketownturf.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.laketownturf.theme.DangerRed

/**
 * Styled text field that adapts to dark/light theme via MaterialTheme.colorScheme.
 * Green focus indicator, error state with red, and consistent styling.
 */
@Composable
fun LTTTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    prefix: @Composable (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = cs.onSurfaceVariant.copy(alpha = 0.5f)) }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            isError = error != null,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = keyboardCapitalization,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onNext = { onImeAction() },
            ),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = cs.onSurface,
                unfocusedTextColor = cs.onSurface,
                disabledTextColor = cs.onSurfaceVariant,
                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor = cs.primary,
                unfocusedBorderColor = cs.outline,
                disabledBorderColor = cs.outline.copy(alpha = 0.5f),
                errorBorderColor = DangerRed,
                focusedLabelColor = cs.primary,
                unfocusedLabelColor = cs.onSurfaceVariant,
                errorLabelColor = DangerRed,
                cursorColor = cs.primary,
                errorCursorColor = DangerRed,
                focusedLeadingIconColor = cs.primary,
                unfocusedLeadingIconColor = cs.onSurfaceVariant,
                focusedTrailingIconColor = cs.primary,
                unfocusedTrailingIconColor = cs.onSurfaceVariant,
                focusedPrefixColor = cs.onSurfaceVariant,
                unfocusedPrefixColor = cs.onSurfaceVariant,
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = DangerRed,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}
