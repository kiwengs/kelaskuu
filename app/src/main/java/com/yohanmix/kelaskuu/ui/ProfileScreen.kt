package com.yohanmix.kelaskuu.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.google.firebase.firestore.Query

@Composable
fun ProfileScreen(
    isDarkMode: Boolean,
    userRole: String = "pelajar",
    onThemeToggle: () -> Unit,
    onHomeClick: () -> Unit,
    onKelaskuClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onKelasClick: () -> Unit = {},
    onCreateCourseClick: () -> Unit = {},
    onInstructorRegisterClick: () -> Unit = {},
    onVerifyInstructorClick: () -> Unit = {},
    onVerifyPaymentClick: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val auth = remember { if (isPreview) null else FirebaseAuth.getInstance() }
    val db = remember { if (isPreview) null else FirebaseFirestore.getInstance() }
    
    var userName by remember { mutableStateOf("User") }
    var userEmail by remember { mutableStateOf("user@example.com") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    
    var registrationStatus by remember { mutableStateOf<String?>(null) }
    var adminComment by remember { mutableStateOf("") }
    var showStatusDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Logic decoding Base64 secara manual
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
        if (currentUser != null && db != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userName = document.getString("nama") ?: "User"
                        userEmail = document.getString("email") ?: currentUser.email ?: "No Email"
                        profileImageUrl = document.getString("profileImage")
                    }
                }
            
            // Cek status pendaftaran pengajar
            db.collection("instructor_requests")
                .whereEqualTo("userId", currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && !snapshot.isEmpty) {
                        val latestDoc = snapshot.documents.maxByOrNull { 
                            it.getTimestamp("timestamp")?.toDate()?.time ?: 0L 
                        }
                        registrationStatus = latestDoc?.getString("status")
                        adminComment = latestDoc?.getString("adminComment") ?: ""
                    } else {
                        registrationStatus = null
                    }
                }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "Profile",
                userRole = userRole,
                onHomeClick = onHomeClick,
                onKelaskuClick = onKelaskuClick,
                onProfileClick = {},
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
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Profile Image with Edit Icon
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(KelaskuuPurple),
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
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onEditProfileClick() },
                    shape = CircleShape,
                    color = KelaskuuPurple,
                    tonalElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = userEmail,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Badge Role
            if (userRole.lowercase() != "pelajar") {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = KelaskuuPurple.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = userRole.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = KelaskuuPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Settings Menu Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (userRole.lowercase() in listOf("admin", "owner")) {
                    SettingsItem(
                        icon = Icons.Outlined.VerifiedUser,
                        title = "Verifikasi Pengajar",
                        onClick = onVerifyInstructorClick
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Payments,
                        title = "Verifikasi Pembayaran",
                        onClick = onVerifyPaymentClick
                    )
                }

                if (userRole.lowercase() == "pelajar") {
                    when (registrationStatus) {
                        "pending" -> {
                            SettingsItem(
                                icon = Icons.Outlined.HourglassEmpty,
                                title = "Cek Verifikasi (Pending)",
                                onClick = { showStatusDialog = true }
                            )
                        }
                        "rejected" -> {
                            SettingsItem(
                                icon = Icons.Outlined.ErrorOutline,
                                title = "Verifikasi Ditolak (Daftar Lagi)",
                                onClick = { showStatusDialog = true }
                            )
                        }
                        else -> {
                            SettingsItem(
                                icon = Icons.Outlined.School,
                                title = "Daftar Menjadi Pengajar",
                                onClick = onInstructorRegisterClick
                            )
                        }
                    }
                }

                SettingsItem(
                    icon = if (isDarkMode) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    title = if (isDarkMode) "Mode Gelap" else "Mode Terang",
                    isSwitch = true,
                    switchChecked = isDarkMode,
                    onSwitchToggle = { onThemeToggle() }
                )
                SettingsItem(
                    icon = Icons.Outlined.Feedback,
                    title = "Feedback",
                    onClick = onFeedbackClick
                )
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Log Out",
                    titleColor = Color.Red,
                    onClick = onLogoutClick // Centralized logout logic
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showStatusDialog) {
            AlertDialog(
                onDismissRequest = { showStatusDialog = false },
                title = { 
                    Text(
                        text = if (registrationStatus == "pending") "Pendaftaran Sedang Diproses" else "Pendaftaran Ditolak",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    Column {
                        Text(
                            text = if (registrationStatus == "pending") 
                                "Admin sedang meninjau pendaftaranmu. Harap bersabar." 
                                else "Mohon maaf, pendaftaranmu ditolak oleh admin."
                        )
                        if (registrationStatus == "rejected" && adminComment.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Alasan: $adminComment", color = Color.Red, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                confirmButton = {
                    if (registrationStatus == "rejected") {
                        Button(
                            onClick = { 
                                showStatusDialog = false
                                onInstructorRegisterClick() 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple)
                        ) {
                            Text("Daftar Lagi")
                        }
                    } else {
                        Button(
                            onClick = { showStatusDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple)
                        ) {
                            Text("OK")
                        }
                    }
                },
                dismissButton = {
                    if (registrationStatus == "rejected") {
                        TextButton(onClick = { showStatusDialog = false }) {
                            Text("Tutup", color = Color.Gray)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    isSwitch: Boolean = false,
    switchChecked: Boolean = false,
    onSwitchToggle: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = if (!isSwitch) onClick else ({}),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KelaskuuPurple.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (titleColor == Color.Red) Color.Red else KelaskuuPurple
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
            }
            if (isSwitch) {
                Switch(
                    checked = switchChecked,
                    onCheckedChange = onSwitchToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = KelaskuuPurple
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: String,
    userRole: String = "pelajar",
    onHomeClick: () -> Unit,
    onKelaskuClick: () -> Unit,
    onProfileClick: () -> Unit,
    onKelasClick: () -> Unit = {},
    onCreateCourseClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = KelaskuuPurple,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            selected = currentScreen == "Home",
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = KelaskuuPurple,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.PlayCircle, contentDescription = "Kelasku") },
            label = { Text("Kelasku", fontSize = 10.sp) },
            selected = currentScreen == "Kelasku",
            onClick = onKelaskuClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = KelaskuuPurple,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )

        if (userRole.lowercase() in listOf("pengajar", "admin", "owner")) {
            NavigationBarItem(
                icon = {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(KelaskuuPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Course",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = { Text("", fontSize = 10.sp) },
                selected = false,
                onClick = onCreateCourseClick,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }

        NavigationBarItem(
            icon = { Icon(Icons.Outlined.MenuBook, contentDescription = "Kelas") },
            label = { Text("Kelas", fontSize = 10.sp) },
            selected = currentScreen == "Kelas",
            onClick = onKelasClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = KelaskuuPurple,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 10.sp) },
            selected = currentScreen == "Profile",
            onClick = onProfileClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = KelaskuuPurple,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(isDarkMode = false, onThemeToggle = {}, onHomeClick = {}, onKelaskuClick = {}, onEditProfileClick = {}, onFeedbackClick = {}, onLogoutClick = {})
}
