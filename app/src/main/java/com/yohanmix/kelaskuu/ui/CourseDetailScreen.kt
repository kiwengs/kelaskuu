package com.yohanmix.kelaskuu.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
fun CourseDetailScreen(
    courseId: String,
    courseTitle: String,
    rating: Double,
    priceOrDuration: String,
    icon: ImageVector,
    isPurchased: Boolean,
    userRole: String = "pelajar",
    imageUrl: String? = null,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onKelaskuClick: () -> Unit,
    onKelasClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateCourseClick: () -> Unit = {},
    onLessonDetailClick: (Lesson) -> Unit = {},
    onQuizClick: (List<Question>) -> Unit = {},
    onEditCourseClick: () -> Unit = {},
    onBuySuccess: () -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val db = remember { if (isPreview) null else FirebaseFirestore.getInstance() }
    val auth = remember { if (isPreview) null else FirebaseAuth.getInstance() }
    
    var instructorDisplayName by remember { mutableStateOf("Loading...") }
    var instructorProfileImage by remember { mutableStateOf<String?>(null) }
    var creatorId by remember { mutableStateOf<String?>(null) }
    var customHeaderImage by remember { mutableStateOf(imageUrl) }
    val lessons = remember { mutableStateListOf<Lesson>() }
    val quizQuestions = remember { mutableStateListOf<Question>() }
    var isLoading by remember { mutableStateOf(true) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    val instructorBitmap: Bitmap? = remember(instructorProfileImage) {
        if (!instructorProfileImage.isNullOrEmpty() && instructorProfileImage!!.startsWith("data:image")) {
            try {
                val base64String = instructorProfileImage!!.substringAfter("base64,")
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    LaunchedEffect(courseId) {
        if (db != null) {
            db.collection("available_courses").document(courseId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val cid = document.getString("creatorId")
                        creatorId = cid
                        customHeaderImage = document.getString("headerImage") ?: imageUrl
                        
                        val lessonsList = document.get("lessons") as? List<Map<String, Any>>
                        lessons.clear()
                        lessonsList?.forEach { lessonMap ->
                            lessons.add(Lesson(
                                title = lessonMap["title"] as? String ?: "",
                                description = lessonMap["description"] as? String ?: "",
                                videoUrl = lessonMap["videoUrl"] as? String ?: ""
                            ))
                        }

                        val quizData = document.get("quiz") as? Map<String, Any>
                        val questionsList = quizData?.get("questions") as? List<Map<String, Any>>
                        quizQuestions.clear()
                        questionsList?.forEach { q ->
                            quizQuestions.add(Question(
                                text = q["text"] as? String ?: "",
                                type = QuestionType.valueOf(q["type"] as? String ?: "MULTIPLE_CHOICE"),
                                options = (q["options"] as? List<String>) ?: listOf("", "", "", ""),
                                correctAnswerIndex = (q["correctAnswerIndex"] as? Long)?.toInt() ?: 0,
                                essayAnswer = q["essayAnswer"] as? String ?: ""
                            ))
                        }

                        if (cid != null) {
                            db.collection("users").document(cid).get()
                                .addOnSuccessListener { userDoc ->
                                    val name = userDoc.getString("nama") ?: "Pengajar"
                                    val role = userDoc.getString("role") ?: "pengajar"
                                    instructorDisplayName = "$name-$role"
                                    instructorProfileImage = userDoc.getString("profileImage")
                                    isLoading = false
                                }
                                .addOnFailureListener {
                                    instructorDisplayName = "Pengajar"
                                    isLoading = false
                                }
                        } else {
                            instructorDisplayName = "Admin"
                            isLoading = false
                        }
                    } else {
                        isLoading = false
                    }
                }
        } else {
            // Mock data for preview
            lessons.add(Lesson("Pengenalan", "Selamat datang di kelas!", ""))
            instructorDisplayName = "John-pengajar"
            isLoading = false
        }
    }

    val isCreator = auth?.currentUser?.uid != null && auth.currentUser?.uid == creatorId
    val isAdminOrOwner = userRole.lowercase() == "admin" || userRole.lowercase() == "owner"

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Apakah kamu yakin $userRole akan menghapus course ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        if (db != null) {
                            db.collection("available_courses").document(courseId).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Course berhasil dihapus", Toast.LENGTH_SHORT).show()
                                    onBackClick()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Ya")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Tidak")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = if (isPurchased) "Kelasku" else "Kelas",
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

            // Back Button and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = KelaskuuPurple
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Detail Kelas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row {
                    if (isCreator) {
                        IconButton(onClick = onEditCourseClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Course",
                                tint = KelaskuuPurple
                            )
                        }
                    }
                    if (isAdminOrOwner) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Course",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Header Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!customHeaderImage.isNullOrEmpty()) {
                                AsyncImage(
                                    model = customHeaderImage,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = KelaskuuPurple,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = courseTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
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
                                    text = " $rating",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                if (!isPurchased) {
                                    val isFree = priceOrDuration.trim().uppercase() == "FREE"
                                    Text(
                                        text = if (isAdminOrOwner) "GRATIS (ADMIN)" else if (isFree) "FREE" else priceOrDuration,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAdminOrOwner || isFree) Color(0xFF4CAF50) else Color.Red
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = " $priceOrDuration",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(KelaskuuPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            if (instructorBitmap != null) {
                                AsyncImage(
                                    model = instructorBitmap,
                                    contentDescription = "Instructor Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (!instructorProfileImage.isNullOrEmpty() && !instructorProfileImage!!.startsWith("data:image")) {
                                AsyncImage(
                                    model = instructorProfileImage,
                                    contentDescription = "Instructor Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = instructorDisplayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lessons List
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KelaskuuPurple)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(lessons) { lesson ->
                        LessonCard(
                            index = lessons.indexOf(lesson) + 1,
                            lesson = lesson,
                            isAccessible = isPurchased,
                            onClick = {
                                if (!isPurchased) {
                                    val isFree = priceOrDuration.trim().uppercase() == "FREE"
                                    val msg = if (isAdminOrOwner) 
                                        "Silakan tekan tombol Cek di bawah untuk membuka akses"
                                        else if (isFree) 
                                        "anda harus menonton iklan untuk mengakses kelas ini" 
                                        else "Silakan melakukan pembayaran untuk mengakses kelas ini"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    onLessonDetailClick(lesson)
                                }
                            }
                        )
                    }
                    
                    if (isPurchased && quizQuestions.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onQuizClick(quizQuestions) },
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
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
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Quiz,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Ujian Akhir Kelas",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${quizQuestions.size} Pertanyaan",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Buy Button
            if (!isPurchased) {
                Button(
                    onClick = onBuySuccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple)
                ) {
                    val buttonText = when {
                        isAdminOrOwner -> "Cek Kelas"
                        priceOrDuration.trim().uppercase() == "FREE" -> "Tonton Iklan Untuk Akses"
                        else -> "Beli Kelas Sekarang"
                    }
                    Text(
                        text = buttonText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LessonCard(index: Int, lesson: Lesson, isAccessible: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
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
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = if (isAccessible) KelaskuuPurple else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "$index. ${lesson.title}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CourseDetailUnpurchasedPreview() {
    CourseDetailScreen(
        courseId = "1",
        courseTitle = "3D Design",
        rating = 4.6,
        priceOrDuration = "FREE",
        icon = Icons.Default.ViewInAr,
        isPurchased = false,
        onBackClick = {},
        onHomeClick = {},
        onKelaskuClick = {},
        onKelasClick = {},
        onProfileClick = {},
        onBuySuccess = {}
    )
}
