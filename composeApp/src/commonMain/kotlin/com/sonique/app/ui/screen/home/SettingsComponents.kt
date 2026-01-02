package com.sonique.app.ui.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sonique.app.ui.theme.typo
import com.sonique.app.ui.theme.white
import org.jetbrains.compose.resources.painterResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_people_alt_24

@Composable
fun ProfileHeader(
    name: String,
    avatarUrl: String?,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar",
            modifier = Modifier.size(100.dp).clip(CircleShape),
            placeholder = painterResource(Res.drawable.baseline_people_alt_24),
            error = painterResource(Res.drawable.baseline_people_alt_24),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(16.dp))
        Text(name, style = typo().headlineSmall, color = white)
    }
}







