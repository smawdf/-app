package com.myorderapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.myorderapp.R

@Composable
fun CandyCoinIcon(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.candy_coin),
        contentDescription = "糖糖币",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
