package com.sonique.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.poppins_medium

@Composable
fun fontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.poppins_medium, FontWeight.Normal, FontStyle.Normal),
    )

@Composable
fun typo(): Typography {
    val fontFamily = fontFamily()

    val typo =
        Typography(
            /***
             * This typo().is use for the title of the Playlist, Artist, Song, Album, etc. in Home, Mood, Genre, Playlist, etc.
             */
            titleSmall =
                TextStyle(
                    fontSize = 15.sp, // Apple subheads
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = Color.White,
                ),
            titleMedium =
                TextStyle(
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = Color.White,
                ),
            titleLarge =
                TextStyle(
                    fontSize = 28.sp, 
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = Color.White,
                ),
            bodySmall =
                TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = musica_grey_text,
                ),
            bodyMedium =
                TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = musica_grey_text,
                ),
            bodyLarge =
                TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = musica_grey_text,
                ),
            displayLarge =
                TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = Color.White,
                ),
            headlineMedium =
                TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = Color.White,
                ),
            headlineLarge =
                TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = Color.White,
                ),
            labelMedium =
                TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = musica_grey_text,
                ),
            labelSmall =
                TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = fontFamily,
                    color = musica_grey_text,
                ),
            // ...
        )
    return typo
}

