package com.sonique.app.ui.component


import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.less
import sonique.composeapp.generated.resources.more

@Composable
fun ExpandableText(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontStyle: FontStyle? = null,
    text: String,
    collapsedMaxLine: Int = 3,
    showMoreText: String = "... " + stringResource(Res.string.more),
    showMoreStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.W500),
    showLessText: String = " " + stringResource(Res.string.less),
    showLessStyle: SpanStyle = showMoreStyle,
    textAlign: TextAlign? = null,
    fontSize: TextUnit,
) {
     
    var isExpanded by remember { mutableStateOf(false) }
    var clickable by remember { mutableStateOf(false) }
    var lastCharIndex by remember { mutableIntStateOf(0) }

     
    Box(
        modifier =
            Modifier
                .clickable { isExpanded = !isExpanded }
                .then(modifier),
    ) {
         
        Text(
            modifier =
                textModifier
                    .fillMaxWidth()
                    .animateContentSize(),
            text =
                buildAnnotatedString {
                    if (clickable) {
                        if (isExpanded) {
                             
                            append(text)
                            withStyle(style = showLessStyle) { append(showLessText) }
                        } else {
                             
                            val adjustText =
                                text
                                    .substring(startIndex = 0, endIndex = lastCharIndex)
                                    .dropLast(showMoreText.length)
                                    .dropLastWhile { Character.isWhitespace(it) || it == '.' }
                            append(adjustText)
                            withStyle(style = showMoreStyle) { append(showMoreText) }
                        }
                    } else {
                         
                        append(text)
                    }
                },
             
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLine,
            fontStyle = fontStyle,
             
            onTextLayout = { textLayoutResult ->
                if (!isExpanded && textLayoutResult.hasVisualOverflow) {
                    clickable = true
                    lastCharIndex =
                        if (kotlin
                                .runCatching {
                                    textLayoutResult.getLineEnd(
                                        collapsedMaxLine - 1,
                                    )
                                }.isSuccess
                        ) {
                            textLayoutResult.getLineEnd(collapsedMaxLine - 1)
                        } else {
                            text.length - 1
                        }
                }
            },
            style = style,
            textAlign = textAlign,
            fontSize = fontSize,
        )
    }
}

