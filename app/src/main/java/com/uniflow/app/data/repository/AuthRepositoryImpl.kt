package com.uniflow.app.data.repository

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.Club
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.model.EventTicket
import com.uniflow.app.domain.model.University
import com.uniflow.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.uniflow.app.core.common.toTurkishErrorMessage
import javax.inject.Inject

import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val sharedPreferences: SharedPreferences,
    @ApplicationContext private val context: android.content.Context
) : AuthRepository {

    companion object {
        private const val KEY_REMEMBER_ME = "key_remember_me"
        private const val KEY_SELECTED_UNI_ID = "key_selected_uni_id"
        private const val KEY_SELECTED_UNI_NAME = "key_selected_uni_name"
        private const val KEY_SELECTED_UNI_CITY = "key_selected_uni_city"
        const val ERROR_USER_NOT_FOUND = "USER_NOT_FOUND"
    }

    override fun isUserLoggedIn(): Boolean {
        val rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
        val currentUser = auth.currentUser
        if (currentUser != null && !rememberMe) {
            auth.signOut()
            return false
        }
        return currentUser != null
    }

    override fun login(email: String, password: String, rememberMe: Boolean): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        auth.signInWithEmailAndPassword(email.trim(), password.trim())
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    auth.signOut()
                    trySend(Resource.Error("Kullanıcı kimliği alınamadı."))
                    close()
                    return@addOnSuccessListener
                }


                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            sharedPreferences.edit().putBoolean(KEY_REMEMBER_ME, rememberMe).apply()
                            trySend(Resource.Success(Unit))
                        } else {
                            // User document deleted from Firestore Database!
                            sharedPreferences.edit().clear().apply()
                            auth.signOut()
                            trySend(Resource.Error("Hesap bulunamadı, lütfen kayıt olun."))
                        }
                        close()
                    }
                    .addOnFailureListener { exception ->
                        sharedPreferences.edit().clear().apply()
                        auth.signOut()
                        trySend(Resource.Error("Hesap bulunamadı, lütfen kayıt olun."))
                        close()
                    }
            }
            .addOnFailureListener { exception ->
                trySend(Resource.Error(exception.toTurkishErrorMessage("Hesap bulunamadı veya şifre hatalı.")))
                close()
            }
        awaitClose {}
    }

    override fun register(email: String, password: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    val userMap = mapOf(
                        "email" to email.trim(),
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            auth.signOut()
                            trySend(Resource.Success(Unit))
                            close()
                        }
                        .addOnFailureListener { err ->
                            auth.signOut()
                            trySend(Resource.Error(err.toTurkishErrorMessage("Kullanıcı kaydı oluşturulamadı.")))
                            close()
                        }
                } else {
                    auth.signOut()
                    trySend(Resource.Success(Unit))
                    close()
                }
            }
            .addOnFailureListener { exception ->
                trySend(Resource.Error(exception.toTurkishErrorMessage("Kayıt oluşturulamadı.")))
                close()
            }
        awaitClose {}
    }

    override fun logout() {
        sharedPreferences.edit().clear().apply()
        auth.signOut()
    }

    override fun getSelectedUniversity(): Flow<Resource<University?>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error(ERROR_USER_NOT_FOUND))
            close()
            return@callbackFlow
        }


        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val uniId = doc.getString("selectedUniversityId")
                    val uniName = doc.getString("selectedUniversityName")
                    val uniCity = doc.getString("selectedUniversityCity") ?: ""
                    if (!uniId.isNullOrBlank() && !uniName.isNullOrBlank()) {
                        val uni = University(id = uniId, name = uniName, city = uniCity)
                        sharedPreferences.edit()
                            .putString(KEY_SELECTED_UNI_ID, uniId)
                            .putString(KEY_SELECTED_UNI_NAME, uniName)
                            .putString(KEY_SELECTED_UNI_CITY, uniCity)
                            .apply()
                        trySend(Resource.Success(uni))
                    } else {
                        clearCachedUniversity()
                        trySend(Resource.Success(null))
                    }
                } else {

                    logout()
                    trySend(Resource.Error(ERROR_USER_NOT_FOUND))
                }
                close()
            }
            .addOnFailureListener {

                val cachedId = sharedPreferences.getString(KEY_SELECTED_UNI_ID, null)
                val cachedName = sharedPreferences.getString(KEY_SELECTED_UNI_NAME, null)
                val cachedCity = sharedPreferences.getString(KEY_SELECTED_UNI_CITY, "") ?: ""
                if (!cachedId.isNullOrBlank() && !cachedName.isNullOrBlank()) {
                    trySend(Resource.Success(University(id = cachedId, name = cachedName, city = cachedCity)))
                } else {
                    trySend(Resource.Success(null))
                }
                close()
            }
        awaitClose {}
    }

    private fun clearCachedUniversity() {
        sharedPreferences.edit()
            .remove(KEY_SELECTED_UNI_ID)
            .remove(KEY_SELECTED_UNI_NAME)
            .remove(KEY_SELECTED_UNI_CITY)
            .apply()
    }

    override fun saveSelectedUniversity(university: University): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Kullanıcı oturumu bulunamadı."))
            close()
            return@callbackFlow
        }

        val updateData = mapOf(
            "selectedUniversityId" to university.id,
            "selectedUniversityName" to university.name,
            "selectedUniversityCity" to university.city
        )

        firestore.collection("users").document(uid).set(updateData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                sharedPreferences.edit()
                    .putString(KEY_SELECTED_UNI_ID, university.id)
                    .putString(KEY_SELECTED_UNI_NAME, university.name)
                    .putString(KEY_SELECTED_UNI_CITY, university.city)
                    .apply()
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { exception ->
                trySend(Resource.Error(exception.toTurkishErrorMessage("Üniversite kaydedilemedi.")))
                close()
            }
        awaitClose {}
    }

    override fun getUserProfile(): Flow<Resource<com.uniflow.app.domain.model.UserProfile?>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error(ERROR_USER_NOT_FOUND))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    trySend(Resource.Error(error.toTurkishErrorMessage("Profil bilgileri alınamadı.")))
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    val email = doc.getString("email") ?: auth.currentUser?.email ?: ""
                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val studentNo = doc.getString("studentNo") ?: ""
                    val faculty = doc.getString("faculty") ?: ""
                    val department = doc.getString("department") ?: ""
                    val grade = doc.getString("grade") ?: "1. Sınıf"
                    val campus = doc.getString("campus") ?: "Ana Kampüs"
                    val profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    val isProfileComplete = doc.getBoolean("isProfileComplete")
                        ?: (studentNo.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank())
                    val isAdmin = doc.getBoolean("isAdmin") ?: email.lowercase().contains("admin")

                    val userProfile = com.uniflow.app.domain.model.UserProfile(
                        uid = uid,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        faculty = faculty,
                        department = department,
                        grade = grade,
                        studentNo = studentNo,
                        campus = campus,
                        profileImageUrl = profileImageUrl,
                        isProfileComplete = isProfileComplete,
                        isAdmin = isAdmin
                    )
                    trySend(Resource.Success(userProfile))
                } else {
                    trySend(Resource.Error(ERROR_USER_NOT_FOUND))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun updateEditableProfile(
        firstName: String,
        lastName: String,
        profileImageUrl: String?
    ): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Kullanıcı oturumu bulunamadı."))
            close()
            return@callbackFlow
        }

        val updateData = mutableMapOf<String, Any>(
            "firstName" to firstName.trim(),
            "lastName" to lastName.trim()
        )

        fun saveToFirestore(finalPhotoUrl: String?) {
            if (finalPhotoUrl != null) {
                updateData["profileImageUrl"] = finalPhotoUrl
            }

            firestore.collection("users").document(uid)
                .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    trySend(Resource.Success(Unit))
                    close()
                }
                .addOnFailureListener { exception ->
                    trySend(Resource.Error(exception.toTurkishErrorMessage("Profil bilgileri güncellenemedi.")))
                    close()
                }
        }

        if (profileImageUrl != null && (profileImageUrl.startsWith("content://") || profileImageUrl.startsWith("file://"))) {
            val uri = android.net.Uri.parse(profileImageUrl)
            val base64Image = compressAndConvertToBase64(context, uri)
            saveToFirestore(base64Image ?: profileImageUrl)
        } else {
            saveToFirestore(profileImageUrl)
        }

        awaitClose {}
    }

    private fun compressAndConvertToBase64(context: android.content.Context, uri: android.net.Uri): String? {
        return try {
            val inputStream: java.io.InputStream? = when (uri.scheme) {
                "file" -> {
                    val path = uri.path ?: return null
                    java.io.File(path).inputStream()
                }
                "content" -> context.contentResolver.openInputStream(uri)
                else -> context.contentResolver.openInputStream(uri)
            }
            if (inputStream == null) return null
            val originalBitmap = inputStream.use { android.graphics.BitmapFactory.decodeStream(it) } ?: return null

            val maxDimension = 250
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = maxDimension.toFloat() / Math.max(width, height)
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64Str"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    override fun updateUserEmail(newEmail: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(Resource.Error("Kullanıcı oturumu bulunamadı."))
            close()
            return@callbackFlow
        }

        val trimmedEmail = newEmail.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            trySend(Resource.Error("Lütfen geçerli bir e-posta adresi giriniz."))
            close()
            return@callbackFlow
        }

        currentUser.updateEmail(trimmedEmail)
            .addOnSuccessListener {
                firestore.collection("users").document(currentUser.uid)
                    .update("email", trimmedEmail)
                    .addOnSuccessListener {
                        trySend(Resource.Success(Unit))
                        close()
                    }
                    .addOnFailureListener {
                        trySend(Resource.Success(Unit))
                        close()
                    }
            }
            .addOnFailureListener { exception ->
                trySend(Resource.Error(exception.toTurkishErrorMessage("E-posta adresi güncellenemedi. Yeniden giriş yapmanız gerekebilir.")))
                close()
            }
        awaitClose {}
    }

    override fun updateUserPassword(newPassword: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(Resource.Error("Kullanıcı oturumu bulunamadı."))
            close()
            return@callbackFlow
        }

        val trimmedPassword = newPassword.trim()
        if (trimmedPassword.length < 6) {
            trySend(Resource.Error("Şifre en az 6 karakter olmalıdır."))
            close()
            return@callbackFlow
        }

        currentUser.updatePassword(trimmedPassword)
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { exception ->
                trySend(Resource.Error(exception.toTurkishErrorMessage("Şifre güncellenemedi. Yeniden giriş yapmanız gerekebilir.")))
                close()
            }
        awaitClose {}
    }

    override fun sendPasswordResetEmail(email: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            trySend(Resource.Error("Lütfen e-posta adresinizi giriniz."))
            close()
            return@callbackFlow
        }
        auth.sendPasswordResetEmail(trimmedEmail)
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { exception ->
                trySend(Resource.Error(exception.toTurkishErrorMessage("Şifre sıfırlama e-postası gönderilemedi.")))
                close()
            }
        awaitClose {}
    }

    override fun saveUserProfile(
        firstName: String,
        lastName: String,
        studentNo: String,
        faculty: String,
        department: String,
        grade: String
    ): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Kullanıcı oturumu bulunamadı."))
            close()
            return@callbackFlow
        }

        val trimmedFirstName = firstName.trim()
        val trimmedLastName = lastName.trim()
        val trimmedStudentNo = studentNo.trim()
        val trimmedFaculty = faculty.trim()
        val trimmedDepartment = department.trim()
        val trimmedGrade = grade.trim()

        // Check if studentNo is unique in Firestore users collection
        firestore.collection("users")
            .whereEqualTo("studentNo", trimmedStudentNo)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val otherUserDoc = querySnapshot.documents.firstOrNull { it.id != uid }
                if (otherUserDoc != null) {
                    trySend(Resource.Error("Bu öğrenci numarası ($trimmedStudentNo) başka bir kullanıcı tarafından kullanılıyor."))
                    close()
                    return@addOnSuccessListener
                }

                val profileData = mapOf(
                    "firstName" to trimmedFirstName,
                    "lastName" to trimmedLastName,
                    "studentNo" to trimmedStudentNo,
                    "faculty" to trimmedFaculty,
                    "department" to trimmedDepartment,
                    "grade" to trimmedGrade,
                    "isProfileComplete" to true
                )

                firestore.collection("users").document(uid)
                    .set(profileData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        trySend(Resource.Success(Unit))
                        close()
                    }
                    .addOnFailureListener { err ->
                        trySend(Resource.Error(err.toTurkishErrorMessage("Profil bilgileri kaydedilemedi.")))
                        close()
                    }
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Öğrenci numarası kontrol edilirken hata oluştu.")))
                close()
            }

        awaitClose {}
    }

    override fun submitClubApplication(
        clubName: String,
        clubCategory: String,
        clubDescription: String,
        documentFileName: String,
        documentUri: String
    ): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Kullanıcı oturumu bulunamadı."))
            close()
            return@callbackFlow
        }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val firstName = doc.getString("firstName") ?: ""
                val lastName = doc.getString("lastName") ?: ""
                val applicantName = if (firstName.isNotBlank() || lastName.isNotBlank()) "$firstName $lastName".trim() else "Öğrenci"
                val email = doc.getString("email") ?: auth.currentUser?.email ?: ""
                val studentNo = doc.getString("studentNo") ?: ""
                val uniId = doc.getString("selectedUniversityId") ?: sharedPreferences.getString("key_selected_uni_id", "") ?: ""
                val uniName = doc.getString("selectedUniversityName") ?: sharedPreferences.getString("key_selected_uni_name", "") ?: ""

                val applicationDoc = firestore.collection("club_applications").document()
                val applicationMap = mapOf(
                    "id" to applicationDoc.id,
                    "applicantUid" to uid,
                    "applicantName" to applicantName,
                    "applicantEmail" to email,
                    "studentNo" to studentNo,
                    "universityId" to uniId,
                    "universityName" to uniName,
                    "clubName" to clubName.trim(),
                    "clubCategory" to clubCategory.trim(),
                    "clubDescription" to clubDescription.trim(),
                    "documentFileName" to documentFileName.trim(),
                    "documentUri" to documentUri.trim(),
                    "status" to "PENDING",
                    "createdAt" to System.currentTimeMillis()
                )

                applicationDoc.set(applicationMap)
                    .addOnSuccessListener {
                        trySend(Resource.Success(Unit))
                        close()
                    }
                    .addOnFailureListener { err ->
                        trySend(Resource.Error(err.toTurkishErrorMessage("Başvuru gönderilemedi.")))
                        close()
                    }
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Kullanıcı bilgileri doğrulanamadı.")))
                close()
            }
        awaitClose {}
    }

    override fun getClubApplications(): Flow<Resource<List<com.uniflow.app.domain.model.ClubApplication>>> = callbackFlow {
        trySend(Resource.Loading())
        firestore.collection("club_applications")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        com.uniflow.app.domain.model.ClubApplication(
                            id = doc.id,
                            applicantUid = doc.getString("applicantUid") ?: "",
                            applicantName = doc.getString("applicantName") ?: "",
                            applicantEmail = doc.getString("applicantEmail") ?: "",
                            studentNo = doc.getString("studentNo") ?: "",
                            universityId = doc.getString("universityId") ?: "",
                            universityName = doc.getString("universityName") ?: "",
                            clubName = doc.getString("clubName") ?: "",
                            clubCategory = doc.getString("clubCategory") ?: "AKADEMİK",
                            clubDescription = doc.getString("clubDescription") ?: "",
                            documentFileName = doc.getString("documentFileName") ?: "",
                            documentUri = doc.getString("documentUri") ?: "",
                            status = doc.getString("status") ?: "PENDING",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.createdAt }
                trySend(Resource.Success(list))
                close()
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Başvurular yüklenemedi.")))
                close()
            }
        awaitClose {}
    }

    override fun getUserClubApplications(): Flow<Resource<List<com.uniflow.app.domain.model.ClubApplication>>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Success(emptyList()))
            close()
            return@callbackFlow
        }

        firestore.collection("club_applications")
            .whereEqualTo("applicantUid", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        com.uniflow.app.domain.model.ClubApplication(
                            id = doc.id,
                            applicantUid = doc.getString("applicantUid") ?: "",
                            applicantName = doc.getString("applicantName") ?: "",
                            applicantEmail = doc.getString("applicantEmail") ?: "",
                            studentNo = doc.getString("studentNo") ?: "",
                            universityId = doc.getString("universityId") ?: "",
                            universityName = doc.getString("universityName") ?: "",
                            clubName = doc.getString("clubName") ?: "",
                            clubCategory = doc.getString("clubCategory") ?: "AKADEMİK",
                            clubDescription = doc.getString("clubDescription") ?: "",
                            documentFileName = doc.getString("documentFileName") ?: "",
                            documentUri = doc.getString("documentUri") ?: "",
                            status = doc.getString("status") ?: "PENDING",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.createdAt }
                trySend(Resource.Success(list))
                close()
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Başvurularınız yüklenemedi.")))
                close()
            }
        awaitClose {}
    }

    override fun updateApplicationStatus(
        applicationId: String,
        status: String,
        application: com.uniflow.app.domain.model.ClubApplication
    ): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        firestore.collection("club_applications").document(applicationId)
            .update("status", status)
            .addOnSuccessListener {
                if (status == "APPROVED" && application.universityId.isNotBlank()) {
                    // Automatically add new approved club to top-level clubs collection
                    val clubDocId = if (application.id.isNotBlank()) application.id else firestore.collection("clubs").document().id
                    val newClubDoc = firestore.collection("clubs").document(clubDocId)

                    val clubMap = mapOf(
                        "id" to newClubDoc.id,
                        "universityId" to application.universityId,
                        "name" to application.clubName,
                        "category" to application.clubCategory,
                        "description" to application.clubDescription,
                        "leaderUid" to application.applicantUid,
                        "memberCount" to 1,
                        "eventCount" to 0,
                        "isJoined" to false,
                        "logoUrl" to "",
                        "instagramUrl" to ""
                    )
                    newClubDoc.set(clubMap)
                }
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Başvuru durumu güncellenemedi.")))
                close()
            }
        awaitClose {}
    }

    override fun joinClub(club: Club): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Oturum açmanız gerekmektedir."))
            close()
            return@callbackFlow
        }

        val clubRef = firestore.collection("users").document(uid).collection("joined_clubs").document(club.id)
        val clubData = mapOf(
            "id" to club.id,
            "universityId" to club.universityId,
            "name" to club.name,
            "category" to club.category,
            "description" to club.description,
            "logoUrl" to club.logoUrl,
            "leaderUid" to club.leaderUid,
            "memberCount" to club.memberCount + 1,
            "eventCount" to club.eventCount,
            "isJoined" to true,
            "joinedAt" to System.currentTimeMillis()
        )

        clubRef.set(clubData).addOnSuccessListener {
            if (club.id.isNotBlank()) {
                firestore.collection("clubs").document(club.id)
                    .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1))
            }
            trySend(Resource.Success(Unit))
            close()
        }.addOnFailureListener { err ->
            trySend(Resource.Error(err.toTurkishErrorMessage("Kulübe katılım başarısız.")))
            close()
        }
        awaitClose {}
    }

    override fun leaveClub(clubId: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Oturum açmanız gerekmektedir."))
            close()
            return@callbackFlow
        }

        firestore.collection("users").document(uid).collection("joined_clubs").document(clubId)
            .delete()
            .addOnSuccessListener {
                if (clubId.isNotBlank()) {
                    firestore.collection("clubs").document(clubId)
                        .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-1))
                }
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Kulüpten ayrılınamadı.")))
                close()
            }
        awaitClose {}
    }

    override fun getUserJoinedClubs(): Flow<Resource<List<Club>>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Success(emptyList()))
            close()
            return@callbackFlow
        }

        var joinedClubsDocs = emptyList<Club>()
        var membershipClubIds = emptyList<String>()

        fun enrichAndEmit(clubsList: List<Club>) {
            enrichClubsWithCounts(clubsList) { enriched ->
                trySend(Resource.Success(enriched))
            }
        }

        fun emitCombined() {
            val map = LinkedHashMap<String, Club>()
            joinedClubsDocs.forEach { club ->
                if (club.id.isNotBlank() && (club.leaderUid.isBlank() || club.leaderUid != uid)) {
                    map[club.id] = club.copy(isJoined = true)
                }
            }

            val missingIds = membershipClubIds.filter { !map.containsKey(it) }
            if (missingIds.isEmpty()) {
                enrichAndEmit(map.values.toList())
                return
            }

            var pending = missingIds.size
            missingIds.forEach { clubId ->
                firestore.collection("clubs").document(clubId).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val c = doc.toObject(Club::class.java)?.copy(id = doc.id, isJoined = true)
                            if (c != null && (c.leaderUid.isBlank() || c.leaderUid != uid)) {
                                map[c.id] = c
                            }
                        }
                        pending--
                        if (pending <= 0) {
                            enrichAndEmit(map.values.toList())
                        }
                    }
                    .addOnFailureListener {
                        pending--
                        if (pending <= 0) {
                            enrichAndEmit(map.values.toList())
                        }
                    }
            }
        }

        val joinedListener = firestore.collection("users").document(uid).collection("joined_clubs")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    joinedClubsDocs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Club::class.java)?.copy(id = doc.id, isJoined = true)
                    }
                }
                emitCombined()
            }

        val membershipsListener = firestore.collection("users").document(uid).collection("club_memberships")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    membershipClubIds = snapshot.documents.mapNotNull { doc ->
                        doc.id.ifBlank { doc.getString("clubId") }
                    }
                }
                emitCombined()
            }

        awaitClose {
            joinedListener.remove()
            membershipsListener.remove()
        }
    }

    override fun getUserManagedClubs(): Flow<Resource<List<Club>>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("clubs")
            .whereEqualTo("leaderUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val managedClubs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Club::class.java)?.copy(id = doc.id, isJoined = true)
                    }
                    enrichClubsWithCounts(managedClubs) { enriched ->
                        trySend(Resource.Success(enriched))
                    }
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }

        awaitClose { listener.remove() }
    }

    private fun enrichClubsWithCounts(clubs: List<Club>, onComplete: (List<Club>) -> Unit) {
        if (clubs.isEmpty()) {
            onComplete(emptyList())
            return
        }
        val resultList = clubs.toMutableList()
        var pending = clubs.size

        clubs.forEachIndexed { index, club ->
            firestore.collection("events").whereEqualTo("clubId", club.id).get()
                .addOnSuccessListener { evSnap ->
                    firestore.collection("clubs").document(club.id).collection("members").get()
                        .addOnSuccessListener { memSnap ->
                            val liveEvents = maxOf(club.eventCount, evSnap.size())
                            val liveMembers = maxOf(club.memberCount, memSnap.size())
                            resultList[index] = club.copy(
                                eventCount = liveEvents,
                                memberCount = liveMembers
                            )
                            pending--
                            if (pending <= 0) onComplete(resultList.toList())
                        }
                        .addOnFailureListener {
                            val liveEvents = maxOf(club.eventCount, evSnap.size())
                            resultList[index] = club.copy(eventCount = liveEvents)
                            pending--
                            if (pending <= 0) onComplete(resultList.toList())
                        }
                }
                .addOnFailureListener {
                    pending--
                    if (pending <= 0) onComplete(resultList.toList())
                }
        }
    }

    override fun getUserTickets(): Flow<Resource<List<EventTicket>>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid).collection("tickets")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val tickets = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(EventTicket::class.java)?.copy(id = doc.id)
                    }
                    trySend(Resource.Success(tickets))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getUserBookmarkedEvents(): Flow<Resource<List<Event>>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid).collection("bookmarks")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val events = snapshot.toObjects(Event::class.java).map { it.copy(isBookmarked = true) }
                    trySend(Resource.Success(events))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun toggleBookmarkEvent(event: Event): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Kullanıcı oturumu bulunamadı."))
            close()
            return@callbackFlow
        }

        val eventId = event.id.ifBlank { "${event.clubId}_${event.title.hashCode()}" }
        val bookmarkRef = firestore.collection("users").document(uid).collection("bookmarks").document(eventId)

        bookmarkRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                bookmarkRef.delete()
                    .addOnSuccessListener {
                        trySend(Resource.Success(Unit))
                        close()
                    }
                    .addOnFailureListener { err ->
                        trySend(Resource.Error(err.toTurkishErrorMessage("Kaydedilenlerden çıkarılamadı.")))
                        close()
                    }
            } else {
                val bookmarkedEvent = event.copy(id = eventId, isBookmarked = true)
                bookmarkRef.set(bookmarkedEvent)
                    .addOnSuccessListener {
                        trySend(Resource.Success(Unit))
                        close()
                    }
                    .addOnFailureListener { err ->
                        trySend(Resource.Error(err.toTurkishErrorMessage("Etkinlik kaydedilemedi.")))
                        close()
                    }
            }
        }.addOnFailureListener { err ->
            trySend(Resource.Error(err.toTurkishErrorMessage("İşlem sırasında hata oluştu.")))
            close()
        }

        awaitClose {}
    }

    override fun getUserPastEvents(): Flow<Resource<List<EventTicket>>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid).collection("past_tickets")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val tickets = snapshot.toObjects(EventTicket::class.java)
                    trySend(Resource.Success(tickets))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }
}
