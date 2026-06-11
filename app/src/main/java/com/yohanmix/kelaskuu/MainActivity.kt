package com.yohanmix.kelaskuu

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.yohanmix.kelaskuu.model.Feedback
import com.yohanmix.kelaskuu.ui.*
import com.yohanmix.kelaskuu.ui.theme.KelaskuuTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen {
    Splash, Onboarding, Login, Register, ForgotPassword, Success, Home, EditProfile, Settings, 
    Feedback, MyCourses, AvailableCourses, CourseDetail, EditCourse, CreateCourse, LessonDetail, 
    InstructorRegistration, VerifyInstructor, VerifyPayment, Payment, FeedbackList, FeedbackDetail,
    Notification, Quiz
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedDarkMode = sharedPref.getBoolean("is_dark_mode", false)
        val loginPref = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(savedDarkMode) }

            LaunchedEffect(isDarkMode) {
                sharedPref.edit().putBoolean("is_dark_mode", isDarkMode).apply()
            }

            KelaskuuTheme(darkTheme = isDarkMode) {
                var currentScreen by remember { mutableStateOf(Screen.Splash) }
                val screenStack = remember { mutableStateListOf(Screen.Splash) }
                
                var selectedCourse by remember { mutableStateOf<Course?>(null) }
                var selectedAvailableCourse by remember { mutableStateOf<AvailableCourse?>(null) }
                var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
                var selectedFeedback by remember { mutableStateOf<Feedback?>(null) }
                val quizQuestions = remember { mutableStateListOf<Question>() }
                
                val purchasedCourses = remember { mutableStateListOf<String>() } 
                var userRole by remember { mutableStateOf("pelajar") }
                
                val auth = FirebaseAuth.getInstance()
                val db = FirebaseFirestore.getInstance()
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                // --- ADMOB STATE & LOGIC ---
                var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
                var rewardedInterstitialAd by remember { mutableStateOf<RewardedInterstitialAd?>(null) }
                var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
                var isRewardedAdLoading by remember { mutableStateOf(false) }
                var transitionCount by remember { mutableIntStateOf(0) }

                fun loadRewardedAd() {
                    if (isRewardedAdLoading || rewardedAd != null) return
                    isRewardedAdLoading = true
                    val adRequest = AdRequest.Builder().build()
                    RewardedAd.load(this@MainActivity, "ca-app-pub-4785663785598276/6340081590", adRequest, object : RewardedAdLoadCallback() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            rewardedAd = null
                            isRewardedAdLoading = false
                        }
                        override fun onAdLoaded(ad: RewardedAd) {
                            rewardedAd = ad
                            isRewardedAdLoading = false
                        }
                    })
                }

                fun loadInterstitialAd() {
                    if (interstitialAd != null) return
                    val adRequest = AdRequest.Builder().build()
                    InterstitialAd.load(this@MainActivity, "ca-app-pub-4785663785598276/5836643004", adRequest, object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                        }
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            interstitialAd = null
                        }
                    })
                }

                fun loadRewardedInterstitialAd() {
                    if (rewardedInterstitialAd != null) return
                    val adRequest = AdRequest.Builder().build()
                    RewardedInterstitialAd.load(this@MainActivity, "ca-app-pub-4785663785598276/5229843195", adRequest, object : RewardedInterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                            rewardedInterstitialAd = ad
                        }
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            rewardedInterstitialAd = null
                        }
                    })
                }

                fun showRewardedInterstitial(onRewardEarned: () -> Unit) {
                    rewardedInterstitialAd?.let { ad ->
                        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                rewardedInterstitialAd = null
                                loadRewardedInterstitialAd()
                            }
                            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                rewardedInterstitialAd = null
                                loadRewardedInterstitialAd()
                            }
                        }
                        ad.show(this@MainActivity, OnUserEarnedRewardListener {
                            onRewardEarned()
                        })
                    } ?: run {
                        loadRewardedInterstitialAd()
                        onRewardEarned() // Fallback
                    }
                }

                fun showInterstitialWithLogic() {
                    transitionCount++
                    if (transitionCount >= 5) {
                        interstitialAd?.let { ad ->
                            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    interstitialAd = null
                                    transitionCount = 0
                                    loadInterstitialAd()
                                }
                                override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                                    interstitialAd = null
                                    loadInterstitialAd()
                                }
                            }
                            ad.show(this@MainActivity)
                        } ?: run {
                            loadInterstitialAd()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    loadInterstitialAd()
                    loadRewardedAd()
                    loadRewardedInterstitialAd()
                }

                fun navigateTo(screen: Screen) {
                    if (currentScreen != screen) {
                        showInterstitialWithLogic()
                        currentScreen = screen
                        screenStack.add(screen)
                    }
                }

                fun saveCourseToFirestore(courseId: String, courseTitle: String = "Kelas") {
                    val user = auth.currentUser
                    if (user != null) {
                        if (!purchasedCourses.contains(courseId)) {
                            db.collection("users").document(user.uid)
                                .update("purchasedCourses", FieldValue.arrayUnion(courseId))
                                .addOnSuccessListener {
                                    val notification = hashMapOf(
                                        "userId" to user.uid,
                                        "title" to "Kelas Baru Tersedia",
                                        "message" to "Selamat! Kelas '$courseTitle' sekarang sudah tersedia di akun Anda. Yuk mulai belajar!",
                                        "type" to "payment",
                                        "timestamp" to FieldValue.serverTimestamp(),
                                        "isRead" to false
                                    )
                                    db.collection("notifications").add(notification)
                                }
                        }
                    }
                }

                fun showRewardedAd(onRewardEarned: () -> Unit) {
                    val activityContext = this@MainActivity
                    rewardedAd?.let { ad ->
                        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                rewardedAd = null
                                loadRewardedAd()
                            }
                            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                rewardedAd = null
                                loadRewardedAd()
                            }
                        }
                        ad.show(activityContext, OnUserEarnedRewardListener {
                            onRewardEarned()
                        })
                    } ?: run {
                        Toast.makeText(context, "Iklan belum siap, mencoba memuat ulang...", Toast.LENGTH_SHORT).show()
                        loadRewardedAd()
                    }
                }

                fun simulateAdAndUnlock(courseId: String, courseTitle: String) {
                    if (userRole.lowercase() == "admin" || userRole.lowercase() == "owner") {
                        saveCourseToFirestore(courseId, courseTitle)
                        navigateTo(Screen.MyCourses)
                        return
                    }
                    
                    if (rewardedAd != null) {
                        showRewardedAd {
                            saveCourseToFirestore(courseId, courseTitle)
                            Toast.makeText(context, "Terima kasih! Kelas telah dibuka.", Toast.LENGTH_SHORT).show()
                            navigateTo(Screen.MyCourses)
                        }
                    } else {
                        scope.launch {
                            Toast.makeText(context, "Memuat iklan... Mohon tunggu sebentar", Toast.LENGTH_LONG).show()
                            loadRewardedAd()
                            var attempts = 0
                            while (rewardedAd == null && attempts < 5) {
                                delay(1000)
                                attempts++
                            }
                            if (rewardedAd != null) {
                                showRewardedAd {
                                    saveCourseToFirestore(courseId, courseTitle)
                                    Toast.makeText(context, "Terima kasih! Kelas telah dibuka.", Toast.LENGTH_SHORT).show()
                                    navigateTo(Screen.MyCourses)
                                }
                            } else {
                                saveCourseToFirestore(courseId, courseTitle)
                                Toast.makeText(context, "Iklan tidak tersedia saat ini. Kelas dibuka otomatis.", Toast.LENGTH_LONG).show()
                                navigateTo(Screen.MyCourses)
                            }
                        }
                    }
                }

                fun navigateBack() {
                    if (screenStack.size > 1) {
                        screenStack.removeAt(screenStack.size - 1)
                        currentScreen = screenStack.last()
                    } else {
                        finish()
                    }
                }

                BackHandler {
                    navigateBack()
                }

                // --- REAL-TIME LISTENERS ---
                LaunchedEffect(auth.currentUser) {
                    val user = auth.currentUser
                    if (user != null && user.email == "kdsaputro555@gmail.com") {
                        db.collection("users").document(user.uid).update("role", "owner")
                    }
                }

                DisposableEffect(auth.currentUser) {
                    val user = auth.currentUser
                    val listener = if (user != null) {
                        db.collection("users").document(user.uid)
                            .addSnapshotListener { snapshot, _ ->
                                if (snapshot != null && snapshot.exists()) {
                                    userRole = snapshot.getString("role") ?: "pelajar"
                                    val courses = snapshot.get("purchasedCourses") as? List<*>
                                    if (courses != null) {
                                        purchasedCourses.clear()
                                        purchasedCourses.addAll(courses.filterIsInstance<String>())
                                    }
                                }
                            }
                    } else {
                        userRole = "pelajar"
                        purchasedCourses.clear()
                        selectedCourse = null
                        selectedAvailableCourse = null
                        selectedLesson = null
                        null
                    }
                    onDispose { listener?.remove() }
                }

                // --- NAVIGATION ROUTING ---
                when (currentScreen) {
                    Screen.Splash -> {
                        SplashScreen(onTimeout = {
                            val rememberMe = loginPref.getBoolean("remember_me", false)
                            val currentUser = auth.currentUser
                            if (currentUser != null && rememberMe) {
                                if (currentUser.isEmailVerified) navigateTo(Screen.Home) else {
                                    auth.signOut()
                                    navigateTo(Screen.Onboarding)
                                }
                            } else {
                                auth.signOut()
                                navigateTo(Screen.Onboarding)
                            }
                        })
                    }
                    Screen.Onboarding -> OnboardingScreen(onFinish = { navigateTo(Screen.Login) })
                    Screen.Login -> LoginScreen(
                        onLoginSuccess = { navigateTo(Screen.Home) },
                        onRegisterClick = { navigateTo(Screen.Register) },
                        onForgotPasswordClick = { navigateTo(Screen.ForgotPassword) }
                    )
                    Screen.Register -> RegisterScreen(
                        onRegisterSuccess = { navigateTo(Screen.Home) },
                        onLoginClick = { navigateBack() }
                    )
                    Screen.ForgotPassword -> ForgotPasswordScreen(
                        onBackClick = { navigateBack() },
                        onUpdateClick = { navigateTo(Screen.Success) }
                    )
                    Screen.Success -> SuccessScreen(onTimeout = { navigateTo(Screen.Login) })
                    Screen.Home -> HomeScreen(
                        userRole = userRole,
                        onEditProfileClick = { navigateTo(Screen.EditProfile) },
                        onKelaskuClick = { navigateTo(Screen.MyCourses) },
                        onBottomProfileClick = { navigateTo(Screen.Settings) },
                        onKelasClick = { navigateTo(Screen.AvailableCourses) },
                        onNotificationClick = { navigateTo(Screen.Notification) },
                        onCreateCourseClick = { navigateTo(Screen.CreateCourse) },
                        onCourseClick = { course ->
                            selectedCourse = course
                            selectedAvailableCourse = null
                            navigateTo(Screen.CourseDetail)
                        }
                    )
                    Screen.Notification -> NotificationScreen(onBackClick = { navigateBack() })
                    Screen.EditProfile -> EditProfileScreen(
                        onBackClick = { navigateBack() },
                        onSaveSuccess = { navigateBack() },
                        onUpdatePasswordClick = { navigateTo(Screen.ForgotPassword) }
                    )
                    Screen.Settings -> ProfileScreen(
                        isDarkMode = isDarkMode,
                        userRole = userRole,
                        onThemeToggle = { isDarkMode = !isDarkMode },
                        onHomeClick = { navigateTo(Screen.Home) },
                        onKelaskuClick = { navigateTo(Screen.MyCourses) },
                        onEditProfileClick = { navigateTo(Screen.EditProfile) },
                        onFeedbackClick = { 
                            if (userRole.lowercase() in listOf("admin", "owner")) navigateTo(Screen.FeedbackList) else navigateTo(Screen.Feedback)
                        },
                        onCreateCourseClick = { navigateTo(Screen.CreateCourse) },
                        onLogoutClick = { 
                            loginPref.edit().remove("remember_me").apply()
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken("228072778953-01jtv4ah6tfag3f3pqj7stigc4br55mj.apps.googleusercontent.com")
                                .requestEmail().build()
                            GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                                auth.signOut()
                                screenStack.clear()
                                navigateTo(Screen.Login)
                            }
                        },
                        onKelasClick = { navigateTo(Screen.AvailableCourses) },
                        onInstructorRegisterClick = { navigateTo(Screen.InstructorRegistration) },
                        onVerifyInstructorClick = { navigateTo(Screen.VerifyInstructor) },
                        onVerifyPaymentClick = { navigateTo(Screen.VerifyPayment) }
                    )
                    Screen.InstructorRegistration -> InstructorRegistrationScreen(
                        onBackClick = { navigateBack() },
                        onRegisterSuccess = {
                            Toast.makeText(context, "Pendaftaran berhasil dikirim!", Toast.LENGTH_LONG).show()
                            navigateBack()
                        }
                    )
                    Screen.VerifyInstructor -> VerifyInstructorScreen(
                        onBackClick = { navigateBack() },
                        onRoleUpdate = { userId, newRole, namaBank, nomorRekening ->
                            db.collection("users").document(userId).update(mapOf("role" to newRole, "namaBank" to namaBank, "nomorRekening" to nomorRekening))
                                .addOnSuccessListener { Toast.makeText(context, "User berhasil diverifikasi sebagai $newRole", Toast.LENGTH_SHORT).show() }
                        }
                    )
                    Screen.VerifyPayment -> VerifyPaymentScreen(onBackClick = { navigateBack() })
                    Screen.Payment -> {
                        val courseId = selectedCourse?.id ?: selectedAvailableCourse?.id ?: ""
                        val courseTitle = selectedCourse?.title ?: selectedAvailableCourse?.title ?: ""
                        val price = if (selectedCourse != null) selectedCourse?.price ?: "" else selectedAvailableCourse?.price ?: ""
                        val instructorId = selectedCourse?.creatorId ?: selectedAvailableCourse?.creatorId ?: ""
                        PaymentScreen(courseId = courseId, courseTitle = courseTitle, price = price, instructorName = "Pengajar", instructorId = instructorId, onBackClick = { navigateBack() }, onPaymentSubmitted = { navigateBack() })
                    }
                    Screen.Feedback -> FeedbackScreen(onBackClick = { navigateBack() }, onSendClick = { navigateBack() })
                    Screen.FeedbackList -> FeedbackListScreen(onBackClick = { navigateBack() }, onFeedbackClick = { feedback -> selectedFeedback = feedback; navigateTo(Screen.FeedbackDetail) })
                    Screen.FeedbackDetail -> selectedFeedback?.let { FeedbackDetailScreen(feedback = it, onBackClick = { navigateBack() }) }
                    Screen.MyCourses -> MyCoursesScreen(
                        userRole = userRole,
                        purchasedCourseIds = purchasedCourses,
                        onHomeClick = { navigateTo(Screen.Home) },
                        onKelaskuClick = { navigateTo(Screen.MyCourses) },
                        onProfileClick = { navigateTo(Screen.Settings) },
                        onKelasClick = { navigateTo(Screen.AvailableCourses) },
                        onCreateCourseClick = { navigateTo(Screen.CreateCourse) },
                        onCourseClick = { course -> selectedCourse = course; selectedAvailableCourse = null; navigateTo(Screen.CourseDetail) }
                    )
                    Screen.AvailableCourses -> AvailableCoursesScreen(
                        userRole = userRole,
                        purchasedCourseIds = purchasedCourses,
                        onHomeClick = { navigateTo(Screen.Home) },
                        onKelaskuClick = { navigateTo(Screen.MyCourses) },
                        onProfileClick = { navigateTo(Screen.Settings) },
                        onKelasClick = { navigateTo(Screen.AvailableCourses) },
                        onCreateCourseClick = { navigateTo(Screen.CreateCourse) },
                        onCourseClick = { course ->
                            if (userRole.lowercase() in listOf("admin", "owner")) {
                                saveCourseToFirestore(course.id, course.title)
                                navigateTo(Screen.MyCourses)
                            } else {
                                selectedAvailableCourse = course
                                selectedCourse = null
                                navigateTo(Screen.CourseDetail)
                            }
                        }
                    )
                    Screen.CourseDetail -> {
                        val courseId = selectedCourse?.id ?: selectedAvailableCourse?.id
                        courseId?.let { id ->
                            CourseDetailScreen(
                                courseId = id,
                                courseTitle = selectedCourse?.title ?: selectedAvailableCourse?.title ?: "",
                                rating = selectedCourse?.rating ?: selectedAvailableCourse?.rating ?: 0.0,
                                priceOrDuration = if (selectedCourse != null) selectedCourse?.duration ?: "" else selectedAvailableCourse?.price ?: "",
                                icon = selectedCourse?.icon ?: selectedAvailableCourse?.icon ?: Icons.AutoMirrored.Filled.MenuBook,
                                isPurchased = purchasedCourses.contains(id),
                                userRole = userRole,
                                imageUrl = selectedCourse?.imageUrl ?: selectedAvailableCourse?.imageUrl,
                                onBackClick = { navigateBack() },
                                onHomeClick = { navigateTo(Screen.Home) },
                                onKelaskuClick = { navigateTo(Screen.MyCourses) },
                                onKelasClick = { navigateTo(Screen.AvailableCourses) },
                                onProfileClick = { navigateTo(Screen.Settings) },
                                onCreateCourseClick = { navigateTo(Screen.CreateCourse) },
                                onLessonDetailClick = { lesson -> selectedLesson = lesson; navigateTo(Screen.LessonDetail) },
                                onQuizClick = { questions -> quizQuestions.clear(); quizQuestions.addAll(questions); navigateTo(Screen.Quiz) },
                                onEditCourseClick = { navigateTo(Screen.EditCourse) },
                                onBuySuccess = {
                                    val priceOrDuration = if (selectedCourse != null) selectedCourse?.duration ?: "" else selectedAvailableCourse?.price ?: ""
                                    if (userRole.lowercase() in listOf("admin", "owner") || priceOrDuration == "FREE") simulateAdAndUnlock(id, selectedCourse?.title ?: selectedAvailableCourse?.title ?: "") else navigateTo(Screen.Payment)
                                }
                            )
                        }
                    }
                    Screen.EditCourse -> selectedCourse?.let { CreateCourseScreen(userRole = userRole, courseId = it.id, onBackClick = { navigateBack() }, onSuccess = { navigateBack() }) }
                    Screen.CreateCourse -> CreateCourseScreen(userRole = userRole, onBackClick = { navigateBack() }, onSuccess = { navigateBack() })
                    Screen.LessonDetail -> selectedLesson?.let { LessonDetailScreen(lessonTitle = it.title, description = it.description, videoUrl = it.videoUrl, onBackClick = { navigateBack() }) }
                    Screen.Quiz -> QuizScreen(
                        courseTitle = selectedCourse?.title ?: selectedAvailableCourse?.title ?: "",
                        questions = quizQuestions,
                        onBackClick = { navigateBack() },
                        onQuizSubmit = { score -> 
                            showRewardedInterstitial {
                                Toast.makeText(context, "Selamat! Skor Anda: $score. Bonus poin/badge didapatkan!", Toast.LENGTH_LONG).show()
                                navigateBack()
                            }
                        }
                    )
                }
            }
        }
    }
}
