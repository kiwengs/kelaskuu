package com.yohanmix.kelaskuu.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCourseScreen(
    userRole: String,
    courseId: String? = null,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit
) {
    var courseName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Design") }
    val categories = listOf("Design", "Programming", "UI/UX")
    var isFree by remember { mutableStateOf(true) }
    var price by remember { mutableStateOf("") }
    
    // Menggunakan EditableLesson agar state per item bisa dipantau dan mendukung Uri video
    val lessons = remember { mutableStateListOf<EditableLesson>() }
    val quizQuestions = remember { mutableStateListOf<EditableQuestion>() }
    var courseImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val db = remember { if (isPreview) null else FirebaseFirestore.getInstance() }
    val auth = remember { if (isPreview) null else FirebaseAuth.getInstance() }
    val storage = remember { if (isPreview) null else FirebaseStorage.getInstance("gs://kelaskuu.firebasestorage.app") }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(courseId != null) }
    var uploadStatus by remember { mutableStateOf("") }
    
    val tintColor = MaterialTheme.colorScheme.primary

    // Inisialisasi materi pertama jika buat kelas baru
    LaunchedEffect(Unit) {
        if (courseId == null && lessons.isEmpty()) {
            lessons.add(EditableLesson(title = "Pertemuan 1"))
        }
    }

    LaunchedEffect(courseId) {
        if (courseId != null && db != null) {
            uploadStatus = "Memuat data kelas..."
            db.collection("available_courses").document(courseId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        courseName = doc.getString("title") ?: ""
                        selectedCategory = doc.getString("category") ?: "Design"
                        val p = doc.getString("price") ?: "FREE"
                        isFree = p == "FREE"
                        if (!isFree) price = p.replace("Rp ", "").replace(".", "")
                        existingImageUrl = doc.getString("headerImage") ?: ""
                        
                        val lessonsList = doc.get("lessons") as? List<Map<String, Any>>
                        lessons.clear()
                        lessonsList?.forEach { l ->
                            lessons.add(EditableLesson(
                                title = l["title"] as? String ?: "",
                                description = l["description"] as? String ?: "",
                                videoUrl = l["videoUrl"] as? String ?: ""
                            ))
                        }

                        val quizData = doc.get("quiz") as? Map<String, Any>
                        val questionsList = quizData?.get("questions") as? List<Map<String, Any>>
                        quizQuestions.clear()
                        questionsList?.forEach { q ->
                            quizQuestions.add(EditableQuestion(
                                text = q["text"] as? String ?: "",
                                type = QuestionType.valueOf(q["type"] as? String ?: "MULTIPLE_CHOICE"),
                                options = (q["options"] as? List<String>) ?: listOf("", "", "", ""),
                                correctAnswerIndex = (q["correctAnswerIndex"] as? Long)?.toInt() ?: 0,
                                essayAnswer = q["essayAnswer"] as? String ?: ""
                            ))
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        courseImageUri = uri
    }

    // Picker Video Global (untuk menangani item tertentu di list)
    var selectingVideoIndex by remember { mutableIntStateOf(-1) }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && selectingVideoIndex != -1) {
            lessons[selectingVideoIndex].videoUri = uri
        }
        selectingVideoIndex = -1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (courseId == null) "Buat Kelas Baru" else "Edit Kelas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = tintColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = tintColor
                )
            )
        }
    ) { padding ->
        if (isLoading && courseId != null && uploadStatus == "Memuat data kelas...") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = tintColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Nama Kelas", fontWeight = FontWeight.Medium, color = tintColor)
                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Masukkan nama kelas") }
                    )
                }

                item {
                    Text("Tema Kelas", fontWeight = FontWeight.Medium, color = tintColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tintColor,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                item {
                    Text("Gambar Header", fontWeight = FontWeight.Medium, color = tintColor)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (courseImageUri != null) {
                            AsyncImage(
                                model = courseImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (existingImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = existingImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = tintColor)
                                Text("Klik untuk upload gambar", fontSize = 12.sp, color = tintColor)
                            }
                        }
                    }
                }

                item {
                    Text("Materi Kelas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = tintColor)
                }

                itemsIndexed(lessons) { index, lesson ->
                    LessonInputItem(
                        index = index,
                        lesson = lesson,
                        onPickVideo = {
                            selectingVideoIndex = index
                            videoPickerLauncher.launch("video/*")
                        },
                        onRemove = { if (lessons.size > 1) lessons.removeAt(index) }
                    )
                }

                item {
                    Button(
                        onClick = { lessons.add(EditableLesson(title = "Pertemuan ${lessons.size + 1}")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Pertemuan")
                    }
                }

                // Section Quiz/Ujian
                item {
                    Text("Ujian / Tes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = tintColor)
                }

                itemsIndexed(quizQuestions) { index, question ->
                    QuestionInputItem(
                        index = index,
                        question = question,
                        onRemove = { quizQuestions.removeAt(index) }
                    )
                }

                item {
                    Button(
                        onClick = { quizQuestions.add(EditableQuestion(text = "")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Pertanyaan")
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Pengaturan Harga", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = tintColor)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kelas Gratis")
                        Switch(
                            checked = isFree,
                            onCheckedChange = { isFree = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = tintColor)
                        )
                    }
                }

                if (!isFree) {
                    item {
                        Text("Harga (Rp)", fontWeight = FontWeight.Medium, color = tintColor)
                        OutlinedTextField(
                            value = price,
                            onValueChange = { if (it.all { char -> char.isDigit() }) price = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Contoh: 50000") }
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (courseName.isBlank()) {
                                Toast.makeText(context, "Nama kelas tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!isFree && price.isBlank()) {
                                Toast.makeText(context, "Harga tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isLoading = true
                            uploadStatus = "Menyiapkan data..."
                            
                            scope.launch {
                                try {
                                    val currentUser = auth?.currentUser
                                    if (currentUser == null) {
                                        Toast.makeText(context, "Sesi berakhir, silakan login ulang", Toast.LENGTH_SHORT).show()
                                        isLoading = false
                                        return@launch
                                    }

                                    // 1. Upload Gambar Header jika ada yang baru
                                    var finalImageUrl = existingImageUrl
                                    if (courseImageUri != null) {
                                        uploadStatus = "Mengunggah gambar header..."
                                        val fileName = "course_headers/${UUID.randomUUID()}.jpg"
                                        val ref = storage?.reference?.child(fileName)
                                        ref?.putFile(courseImageUri!!)?.await()
                                        finalImageUrl = ref?.downloadUrl?.await()?.toString() ?: ""
                                    }

                                    // 2. Upload Video per Materi secara berurutan
                                    val uploadedLessons = mutableListOf<Map<String, Any>>()
                                    for (lesson in lessons) {
                                        var finalVideoUrl = lesson.videoUrl
                                        if (lesson.videoUri != null) {
                                            uploadStatus = "Mengunggah video: ${lesson.title}..."
                                            val videoFileName = "course_videos/${System.currentTimeMillis()}_${UUID.randomUUID()}.mp4"
                                            val videoRef = storage?.reference?.child(videoFileName)
                                            videoRef?.putFile(lesson.videoUri!!)?.await()
                                            finalVideoUrl = videoRef?.downloadUrl?.await()?.toString() ?: ""
                                        }
                                        uploadedLessons.add(mapOf(
                                            "title" to lesson.title,
                                            "description" to lesson.description,
                                            "videoUrl" to finalVideoUrl
                                        ))
                                    }

                                    // 3. Simpan ke Firestore
                                    val courseData = mutableMapOf(
                                        "title" to courseName,
                                        "category" to selectedCategory,
                                        "price" to if (isFree) "FREE" else "Rp ${NumberFormat.getInstance(Locale("id", "ID")).format(price.toLongOrNull() ?: 0)}",
                                        "headerImage" to finalImageUrl,
                                        "instructorId" to currentUser.uid,
                                        "instructorName" to (currentUser.displayName ?: "Instructor"),
                                        "lessons" to uploadedLessons,
                                        "quiz" to mapOf(
                                            "questions" to quizQuestions.map { mapOf(
                                                "text" to it.text,
                                                "type" to it.type.name,
                                                "options" to it.options,
                                                "correctAnswerIndex" to it.correctAnswerIndex,
                                                "essayAnswer" to it.essayAnswer
                                            )}
                                        ),
                                        "lastUpdated" to FieldValue.serverTimestamp()
                                    )

                                    if (courseId == null) {
                                        courseData["enrolledStudents"] = 0
                                        courseData["rating"] = 4.5
                                        db?.collection("available_courses")?.add(courseData)?.await()
                                        Toast.makeText(context, "Kelas berhasil dibuat", Toast.LENGTH_SHORT).show()
                                    } else {
                                        db?.collection("available_courses")?.document(courseId)?.update(courseData as Map<String, Any>)?.await()
                                        Toast.makeText(context, "Kelas berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                    }
                                    
                                    onSuccess()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = tintColor),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(uploadStatus)
                            }
                        } else {
                            Text(if (courseId == null) "Terbitkan Kelas" else "Simpan Perubahan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonInputItem(
    index: Int,
    lesson: EditableLesson,
    onPickVideo: () -> Unit,
    onRemove: () -> Unit
) {
    val tintColor = MaterialTheme.colorScheme.primary
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pertemuan ${index + 1}", fontWeight = FontWeight.Bold, color = tintColor)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                }
            }
            
            OutlinedTextField(
                value = lesson.title,
                onValueChange = { lesson.title = it },
                label = { Text("Judul Materi") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Opsi Upload Video
            Text("Video Materi", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = tintColor)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onPickVideo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tintColor.copy(alpha = 0.1f),
                        contentColor = tintColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lesson.videoUri != null) "Ganti Video" else "Pilih File Video")
                }
                
                if (lesson.videoUri != null) {
                    IconButton(onClick = { lesson.videoUri = null }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red)
                    }
                }
            }
            
            if (lesson.videoUri != null) {
                Text(
                    text = "Video terpilih: ${lesson.videoUri?.lastPathSegment?.takeLast(20)}",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (lesson.videoUrl.isNotEmpty()) {
                Text(
                    text = "Video sudah tersedia di cloud",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = lesson.videoUrl,
                onValueChange = { lesson.videoUrl = it },
                label = { Text("Atau URL Video (Youtube/Drive)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = lesson.videoUri == null,
                placeholder = { Text("Masukkan URL jika tidak upload file") }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = lesson.description,
                onValueChange = { lesson.description = it },
                label = { Text("Deskripsi Singkat") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                minLines = 2
            )
        }
    }
}

@Composable
fun QuestionInputItem(
    index: Int,
    question: EditableQuestion,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pertanyaan ${index + 1}", fontWeight = FontWeight.Bold)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                }
            }

            OutlinedTextField(
                value = question.text,
                onValueChange = { question.text = it },
                label = { Text("Teks Pertanyaan") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = question.type == QuestionType.MULTIPLE_CHOICE,
                    onClick = { question.type = QuestionType.MULTIPLE_CHOICE }
                )
                Text("Pilihan Ganda")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = question.type == QuestionType.ESSAY,
                    onClick = { question.type = QuestionType.ESSAY }
                )
                Text("Esai")
            }

            if (question.type == QuestionType.MULTIPLE_CHOICE) {
                question.options.forEachIndexed { optIndex, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = question.correctAnswerIndex == optIndex,
                            onClick = { question.correctAnswerIndex = optIndex }
                        )
                        OutlinedTextField(
                            value = option,
                            onValueChange = {
                                val newList = question.options.toMutableList()
                                newList[optIndex] = it
                                question.options = newList
                            },
                            label = { Text("Opsi ${optIndex + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = question.essayAnswer,
                    onValueChange = { question.essayAnswer = it },
                    label = { Text("Kunci Jawaban Esai (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

class EditableLesson(
    title: String = "",
    description: String = "",
    videoUrl: String = "",
    videoUri: Uri? = null
) {
    var title by mutableStateOf(title)
    var description by mutableStateOf(description)
    var videoUrl by mutableStateOf(videoUrl)
    var videoUri by mutableStateOf(videoUri)
}

class EditableQuestion(
    text: String = "",
    type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    options: List<String> = listOf("", "", "", ""),
    correctAnswerIndex: Int = 0,
    essayAnswer: String = ""
) {
    var text by mutableStateOf(text)
    var type by mutableStateOf(type)
    var options by mutableStateOf(options)
    var correctAnswerIndex by mutableStateOf(correctAnswerIndex)
    var essayAnswer by mutableStateOf(essayAnswer)
}
