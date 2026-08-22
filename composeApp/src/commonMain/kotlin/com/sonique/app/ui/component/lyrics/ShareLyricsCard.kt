package com.sonique.app.ui.component.lyrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.app_icon_circle

val ShareLyricsCardMaxWidth = 340.dp

internal fun Color.shareCardContentColor(): Color = if (luminance() > 0.5f) Color(0xFF141414) else Color.White

internal fun Color.shareTintOn(fill: Color): Color =
    if (fill.luminance() > 0.5f) lerp(this, Color.Black, 0.55f) else lerp(this, Color.White, 0.55f)

@Composable
internal fun ShareLyricsCard(
    lines: List<String>,
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap?,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val content = background.shareCardContentColor()
    val secondary = content.copy(alpha = 0.68f)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .widthIn(max = ShareLyricsCardMaxWidth)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(background, lerp(background, Color.Black, 0.28f))))
                .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (artwork != null) {
                Image(
                    bitmap = artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(5.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = songTitle,
                    color = content,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artistName,
                    color = secondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            lines.forEach { line ->
                Text(
                    text = line,
                    color = content,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(16.dp).clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_icon_circle),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sonique",
                color = secondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
