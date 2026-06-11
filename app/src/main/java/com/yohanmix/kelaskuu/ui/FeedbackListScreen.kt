package com.yohanmix.kelaskuu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.yohanmix.kelaskuu.model.Feedback
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackListScreen(
    onBackClick: () -> Unit,
    onFeedbackClick: (Feedback) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val feedbacks = remember { mutableStateListOf<Feedback>() }
    var isLoading by remember { mutableStateOf(true) }
    
    val tintColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        db.collection("feedbacks")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        Feedback(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            email = doc.getString("email") ?: "",
                            masukan = doc.getString("masukan") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.toDate()
                        )
                    }
                    feedbacks.clear()
                    feedbacks.addAll(list)
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("List Feedback", fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = tintColor)
            }
        } else if (feedbacks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada feedback dari user.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(feedbacks) { feedback ->
                    FeedbackItem(
                        feedback = feedback,
                        tintColor = tintColor,
                        onClick = { onFeedbackClick(feedback) }
                    )
                }
            }
        }
    }
}

@Composable
fun FeedbackItem(
    feedback: Feedback,
    tintColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                Icon(Icons.Default.Feedback, contentDescription = null, tint = tintColor)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feedback.email,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = feedback.masukan,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                val dateStr = feedback.timestamp?.let { 
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(it)
                } ?: ""
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
