package dev.niklas.fitnessrpg.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/** Text with style first (all text in this app comes from the T.* styles). */
@Composable
fun Txt(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true
) = BasicText(text, modifier, style, overflow = overflow, softWrap = softWrap, maxLines = maxLines)

@Composable
fun Txt(text: AnnotatedString, style: TextStyle, modifier: Modifier = Modifier) = BasicText(text, modifier, style)
