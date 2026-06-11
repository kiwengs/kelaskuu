package com.yohanmix.kelaskuu.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isVerificationSent by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        Text(
            text = "Buat Akun",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = KelaskuuPurple
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Name Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Nama", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nama Anda", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Alamat Email", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Email@gmail.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = KelaskuuPurple) },
                trailingIcon = {
                    if (isVerificationSent) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Terkirim",
                            tint = Color.Green,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    } else {
                        Button(
                            onClick = {
                                if (email.isNotEmpty()) {
                                    isLoading = true
                                    // Gunakan password dummy sementara karena Firebase butuh user terdaftar untuk kirim link
                                    val tempPassword = if (password.isNotEmpty()) password else "temp123456"
                                    
                                    auth.createUserWithEmailAndPassword(email, tempPassword)
                                        .addOnSuccessListener { result ->
                                            result.user?.sendEmailVerification()
                                                ?.addOnSuccessListener {
                                                    isLoading = false
                                                    isVerificationSent = true
                                                    Toast.makeText(context, "Link verifikasi telah dikirim! Cek email Anda.", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            if (e.message?.contains("already in use") == true) {
                                                // Jika sudah ada tapi belum verif, coba login & resend
                                                auth.signInWithEmailAndPassword(email, tempPassword)
                                                    .addOnSuccessListener { res ->
                                                        res.user?.sendEmailVerification()
                                                            ?.addOnSuccessListener {
                                                                isLoading = false
                                                                isVerificationSent = true
                                                                Toast.makeText(context, "Link verifikasi dikirim ulang!", Toast.LENGTH_SHORT).show()
                                                            }
                                                    }
                                                    .addOnFailureListener {
                                                        isLoading = false
                                                        Toast.makeText(context, "Email sudah terdaftar. Masukkan password Anda lalu klik Verifikasi.", Toast.LENGTH_LONG).show()
                                                    }
                                            } else {
                                                isLoading = false
                                                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(context, "Isi email terlebih dahulu untuk verifikasi", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 4.dp).height(36.dp)
                        ) {
                            Text("Verifikasi", fontSize = 12.sp, color = Color.White)
                        }
                    }
                },
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Kata Sandi", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Password123", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = KelaskuuPurple) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ulangi Kata Sandi", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Password123", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = KelaskuuPurple) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (isLoading) {
            CircularProgressIndicator(color = KelaskuuPurple)
        } else {
            Button(
                onClick = {
                    if (nama.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(context, "Harap isi semua bidang", Toast.LENGTH_SHORT).show()
                    } else if (password != confirmPassword) {
                        Toast.makeText(context, "Kata sandi tidak cocok", Toast.LENGTH_SHORT).show()
                    } else if (!isVerificationSent) {
                        Toast.makeText(context, "Silakan klik tombol Verifikasi di samping Email!", Toast.LENGTH_LONG).show()
                    } else {
                        val currentUser = auth.currentUser
                        if (currentUser != null && currentUser.email == email) {
                            isLoading = true
                            // REFRESH data user dari server untuk cek status verifikasi terbaru
                            currentUser.reload().addOnCompleteListener { task ->
                                if (currentUser.isEmailVerified) {
                                    // Update password ke yang asli (jika tadi pas verif pake dummy)
                                    currentUser.updatePassword(password).addOnCompleteListener {
                                        // BERHASIL: Link sudah diklik, simpan ke Firestore
                                        val user = com.yohanmix.kelaskuu.model.User(currentUser.uid, nama, email)
                                        db.collection("users").document(currentUser.uid).set(user)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                Toast.makeText(context, "Pendaftaran Selesai!", Toast.LENGTH_SHORT).show()
                                                onRegisterSuccess()
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                Toast.makeText(context, "Error Database: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Mohon klik link verifikasi di email Anda terlebih dahulu!", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Sesi salah, silakan ulangi verifikasi email.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "SIGN UP", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row {
            Text(text = "Sudah Punya Akun? ", color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = "Masuk",
                color = KelaskuuPurple,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLoginClick() }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen({}, {})
}
