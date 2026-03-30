package com.sonique.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sonique.app.extension.shimmer
import com.sonique.app.ui.theme.shimmerBackground

@Composable
fun HomeItemShimmer() {
    Column {
        Box(
            Modifier
                .width(150.dp)
                .height(36.dp)
                .padding(vertical = 8.dp)
                .background(
                    color = shimmerBackground,
                ).clip(RoundedCornerShape(8.dp))
                .shimmer(),
        )
        LazyRow(userScrollEnabled = false) {
            items(10) {
                PlaylistShimmer()
            }
        }
    }
}

@Composable
fun PlaylistShimmer() {
    Column(
        Modifier
            .height(260.dp)
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    color = shimmerBackground,
                ).shimmer(),
        )
        Spacer(modifier = Modifier.size(10.dp))
        Box(
            Modifier
                .width(130.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    color = shimmerBackground,
                ).shimmer(),
        )
        Spacer(modifier = Modifier.size(10.dp))
        Box(
            Modifier
                .width(100.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    color = shimmerBackground,
                ).shimmer(),
        )
    }
}

@Composable
fun SpeedDialShimmerItem(modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(shimmerBackground)
            .shimmer(),
    )
}

@Composable
fun SpeedDialShimmer() {
    Column(Modifier.padding(bottom = 20.dp)) {
        Box(
            Modifier
                .width(150.dp)
                .height(36.dp)
                .padding(vertical = 8.dp)
                .background(
                    color = shimmerBackground,
                ).clip(RoundedCornerShape(8.dp))
                .shimmer(),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(3) {
                        SpeedDialShimmerItem(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeShimmer() {
    Column(
        Modifier.padding(horizontal = 15.dp),
    ) {
        SpeedDialShimmer()
        LazyColumn(userScrollEnabled = false) {
            items(10) {
                HomeItemShimmer()
            }
        }
    }
}

@Composable
fun ShimmerSearchItem() {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
         
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBackground)
                .shimmer()
        )

        Spacer(modifier = Modifier.width(12.dp))

         
        Column {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBackground)
                    .shimmer()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBackground)
                    .shimmer()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBackground)
                    .shimmer()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShimmerSearchItemPreview() {
    ShimmerSearchItem()
}


