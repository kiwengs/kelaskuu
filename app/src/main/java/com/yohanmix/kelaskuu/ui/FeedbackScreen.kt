package com.yohanmix.kelaskuu.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

@Composable
fun FeedbackScreen(
    onBackClick: () -> Unit,
    onSendClick: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    
    var feedbackText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val maxChars = 5000

    fun handleSendFeedback() {
        if (feedbackText.isBlank()) {
            Toast.makeText(context, "Mohon isi masukan Anda", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        val currentUser = auth.currentUser
        
        val feedbackData = hashMapOf(
            "userId" to (currentUser?.uid ?: "anonymous"),
            "email" to (currentUser?.email ?: "anonymous"),
            "masukan" to feedbackText, // Menggunakan nama "masukan" agar mudah dibaca di Firebase
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("feedbacks")
            .add(feedbackData)
            .addOnSuccessListener {
                isLoading = false
                Toast.makeText(context, "Terima kasih! Masukan Anda telah terkirim.", Toast.LENGTH_SHORT).show()
                onSendClick() // Menjalankan navigasi setelah berhasil
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Gagal mengirim: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Kembali Button
        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(40.dp),
            enabled = !isLoading
        ) {
            Text(text = "Kembali", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title text
        Text(
            text = "Masukan anda akan sangat berarti bagi kita",
            fontSize = 18.sp,
            color = KelaskuuPurple,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Feedback Input
        OutlinedTextField(
            value = feedbackText,
            onValueChange = {
                if (it.length <= maxChars) {
                    feedbackText = it
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            placeholder = { Text("Tulis masukan Anda di sini...", color = Color.Gray) }, // Tambah petunjuk agar user tidak bingung
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading,
            textStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = KelaskuuPurple,
                focusedBorderColor = KelaskuuPurple,
                cursorColor = KelaskuuPurple,
                unfocusedTextColor = Color.Black,
                focusedTextColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Character Counter
        Text(
            text = "${feedbackText.length}/$maxChars",
            modifier = Modifier.align(Alignment.End),
            fontSize = 14.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        // Kirim Button
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = KelaskuuPurple
            )
        } else {
            Button(
                onClick = { handleSendFeedback() },
                colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Kirim",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun FeedbackScreenPreview() {
    FeedbackScreen(onBackClick = {}, onSendClick = {})
}
