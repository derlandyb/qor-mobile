package br.com.qualorock.androidApp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.NightlifeGvTokens

const val ConsentCheckboxTestTag = "consentCheckbox"

/**
 * A5 — required, non-pre-checked consent acceptance (AUTH-02/AUTH-03), shared shape reused
 * across signup screens. `accepted` is hoisted — the caller owns whether it's persisted/reset.
 */
@Composable
fun ConsentCapture(accepted: Boolean, onAcceptedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = NightlifeGvTokens.Space2Dp.dp),
    ) {
        Checkbox(
            checked = accepted,
            onCheckedChange = onAcceptedChange,
            colors = CheckboxDefaults.colors(checkedColor = Color(NightlifeGvTokens.AccentPink)),
            modifier = Modifier.testTag(ConsentCheckboxTestTag),
        )
        Text(
            text = "Li e aceito os Termos de Uso e a Política de Privacidade.",
            color = Color(NightlifeGvTokens.ColorTextSecondary),
            fontSize = NightlifeGvTokens.TextBody.SizeSp.sp,
        )
    }
}
