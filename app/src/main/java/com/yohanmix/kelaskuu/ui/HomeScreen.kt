package com.yohanmix.kelaskuu.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CourseItem(course: Course, onClick: () -> Unit = {}) {
    val isFree = course.price.trim().uppercase() == "FREE"
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFE1FF)), // Warna Pink
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
                        tint = KelaskuuPurple
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = " ${course.rating}  •  ${course.price}",
                        fontSize = 12.sp,
                        color = if (isFree) Color(0xFF4CAF50) else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HomeScreen(
    userRole: String,
    onEditProfileClick: () -> Unit,
    onKelaskuClick: () -> Unit,
    onBottomProfileClick: () -> Unit,
    onKelasClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onCreateCourseClick: () -> Unit = {},
    onCourseClick: (Course) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua", "Design", "Programming", "UI/UX")
    var searchQuery by remember { mutableStateOf("") }
    
    // Check if we are in Preview mode to avoid Firebase initialization error
    val isPreview = LocalInspectionMode.current
    val auth = remember { if (isPreview) null else FirebaseAuth.getInstance() }
    val db = remember { if (isPreview) null else FirebaseFirestore.getInstance() }
    
    var userName by remember { mutableStateOf("User") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    val dynamicCourses = remember { mutableStateListOf<Course>() }
    var isLoading by remember { mutableStateOf(true) }
    var unreadNotifications by remember { mutableIntStateOf(0) }

    // Logic decoding Base64 secara manual (sama dengan ProfileScreen)
    val profileBitmap: Bitmap? = remember(profileImageUrl) {
        if (!profileImageUrl.isNullOrEmpty() && profileImageUrl!!.startsWith("data:image")) {
            try {
                val base64String = profileImageUrl!!.substringAfter("base64,")
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        val currentUser = auth?.currentUser
        if (currentUser != null) {
            db?.collection("users")?.document(currentUser.uid)?.get()
                ?.addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userName = document.getString("nama") ?: "User"
                        profileImageUrl = document.getString("profileImage")
                    }
                }
            
            // Listen for unread notifications
            db?.collection("notifications")
                ?.whereEqualTo("userId", currentUser.uid)
                ?.whereEqualTo("isRead", false)
                ?.addSnapshotListener { snapshot, _ ->
                    unreadNotifications = snapshot?.size() ?: 0
                }
        }

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
                            
                            Course(id, title, rating, "Lama Belajar", icon, price, category, creatorId = creatorId, imageUrl = imageUrl)
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
                currentScreen = "Home",
                userRole = userRole,
                onHomeClick = {},
                onKelaskuClick = onKelaskuClick,
                onProfileClick = onBottomProfileClick,
                onKelasClick = onKelasClick,
                onCreateCourseClick = onCreateCourseClick
            )
        }
    ) { paddingValues ->
        val filteredCourses = dynamicCourses.filter { course ->
            val matchesCategory = if (selectedCategory == "Semua") true else course.category == selectedCategory
            val matchesSearch = course.title.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Header: Grid Icon and Profile/Notifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = KelaskuuPurple,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Notification Icon with Badge
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            IconButton(onClick = onNotificationClick) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifikasi",
                                    tint = KelaskuuPurple,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            if (unreadNotifications > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                            }
                        }

                        // Profile Circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(KelaskuuPurple)
                                .clickable { onEditProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileBitmap != null) {
                                AsyncImage(
                                    model = profileBitmap,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (!profileImageUrl.isNullOrEmpty() && !profileImageUrl!!.startsWith("data:image")) {
                                AsyncImage(
                                    model = profileImageUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Greeting
                Text(
                    text = "Hello, $userName",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Mau belajar apa nih?",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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

                // Native Ad Advanced (Menggantikan Banner "Kelas Baru")
                NativeAdViewComposable(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KelaskuuPurple.copy(alpha = 0.05f))
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section "Kelas"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kelas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Liat Semua",
                        fontSize = 14.sp,
                        color = KelaskuuPurple.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onKelasClick() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categories
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Course List with Search and Category Filtering
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KelaskuuPurple)
                    }
                }
            } else if (filteredCourses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada kelas ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredCourses) { course ->
                    CourseItem(course, onClick = { onCourseClick(course) })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                // Banner Iklan di bawah daftar kursus
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BannerAdView()
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(userRole = "pelajar", onEditProfileClick = {}, onKelaskuClick = {}, onBottomProfileClick = {}, onKelasClick = {}, onNotificationClick = {})
}
