package com.yohanmix.kelaskuu.ui

import androidx.compose.ui.graphics.vector.ImageVector

data class Course(
    val id: String,
    val title: String,
    val rating: Double,
    val duration: String,
    val icon: ImageVector,
    val price: String = "",
    val category: String = "Semua",
    val creatorId: String? = null,
    val imageUrl: String? = null
)

data class Lesson(
    var title: String = "",
    var description: String = "",
    var videoUrl: String = ""
)

data class Quiz(
    val questions: List<Question> = emptyList()
)

data class Question(
    var id: String = "",
    var text: String = "",
    var type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    var options: List<String> = listOf("", "", "", ""),
    var correctAnswerIndex: Int = 0,
    var essayAnswer: String = "" // For reference or grading criteria if needed
)

enum class QuestionType {
    MULTIPLE_CHOICE,
    ESSAY
}
