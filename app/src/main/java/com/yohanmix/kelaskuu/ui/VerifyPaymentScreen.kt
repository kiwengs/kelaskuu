package com.yohanmix.kelaskuu.ui

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
import androidx.compose.material.icons.filled.Payments
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

data class PaymentRequest(
    val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val price: String = "",
    val instructorName: String = "",
    val instructorId: String = "",
    val status: String = "pending",
    var userName: String = "",
    var userEmail: String = "",
    var instructorBankName: String = "",
    var instructorAccountNo: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyPaymentScreen(
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Menunggu Verifikasi", "Terverifikasi")
    
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val payments = remember { mutableStateListOf<PaymentRequest>() }
    var isLoading by remember { mutableStateOf(true) }
    val tintColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        db.collection("payments")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val userId = doc.getString("userId") ?: ""
                        PaymentRequest(
                            id = doc.id,
                            userId = userId,
                            courseId = doc.getString("courseId") ?: "",
                            courseTitle = doc.getString("courseTitle") ?: "",
                            price = doc.getString("price") ?: "",
                            instructorName = doc.getString("instructorName") ?: "",
                            instructorId = doc.getString("instructorId") ?: "",
                            status = doc.getString("status") ?: "pending"
                        )
                    }
                    
                    list.forEach { payment ->
                        // Fetch Buyer Info
                        db.collection("users").document(payment.userId).get()
                            .addOnSuccessListener { userDoc ->
                                payment.userName = userDoc.getString("nama") ?: "Unknown"
                                payment.userEmail = userDoc.getString("email") ?: "Unknown"
                                updatePaymentInList(payments, payment)
                            }
                        
                        // Fetch Instructor Bank Info
                        if (payment.instructorId.isNotEmpty()) {
                            db.collection("users").document(payment.instructorId).get()
                                .addOnSuccessListener { instDoc ->
                                    payment.instructorBankName = instDoc.getString("namaBank") ?: "-"
                                    payment.instructorAccountNo = instDoc.getString("nomorRekening") ?: "-"
                                    updatePaymentInList(payments, payment)
                                }
                        }
                    }
                    
                    payments.clear()
                    payments.addAll(list)
                    isLoading = false
                }
            }
    }

    var selectedPayment by remember { mutableStateOf<PaymentRequest?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Verifikasi Pembayaran", fontWeight = FontWeight.Bold) },
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

            if (isLoading && payments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = tintColor)
                }
            } else {
                val filteredPayments = when (selectedTab) {
                    0 -> payments.filter { it.status == "pending" }
                    else -> payments.filter { it.status != "pending" }
                }

                if (filteredPayments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada data pembayaran.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredPayments) { payment ->
                            PaymentRequestItem(
                                payment = payment,
                                isClickable = true,
                                tintColor = tintColor,
                                onClick = { selectedPayment = payment }
                            )
                        }
                    }
                }
            }
        }

        if (selectedPayment != null) {
            DetailPaymentDialog(
                payment = selectedPayment!!,
                tintColor = tintColor,
                onDismiss = { selectedPayment = null },
                onVerify = {
                    val pay = selectedPayment!!
                    db.collection("payments").document(pay.id)
                        .update("status", "verified")
                        .addOnSuccessListener {
                            db.collection("users").document(pay.userId)
                                .update("purchasedCourses", FieldValue.arrayUnion(pay.courseId))
                                .addOnSuccessListener {
                                    
                                    // Kirim Notifikasi Pembayaran Berhasil
                                    val notification = hashMapOf(
                                        "userId" to pay.userId,
                                        "title" to "Pembayaran Berhasil",
                                        "message" to "Hore! Pembayaran untuk kelas '${pay.courseTitle}' telah diverifikasi. Kelas sekarang tersedia di akun Anda.",
                                        "type" to "payment",
                                        "timestamp" to FieldValue.serverTimestamp(),
                                        "isRead" to false
                                    )
                                    db.collection("notifications").add(notification)

                                    // Opsional: Notifikasi untuk Pengajar bahwa ada yang beli kursusnya
                                    if (pay.instructorId.isNotEmpty()) {
                                        val instructorNotif = hashMapOf(
                                            "userId" to pay.instructorId,
                                            "title" to "Penjualan Baru!",
                                            "message" to "Seseorang baru saja membeli kelas '${pay.courseTitle}' Anda.",
                                            "type" to "payment",
                                            "timestamp" to FieldValue.serverTimestamp(),
                                            "isRead" to false
                                        )
                                        db.collection("notifications").add(instructorNotif)
                                    }

                                    Toast.makeText(context, "Pembayaran diverifikasi", Toast.LENGTH_SHORT).show()
                                    selectedPayment = null
                                }
                        }
                },
                onReject = {
                    val pay = selectedPayment!!
                    db.collection("payments").document(pay.id)
                        .update("status", "rejected")
                        .addOnSuccessListener {
                            
                            // Kirim Notifikasi Pembayaran Ditolak
                            val notification = hashMapOf(
                                "userId" to pay.userId,
                                "title" to "Pembayaran Ditolak",
                                "message" to "Maaf, bukti pembayaran untuk kelas '${pay.courseTitle}' ditolak oleh admin. Silakan cek kembali bukti pembayaran Anda.",
                                "type" to "payment",
                                "timestamp" to FieldValue.serverTimestamp(),
                                "isRead" to false
                            )
                            db.collection("notifications").add(notification)

                            Toast.makeText(context, "Pembayaran ditolak", Toast.LENGTH_SHORT).show()
                            selectedPayment = null
                        }
                }
            )
        }
    }
}

fun updatePaymentInList(list: MutableList<PaymentRequest>, payment: PaymentRequest) {
    val index = list.indexOfFirst { it.id == payment.id }
    if (index != -1) {
        list[index] = payment.copy()
    }
}

@Composable
fun PaymentRequestItem(
    payment: PaymentRequest, 
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
                Icon(Icons.Default.Payments, contentDescription = null, tint = tintColor)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.userName.ifEmpty { "Loading..." },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = payment.courseTitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = payment.price,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = tintColor
                )
            }

            if (payment.status != "pending") {
                val statusColor = if (payment.status == "verified") Color(0xFF4CAF50) else Color.Red
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = payment.status.uppercase(),
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
fun DetailPaymentDialog(
    payment: PaymentRequest, 
    tintColor: Color,
    onDismiss: () -> Unit, 
    onVerify: () -> Unit, 
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail Pembayaran", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailField("Nama Pembeli", payment.userName)
                DetailField("Email Pembeli", payment.userEmail)
                DetailField("Nama Kelas", payment.courseTitle)
                DetailField("Harga", payment.price)
                DetailField("Nama Pengajar", payment.instructorName)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Informasi Rekening Pengajar:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = tintColor)
                DetailField("Bank / E-Wallet", payment.instructorBankName)
                DetailField("Nomor Rekening", payment.instructorAccountNo)
            }
        },
        confirmButton = {
            if (payment.status == "pending") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) {
                        Text("Tolak")
                    }
                    Button(
                        onClick = onVerify, 
                        modifier = Modifier.weight(1f), 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
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
            if (payment.status == "pending") {
                TextButton(onClick = onDismiss) { Text("Batal", color = Color.Gray) }
            }
        }
    )
}
