package com.yohanmix.kelaskuu.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val description: String,
    val buttonText: String
)

val pages = listOf(
    OnboardingPageData(
        title = "Selamat Datang di Kelas Ungu",
        description = "Apakah kamu siap untuk melatih kemampuan kamu",
        buttonText = "Selanjutnya"
    ),
    OnboardingPageData(
        title = "Cari Kelas Mu!",
        description = "Banyak pilihan kelas sesuai kemampuan",
        buttonText = "Selanjutnya"
    ),
    OnboardingPageData(
        title = "Punya Kemampuan Mengajar?",
        description = "tidak hanya belajar\nkamu bisa jadi salah satu\npengajar juga loh!",
        buttonText = "Mulai"
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit = {}) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip Button
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            TextButton(onClick = onFinish) {
                Text(text = "Skip", color = KelaskuuPurple)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Illustration Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            IllustrationPlaceholder()
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Pager Indicator
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) KelaskuuPurple else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(10.dp)
                        .background(color, RoundedCornerShape(5.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = page.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = KelaskuuPurple,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = page.description,
                    fontSize = 16.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        // Navigation Button
        Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    onFinish()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .width(180.dp)
                .height(48.dp)
                .align(Alignment.End)
                .offset(y = (-40).dp) // Adjust to match design positioning
        ) {
            Text(text = pages[pagerState.currentPage].buttonText, color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun IllustrationPlaceholder() {
    // A simple drawing to represent the classroom illustration
    Canvas(modifier = Modifier.size(280.dp, 180.dp)) {
        // Board
        drawRoundRect(
            color = Color.Gray,
            topLeft = Offset(20.dp.toPx(), 0f),
            size = androidx.compose.ui.geometry.Size(240.dp.toPx(), 120.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        // Table Left
        drawLine(Color.Black, Offset(0f, 150.dp.toPx()), Offset(60.dp.toPx(), 150.dp.toPx()), 4f)
        // Table Right
        drawLine(Color.Black, Offset(220.dp.toPx(), 150.dp.toPx()), Offset(280.dp.toPx(), 150.dp.toPx()), 4f)
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen()
}
