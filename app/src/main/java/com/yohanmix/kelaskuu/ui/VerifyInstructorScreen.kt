package com.yohanmix.kelaskuu.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple

data class InstructorRequest(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val expertise: String = "",
    val address: String = "",
    val experience: String = "",
    val namaBank: String = "",
    val nomorRekening: String = "",
    val certificateUrl: String = "",
    var status: String = "pending", // "pending", "verified", "rejected"
    val adminComment: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyInstructorScreen(
    onBackClick: () -> Unit,
    onRoleUpdate: (String, String, String, String) -> Unit // Callback updated: (userId, newRole, namaBank, nomorRekening)
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Baru Mendaftar", "Sudah Dicek")
    
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val requests = remember { mutableStateListOf<InstructorRequest>() }
    var isLoading by remember { mutableStateOf(true) }
    val tintColor = MaterialTheme.colorScheme.primary

    // Mengambil data dari Firestore secara realtime
    LaunchedEffect(Unit) {
        db.collection("instructor_requests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        InstructorRequest(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            name = doc.getString("name") ?: "",
                            email = doc.getString("email") ?: "",
                            expertise = doc.getString("expertise") ?: "",
                            address = doc.getString("address") ?: "",
                            experience = doc.getString("experience") ?: "",
                            namaBank = doc.getString("namaBank") ?: "",
                            nomorRekening = doc.getString("nomorRekening") ?: "",
                            certificateUrl = doc.getString("certificateUrl") ?: "",
                            status = doc.getString("status") ?: "pending",
                            adminComment = doc.getString("adminComment") ?: ""
                        )
                    }
                    requests.clear()
                    requests.addAll(list)
                    isLoading = false
                }
            }
    }

    var selectedRequest by remember { mutableStateOf<InstructorRequest?>(null) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Verifikasi Pengajar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = tintColor,
                    navigationIconContentColor = tintColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = tintColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = tintColor
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = tintColor)
                }
            } else {
                val filteredRequests = when (selectedTab) {
                    0 -> requests.filter { it.status == "pending" }
                    else -> requests.filter { it.status != "pending" }
                }

                if (filteredRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada data pendaftaran.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredRequests) { request ->
                            InstructorRequestItem(
                                request = request,
                                isClickable = true,
                                tintColor = tintColor,
                                onClick = { selectedRequest = request }
                            )
                        }
                    }
                }
            }
        }

        // Dialog Detail
        if (selectedRequest != null) {
            DetailRequestDialog(
                request = selectedRequest!!,
                tintColor = tintColor,
                onDismiss = { selectedRequest = null },
                onVerify = {
                    val requestId = selectedRequest!!.id
                    val userId = selectedRequest!!.userId
                    val bank = selectedRequest!!.namaBank
                    val norek = selectedRequest!!.nomorRekening
                    
                    db.collection("instructor_requests").document(requestId)
                        .update("status", "verified", "adminComment", "")
                        .addOnSuccessListener {
                            // Update role user di koleksi 'users' beserta data bank
                            onRoleUpdate(userId, "pengajar", bank, norek)
                            
                            // Kirim Notifikasi
                            val notification = hashMapOf(
                                "userId" to userId,
                                "title" to "Pendaftaran Pengajar Disetujui",
                                "message" to "Selamat! Pendaftaran Anda sebagai pengajar telah disetujui. Sekarang Anda dapat membuat kelas baru.",
                                "type" to "registration",
                                "timestamp" to FieldValue.serverTimestamp(),
                                "isRead" to false
                            )
                            db.collection("notifications").add(notification)

                            Toast.makeText(context, "Pendaftaran disetujui", Toast.LENGTH_SHORT).show()
                            selectedRequest = null
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                onReject = {
                    showRejectDialog = true
                }
            )
        }

        if (showRejectDialog) {
            AlertDialog(
                onDismissRequest = { showRejectDialog = false },
                title = { Text("Alasan Penolakan") },
                text = {
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        placeholder = { Text("Contoh: Sertifikat tidak valid") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val requestId = selectedRequest!!.id
                            val userId = selectedRequest!!.userId
                            db.collection("instructor_requests").document(requestId)
                                .update("status", "rejected", "adminComment", rejectionReason)
                                .addOnSuccessListener {
                                    // Kirim Notifikasi Penolakan
                                    val notification = hashMapOf(
                                        "userId" to userId,
                                        "title" to "Pendaftaran Pengajar Ditolak",
                                        "message" to "Mohon maaf, pendaftaran Anda sebagai pengajar ditolak. Alasan: $rejectionReason",
                                        "type" to "registration",
                                        "timestamp" to FieldValue.serverTimestamp(),
                                        "isRead" to false
                                    )
                                    db.collection("notifications").add(notification)

                                    Toast.makeText(context, "Pendaftaran ditolak", Toast.LENGTH_SHORT).show()
                                    showRejectDialog = false
                                    selectedRequest = null
                                    rejectionReason = ""
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Tolak")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRejectDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun InstructorRequestItem(
    request: InstructorRequest,
    isClickable: Boolean,
    tintColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isClickable) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tintColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = tintColor)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = request.email,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (request.status != "pending") {
                val statusColor = if (request.status == "verified") Color(0xFF4CAF50) else Color.Red
                val statusText = if (request.status == "verified") "Verified" else "Rejected"
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRequestDialog(
    request: InstructorRequest,
    tintColor: Color,
    onDismiss: () -> Unit,
    onVerify: () -> Unit,
    onReject: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail Pendaftaran", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailField(label = "Nama Lengkap", value = request.name)
                DetailField(label = "Email", value = request.email)
                DetailField(label = "Keahlian", value = request.expertise)
                DetailField(label = "Alamat", value = request.address)
                DetailField(label = "Pengalaman", value = request.experience)
                DetailField(label = "Nama Bank / E-Wallet", value = request.namaBank)
                DetailField(label = "Nomor Rekening", value = request.nomorRekening)
                
                if (request.status == "rejected" && request.adminComment.isNotEmpty()) {
                    DetailField(label = "Alasan Penolakan", value = request.adminComment)
                }

                if (request.certificateUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(request.certificateUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = tintColor.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = tintColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lihat Sertifikat", color = tintColor)
                    }
                }
            }
        },
        confirmButton = {
            if (request.status == "pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tolak")
                    }
                    Button(
                        onClick = onVerify,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terima")
                    }
                }
            } else {
                Button(
                    onClick = onDismiss, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Tutup")
                }
            }
        },
        dismissButton = {
            if (request.status == "pending") {
                TextButton(onClick = onDismiss) {
                    Text("Batal", color = Color.Gray)
                }
            }
        }
    )
}

@Composable
fun DetailField(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = if (value.isEmpty()) "-" else value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}
