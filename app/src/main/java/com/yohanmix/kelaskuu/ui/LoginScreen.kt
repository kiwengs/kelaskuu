package com.yohanmix.kelaskuu.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.yohanmix.kelaskuu.R
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple
import com.yohanmix.kelaskuu.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(sharedPref.getBoolean("remember_me", false)) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Google Sign In Configuration
    // Menggunakan string resource default_web_client_id yang digenerate otomatis
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            isLoading = true
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { authResult ->
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        db.collection("users").document(firebaseUser.uid).get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    val newUser = User(
                                        uid = firebaseUser.uid,
                                        nama = firebaseUser.displayName ?: "User Google",
                                        email = firebaseUser.email ?: ""
                                    )
                                    db.collection("users").document(firebaseUser.uid).set(newUser)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onLoginSuccess()
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            onLoginSuccess()
                                        }
                                } else {
                                    isLoading = false
                                    onLoginSuccess()
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                onLoginSuccess()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Log.e("LoginScreen", "FirebaseAuth error", e)
                    Toast.makeText(context, "Firebase Auth Gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: ApiException) {
            isLoading = false
            Log.e("LoginScreen", "Google Sign-In error code: ${e.statusCode}", e)
            val msg = when(e.statusCode) {
                10 -> "Error 10: SHA-1 tidak terdaftar di Firebase Console."
                7 -> "Error 7: Tidak ada koneksi internet."
                12500 -> "Error 12500: Masalah Google Play Services."
                else -> "Error ${e.statusCode}: ${e.localizedMessage}"
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "Selamat Datang di Kelaskuu",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = KelaskuuPurple,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Masuk",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = KelaskuuPurple,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Alamat Email", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("emailanda@gmail.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = KelaskuuPurple) },
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

        Spacer(modifier = Modifier.height(12.dp))

        // Password Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Kata Sandi", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("isi password anda", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = KelaskuuPurple)
                    )
                    Text("Ingat Saya", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = "Ubah Sandi?",
                    color = KelaskuuPurple,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onForgotPasswordClick() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(color = KelaskuuPurple)
        } else {
            Button(
                onClick = {
                    if (email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(context, "Harap isi email dan password", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                val user = auth.currentUser
                                if (user != null) {
                                    if (user.isEmailVerified) {
                                        isLoading = false
                                        sharedPref.edit().putBoolean("remember_me", rememberMe).apply()
                                        onLoginSuccess()
                                    } else {
                                        isLoading = false
                                        user.sendEmailVerification()
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Email belum diverifikasi. Link verifikasi baru telah dikirim ke email Anda.", Toast.LENGTH_LONG).show()
                                            }
                                        auth.signOut()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Login Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "LOG IN", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = Color.LightGray)
                Text(
                    text = "atau",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = Color.LightGray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Login Button
            Surface(
                onClick = {
                    // Coba sign out dulu, tapi tetap lanjut login meskipun gagal/selesai
                    googleSignInClient.signOut().addOnCompleteListener {
                        try {
                            launcher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuka Google Sign-In", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_search),
                        contentDescription = "Google Icon",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign in with Google", 
                        fontWeight = FontWeight.Medium, 
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row {
            Text(text = "Belum punya akun? ", color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = "Buat Akun",
                color = KelaskuuPurple,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen({}, {}, {})
}
