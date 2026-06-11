package com.yohanmix.kelaskuu.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Kembali", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Ubah password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = KelaskuuPurple
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Email Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Masukan Email", color = KelaskuuPurple, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("email@gmail.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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

        Spacer(modifier = Modifier.height(48.dp))

        if (isLoading) {
            CircularProgressIndicator(color = KelaskuuPurple)
        } else {
            Button(
                onClick = {
                    if (email.isNotEmpty()) {
                        isLoading = true
                        auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Link verifikasi telah dikirim ke email anda", Toast.LENGTH_LONG).show()
                                onUpdateClick()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Gagal mengirim: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Harap isi email terlebih dahulu", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Ubah", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    ForgotPasswordScreen({}, {})
}
