package com.yohanmix.kelaskuu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MyCourseItem(course: Course, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!course.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = course.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = course.icon,
                        contentDescription = null,
                        tint = KelaskuuPurple,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " ${course.rating}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = course.duration,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A8A)
                    )
                }
            }
        }
    }
}

@Composable
fun MyCoursesScreen(
    userRole: String = "pelajar",
    purchasedCourseIds: List<String>,
    onHomeClick: () -> Unit,
    onKelaskuClick: () -> Unit,
    onProfileClick: () -> Unit,
    onKelasClick: () -> Unit,
    onCreateCourseClick: () -> Unit = {},
    onCourseClick: (Course) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua", "Design", "Programming", "UI/UX")

    val isPreview = LocalInspectionMode.current
    val db = remember { if (isPreview) null else FirebaseFirestore.getInstance() }
    val dynamicCourses = remember { mutableStateListOf<Course>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (db != null) {
            db.collection("available_courses")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val courses = snapshot.documents.mapNotNull { doc ->
                            val id = doc.id
                            val title = doc.getString("title") ?: return@mapNotNull null
                            val category = doc.getString("category") ?: "Semua"
                            val rating = doc.getDouble("rating") ?: 5.0
                            val imageUrl = doc.getString("headerImage")
                            
                            val icon = when (category) {
                                "Design" -> Icons.Default.Brush
                                "Programming" -> Icons.Default.Code
                                "UI/UX" -> Icons.Default.Draw
                                else -> Icons.Default.MenuBook
                            }
                            
                            Course(id, title, rating, "Lama Belajar", icon, category = category, imageUrl = imageUrl)
                        }
                        dynamicCourses.clear()
                        dynamicCourses.addAll(courses)
                        isLoading = false
                    }
                }
        } else {
            isLoading = false
        }
    }

    // Filter hanya kelas yang sudah dibeli oleh user
    val userPurchasedCourses = dynamicCourses.filter { it.id in purchasedCourseIds }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "Kelasku",
                userRole = userRole,
                onHomeClick = onHomeClick,
                onKelaskuClick = onKelaskuClick,
                onProfileClick = onProfileClick,
                onKelasClick = onKelasClick,
                onCreateCourseClick = onCreateCourseClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KelaskuuPurple)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Text(
                        text = "Kelas Baru!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onKelasClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B6BCB)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Lihat Sekarang", fontSize = 14.sp, color = Color.White)
                    }
                }
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KelaskuuPurple)
                }
            } else if (userPurchasedCourses.isEmpty()) {
                // Tampilan jika MyCourses masih kosong
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum Ada Kelas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ayo beli kelas favoritmu dan mulai belajar sekarang!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onKelasClick,
                        colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple)
                    ) {
                        Text("Cari Kelas")
                    }
                }
            } else {
                // Categories
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        Surface(
                            onClick = { selectedCategory = category },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) KelaskuuPurple else Color.Transparent,
                        ) {
                            Text(
                                text = category,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Course List
                val filteredCourses = if (selectedCategory == "Semua") {
                    userPurchasedCourses
                } else {
                    userPurchasedCourses.filter { it.category == selectedCategory }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredCourses) { course ->
                        MyCourseItem(course, onClick = { onCourseClick(course) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MyCoursesScreenPreview() {
    MyCoursesScreen(
        purchasedCourseIds = emptyList(),
        onHomeClick = {},
        onKelaskuClick = {},
        onProfileClick = {},
        onKelasClick = {},
        onCourseClick = {}
    )
}
