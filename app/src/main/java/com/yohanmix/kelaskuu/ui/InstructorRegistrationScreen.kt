package com.yohanmix.kelaskuu.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorRegistrationScreen(
    onBackClick: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val isPreview = LocalInspectionMode.current
    var fullName by remember { mutableStateOf(if (isPreview) "John Doe (Preview)" else "Memuat nama...") }
    var expertise by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    val experienceOptions = listOf(
        "Belum ada pengalaman",
        "Pengalaman 1-2 tahun",
        "Pengalaman 3-5 tahun",
        "Pengalaman di atas 5 tahun"
    )
    var experience by remember { mutableStateOf(experienceOptions[0]) }
    var expandedExperience by remember { mutableStateOf(false) }
    
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var selectedCertificateUri by remember { mutableStateOf<Uri?>(null) }
    var isAgreed by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = remember { if (isPreview) null else FirebaseAuth.getInstance() }
    val db = remember { if (isPreview) null else FirebaseFirestore.getInstance() }
    val storage = remember { if (isPreview) null else FirebaseStorage.getInstance("gs://kelaskuu.firebasestorage.app") }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedCertificateUri = uri
    }

    LaunchedEffect(Unit) {
        if (isPreview) return@LaunchedEffect
        val currentUser = auth?.currentUser
        if (currentUser != null && db != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        fullName = document.getString("nama") ?: "Tanpa Nama"
                    } else {
                        fullName = "Profil tidak ditemukan"
                    }
                }
                .addOnFailureListener {
                    fullName = "Gagal memuat nama"
                }
        } else {
            fullName = "Silakan login"
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Daftar Pengajar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Kembali",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = primaryColor,
                    navigationIconContentColor = primaryColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Lengkapi data dirimu untuk menjadi bagian dari pengajar Kelaskuu",
                fontSize = 14.sp,
                color = onSurfaceVariantColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = onSurfaceColor,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = onSurfaceVariantColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = expertise,
                onValueChange = { expertise = it },
                label = { Text("Keahlian (Contoh: UI/UX, Kotlin, etc)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Alamat") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expandedExperience && !isLoading,
                onExpandedChange = { if(!isLoading) expandedExperience = !expandedExperience }
            ) {
                OutlinedTextField(
                    value = experience,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pengalaman Mengajar / Kerja") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExperience) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedExperience,
                    onDismissRequest = { expandedExperience = false }
                ) {
                    experienceOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                experience = option
                                expandedExperience = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Dokumen Pendukung", fontWeight = FontWeight.Bold, color = primaryColor)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                onClick = { if(!isLoading) filePickerLauncher.launch("application/pdf,image/*") },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CloudUpload, null, tint = primaryColor, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedCertificateUri == null) "Upload Sertifikat (PDF/Image)" else "File Terpilih",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryColor
                    )
                    if (selectedCertificateUri != null) {
                        Text(
                            text = selectedCertificateUri?.lastPathSegment ?: "File dipilih",
                            fontSize = 12.sp,
                            color = onSurfaceVariantColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Informasi Pembayaran (Wajib)", fontWeight = FontWeight.Bold, color = primaryColor)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Nama Bank / E-Wallet") },
                placeholder = { Text("Contoh: BCA / Mandiri / GoPay") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { if (it.all { char -> char.isDigit() }) accountNumber = it },
                label = { Text("Nomor Rekening / HP") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Baris Persetujuan yang bisa diklik seluruhnya
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isLoading) { isAgreed = !isAgreed }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isAgreed,
                    onCheckedChange = { isAgreed = it },
                    enabled = !isLoading,
                    colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                )
                Text(text = "Saya setuju dengan ", fontSize = 14.sp, color = onSurfaceColor)
                Text(
                    text = "Syarat dan Ketentuan",
                    fontSize = 14.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showTerms = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    if (fullName.isNotEmpty() && fullName != "Memuat nama..." && expertise.isNotEmpty() && bankName.isNotEmpty() && accountNumber.isNotEmpty() && isAgreed && selectedCertificateUri != null) {
                        isLoading = true
                        if (isPreview) {
                            isLoading = false
                            onRegisterSuccess()
                            return@Button
                        }
                        
                        val currentUser = auth?.currentUser
                        if (currentUser != null && db != null && storage != null) {
                            val fileRef = storage.reference.child("certificates/${UUID.randomUUID()}")
                            fileRef.putFile(selectedCertificateUri!!)
                                .addOnSuccessListener {
                                    fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                        val request = hashMapOf(
                                            "userId" to currentUser.uid,
                                            "name" to fullName,
                                            "expertise" to expertise,
                                            "address" to address,
                                            "experience" to experience,
                                            "email" to (currentUser.email ?: ""),
                                            "status" to "pending",
                                            "namaBank" to bankName,
                                            "nomorRekening" to accountNumber,
                                            "certificateUrl" to downloadUrl.toString(),
                                            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                        )
                                        
                                        db.collection("instructor_requests").add(request)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                onRegisterSuccess()
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Gagal upload: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isAgreed && fullName.isNotEmpty() && fullName != "Memuat nama..." && expertise.isNotEmpty() && bankName.isNotEmpty() && accountNumber.isNotEmpty() && !isLoading && selectedCertificateUri != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Kirim Pendaftaran", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showTerms) {
            AlertDialog(
                onDismissRequest = { showTerms = false },
                title = { Text("Syarat & Ketentuan Pengajar") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("1. Pengajar wajib memberikan materi orisinal.\n2. Materi harus sesuai standar Kelaskuu.\n3. Interaksi profesional.\n4. Pembagian hasil diatur kemudian.\n5. Kelaskuu berhak menonaktifkan akun jika melanggar hukum.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTerms = false }) { Text("Tutup", color = primaryColor) }
                }
            )
        }
    }
}
