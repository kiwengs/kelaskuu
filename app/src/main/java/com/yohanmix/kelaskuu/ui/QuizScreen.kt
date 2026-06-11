package com.yohanmix.kelaskuu.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohanmix.kelaskuu.ui.theme.KelaskuuPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    courseTitle: String,
    questions: List<Question>,
    onBackClick: () -> Unit,
    onQuizSubmit: (Int) -> Unit
) {
    val context = LocalContext.current
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    val essayAnswers = remember { mutableStateMapOf<Int, String>() }
    var showResults by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ujian: $courseTitle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = KelaskuuPurple
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            if (questions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada pertanyaan untuk ujian ini.")
                }
            } else if (showResults) {
                QuizResultView(
                    score = score,
                    totalQuestions = questions.size,
                    onBackClick = onBackClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    itemsIndexed(questions) { index, question ->
                        QuestionCard(
                            index = index,
                            question = question,
                            selectedOption = userAnswers[index],
                            essayText = essayAnswers[index] ?: "",
                            onOptionSelected = { userAnswers[index] = it },
                            onEssayChanged = { essayAnswers[index] = it }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (userAnswers.size + essayAnswers.filter { it.value.isNotBlank() }.size < questions.size) {
                            Toast.makeText(context, "Harap jawab semua pertanyaan", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        var correctCount = 0
                        questions.forEachIndexed { index, question ->
                            if (question.type == QuestionType.MULTIPLE_CHOICE) {
                                if (userAnswers[index] == question.correctAnswerIndex) {
                                    correctCount++
                                }
                            } else {
                                // Simple essay check (case insensitive)
                                if (essayAnswers[index]?.trim()?.equals(question.essayAnswer.trim(), ignoreCase = true) == true) {
                                    correctCount++
                                }
                            }
                        }
                        score = (correctCount * 100) / questions.size
                        showResults = true
                        onQuizSubmit(score)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Selesaikan Ujian", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuestionCard(
    index: Int,
    question: Question,
    selectedOption: Int?,
    essayText: String,
    onOptionSelected: (Int) -> Unit,
    onEssayChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${index + 1}. ${question.text}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (question.type == QuestionType.MULTIPLE_CHOICE) {
                question.options.forEachIndexed { optIndex, option ->
                    if (option.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOptionSelected(optIndex) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedOption == optIndex,
                                onClick = { onOptionSelected(optIndex) },
                                colors = RadioButtonDefaults.colors(selectedColor = KelaskuuPurple)
                            )
                            Text(option)
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = essayText,
                    onValueChange = onEssayChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ketik jawaban Anda di sini...") },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
fun QuizResultView(score: Int, totalQuestions: Int, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ujian Selesai!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = KelaskuuPurple
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Skor Anda:",
            fontSize = 18.sp
        )
        Text(
            text = "$score / 100",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (score >= 70) Color(0xFF4CAF50) else Color.Red
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(containerColor = KelaskuuPurple)
        ) {
            Text("Kembali ke Detail Kelas")
        }
    }
}
