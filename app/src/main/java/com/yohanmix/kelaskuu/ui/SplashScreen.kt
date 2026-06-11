package com.yohanmix.kelaskuu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000) // Tampilkan splash selama 2 detik
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconGrid()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Kelaskuu",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = KelaskuuPurple
            )
        }
    }
}

@Composable
fun IconGrid() {
    Column {
        Row {
            RoundedSquare()
            Spacer(modifier = Modifier.width(8.dp))
            RoundedSquare()
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            RoundedSquare()
            Spacer(modifier = Modifier.width(8.dp))
            RoundedSquare()
        }
    }
}

@Composable
fun RoundedSquare() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = KelaskuuPurple,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(onTimeout = {})
}
