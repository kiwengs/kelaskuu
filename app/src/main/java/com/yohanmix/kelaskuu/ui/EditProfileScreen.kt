package com.yohanmix.kelaskuu.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    onUpdatePasswordClick: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance("gs://kelaskuu.firebasestorage.app")
    val context = LocalContext.current
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()

    var nama by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var namaBank by remember { mutableStateOf("") }
    var nomorRekening by remember { mutableStateOf("") }
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            email = currentUser.email ?: ""
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        nama = document.getString("nama") ?: ""
                        profileImageUrl = document.getString("profileImage")
                        userRole = document.getString("role") ?: ""
                        namaBank = document.getString("namaBank") ?: ""
                        nomorRekening = document.getString("nomorRekening") ?: ""
                    }
                }
        }
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // Decoding preview manual agar pratinjau lancar
    val previewBitmap = remember(imageUri) {
        if (imageUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri!!)
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) { null }
        } else null
    }

    fun handleSaveChanges() {
        if (currentUser == null) return
        if (nama.isEmpty()) {
            Toast.makeText(context, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        scope.launch {
            try {
                var finalImageUrl = profileImageUrl
                
                // 1. Upload to Storage if new image selected
                if (imageUri != null) {
                    val ref = storage.reference.child("profiles/${currentUser.uid}.jpg")
                    ref.putFile(imageUri!!).await()
                    finalImageUrl = ref.downloadUrl.await().toString()
                }

                // 2. Update Firestore
                val updates = hashMapOf<String, Any>(
                    "nama" to nama
                )
                finalImageUrl?.let { updates["profileImage"] = it }
                
                if (userRole.lowercase() == "pengajar") {
                    updates["namaBank"] = namaBank
                    updates["nomorRekening"] = nomorRekening
                }

                db.collection("users").document(currentUser.uid).update(updates).await()
                
                isLoading = false
                Toast.makeText(context, "Profil diperbarui!", Toast.LENGTH_SHORT).show()
                onSaveSuccess()
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(140.dp).background(
                    color = KelaskuuPurple,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
            ) {
                Text(
                    text = "Kembali",
                    color = Color.White,
                    modifier = Modifier.padding(top = 40.dp, start = 24.dp).clickable { onBackClick() },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).size(100.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(KelaskuuPurple),
                    contentAlignment = Alignment.Center
                ) {
                    // Tampilkan ikon fallback dulu
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize(0.7f))

                    // Tindih dengan gambar jika ada
                    if (previewBitmap != null) {
                        AsyncImage(model = previewBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else if (!profileImageUrl.isNullOrEmpty()) {
                        // Jika URL adalah Base64 (lama) atau URL Storage (baru)
                        if (profileImageUrl!!.startsWith("data:image")) {
                            val existingBitmap = remember(profileImageUrl) {
                                try {
                                    val base64String = profileImageUrl!!.substringAfter("base64,")
                                    val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                } catch (e: Exception) { null }
                            }
                            if (existingBitmap != null) {
                                AsyncImage(model = existingBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        } else {
                            AsyncImage(model = profileImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                }
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(Color(0xFF1A5A96)).clickable { launcher.launch("image/*") }.padding(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Edit Profile", 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Name Field
            Text(text = "Nama", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(
                    fontWeight = FontWeight.SemiBold, 
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = KelaskuuPurple,
                    focusedBorderColor = KelaskuuPurple,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Email Field (Read-only)
            Text(text = "Alamat Email", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = email,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = KelaskuuPurple) },
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(
                    fontWeight = FontWeight.SemiBold, 
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = KelaskuuPurple.copy(alpha = 0.5f),
                    focusedBorderColor = KelaskuuPurple,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )

            if (userRole.lowercase() == "pengajar") {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(text = "Nama Bank / E-Wallet", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = namaBank,
                    onValueChange = { namaBank = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Contoh: BCA, OVO, Dana") },
                    textStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = KelaskuuPurple,
                        focusedBorderColor = KelaskuuPurple
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "No Rekening / E-Wallet", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = nomorRekening,
                    onValueChange = { nomorRekening = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Masukkan nomor rekening atau HP") },
                    textStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = KelaskuuPurple,
                        focusedBorderColor = KelaskuuPurple
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Ganti Password?", color = KelaskuuPurple, modifier = Modifier.align(Alignment.End).clickable { onUpdatePasswordClick() }, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = KelaskuuPurple)
            } else {
                Button(
                    onClick = { handleSaveChanges() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Simpan Perubahan", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
