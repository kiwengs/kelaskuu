package com.yohanmix.kelaskuu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple
import com.google.firebase.firestore.FirebaseFirestore

data class AvailableCourse(
    val id: String,
    val title: String,
    val rating: Double,
    val price: String,
    val icon: ImageVector,
    val category: String = "Semua",
    val imageUrl: String? = null,
    val creatorId: String? = null
)

@Composable
fun AvailableCourseItem(course: AvailableCourse, userRole: String, onClick: () -> Unit) {
    val isAdminOrOwner = userRole.lowercase() == "admin" || userRole.lowercase() == "owner"
    val isFree = course.price.trim().uppercase() == "FREE"
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                        text = if (isAdminOrOwner) "Gratis (Admin)" else course.price,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdminOrOwner || isFree) Color(0xFF4CAF50) else Color.Red
                    )
                }
            }
            // Tombol Beli (Sekarang memicu Iklan)
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                val buttonText = when {
                    isAdminOrOwner -> "Cek"
                    isFree -> "Tonton Iklan"
                    else -> "Beli Kelas"
                }
                Text(buttonText, fontSize = 10.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun AvailableCoursesScreen(
    userRole: String = "pelajar",
    purchasedCourseIds: List<String>,
    onHomeClick: () -> Unit,
    onKelaskuClick: () -> Unit,
    onProfileClick: () -> Unit,
    onKelasClick: () -> Unit,
    onCreateCourseClick: () -> Unit = {},
    onCourseClick: (AvailableCourse) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua", "Design", "Programming", "UI/UX")
    var searchQuery by remember { mutableStateOf("") }
    
    val isPreview = LocalInspectionMode.current
    val db = remember { if (isPreview) null else FirebaseFirestore.getInstance() }
    val dynamicCourses = remember { mutableStateListOf<AvailableCourse>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (db != null) {
            db.collection("available_courses")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    
                    if (snapshot != null) {
                        val courses = snapshot.documents.mapNotNull { doc ->
                            val id = doc.id
                            val title = doc.getString("title") ?: return@mapNotNull null
                            val category = doc.getString("category") ?: "Semua"
                            val price = doc.getString("price") ?: "FREE"
                            val rating = doc.getDouble("rating") ?: 5.0
                            val imageUrl = doc.getString("headerImage")
                            val creatorId = doc.getString("creatorId")
                            
                            // Map category to Icon
                            val icon = when (category) {
                                "Design" -> Icons.Default.Brush
                                "Programming" -> Icons.Default.Code
                                "UI/UX" -> Icons.Default.Draw
                                else -> Icons.Default.MenuBook
                            }
                            
                            AvailableCourse(id, title, rating, price, icon, category, imageUrl, creatorId)
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

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "Kelas",
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

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search..", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = KelaskuuPurple.copy(alpha = 0.5f),
                    focusedBorderColor = KelaskuuPurple,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KelaskuuPurple)
                }
            } else {
                // Course List with Filter
                val filteredCourses = dynamicCourses.filter { course ->
                    val isNotPurchased = course.id !in purchasedCourseIds
                    val matchesCategory = if (selectedCategory == "Semua") true else course.category == selectedCategory
                    val matchesSearch = course.title.contains(searchQuery, ignoreCase = true)
                    isNotPurchased && matchesCategory && matchesSearch
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredCourses) { course ->
                        AvailableCourseItem(course, userRole, onClick = { onCourseClick(course) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AvailableCoursesScreenPreview() {
    AvailableCoursesScreen(
        purchasedCourseIds = emptyList(),
        onHomeClick = {},
        onKelaskuClick = {},
        onProfileClick = {},
        onKelasClick = {},
        onCourseClick = {}
    )
}
