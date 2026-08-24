package com.lumenconnection.stream.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenconnection.stream.ui.theme.Lumen

/**
 * Componentes que replicam os helpers de `src/ui/theme.rs` do desktop:
 * card_frame, page_header, accent_button, ghost_button e nav_item.
 * Mantê-los aqui garante que as duas versões do app tenham a mesma linguagem
 * visual: mesmas cores, mesmos raios, mesmos espaçamentos.
 */

/** Equivalente ao card_frame(): fill bg_card, raio 10, borda 1px, margem 18. */
@Composable
fun LumenCard(
    modifier: Modifier = Modifier,
    contentPadding: Int = Lumen.dimens.cardMargin,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Lumen.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Lumen.dimens.cardRounding.dp),
        colors = CardDefaults.cardColors(containerColor = c.bgCard, contentColor = c.text),
        border = BorderStroke(1.dp, c.border),
    ) {
        Column(Modifier.padding(contentPadding.dp), content = content)
    }
}

/**
 * Equivalente ao page_header(), com o losango laranja que a Home do desktop
 * desenha antes do título.
 */
@Composable
fun PageHeader(title: String, subtitle: String = "", diamond: Boolean = true) {
    val c = Lumen.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (diamond) {
            Text("◆", color = c.accent, fontSize = 26.sp)
            Spacer(Modifier.width(10.dp))
        }
        Text(title, color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
    if (subtitle.isNotEmpty()) {
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = c.textMuted, fontSize = 14.sp)
    }
}

/** Rótulo de seção dentro de um cartão (o home_quick do desktop). */
@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Lumen.colors.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** accent_button(): fundo laranja, texto branco, raio 8. */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(Lumen.dimens.widgetRounding.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Lumen.colors.accent,
            contentColor = Color.White,
            disabledContainerColor = Lumen.colors.bgCardHover,
            disabledContentColor = Lumen.colors.textFaint,
        ),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** ghost_button(): fundo do cartão, texto normal, borda sutil. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Lumen.colors
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(Lumen.dimens.widgetRounding.dp),
        border = BorderStroke(1.dp, c.border),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.bgCard,
            contentColor = c.text,
            disabledContainerColor = c.bgCard,
            disabledContentColor = c.textFaint,
        ),
    ) {
        Text(text, fontSize = 15.sp)
    }
}

/**
 * nav_item(): 46dp de altura, raio 10, fundo accent_soft quando ativo e uma
 * barra laranja de 3dp à esquerda. O texto do item ativo continua na cor normal,
 * nunca vira laranja, igual ao comentário no código do desktop.
 */
@Composable
fun NavItem(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = Lumen.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(
                color = if (selected) c.accentSoft else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        if (selected) {
            Box(
                Modifier
                    .padding(vertical = 9.dp)
                    .width(3.dp)
                    .height(28.dp)
                    .align(Alignment.CenterStart)
                    .background(c.accent, RoundedCornerShape(2.dp)),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, fontSize = 20.sp, color = c.text)
            Spacer(Modifier.width(14.dp))
            Text(label, fontSize = 16.sp, color = c.text)
        }
    }
}

/** Campo de texto no estilo do desktop: bg_input + borda, foco laranja. */
@Composable
fun LumenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val c = Lumen.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, color = c.textFaint, fontSize = 15.sp) },
        singleLine = singleLine,
        shape = RoundedCornerShape(Lumen.dimens.widgetRounding.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.bgInput,
            unfocusedContainerColor = c.bgInput,
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.border,
            focusedTextColor = c.text,
            unfocusedTextColor = c.text,
            cursorColor = c.accent,
        ),
    )
}

/** Linha rótulo + valor usada nos cartões de detalhe e ajuda. */
@Composable
fun InfoRow(label: String, value: String) {
    val c = Lumen.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = c.textMuted, fontSize = 13.sp)
        Text(value, color = c.text, fontSize = 13.sp)
    }
}
