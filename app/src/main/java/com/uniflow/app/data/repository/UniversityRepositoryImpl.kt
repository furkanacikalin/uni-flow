package com.uniflow.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.model.Club
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.model.EventTicket
import com.uniflow.app.domain.model.University
import com.uniflow.app.domain.model.UserProfile
import com.uniflow.app.domain.repository.UniversityRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

class UniversityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : UniversityRepository {

    override fun getUniversities(): Flow<Resource<List<University>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("universities")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.toTurkishErrorMessage("Üniversiteler alınamadı.")))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val trCollator = Collator.getInstance(Locale("tr", "TR"))
                    val list = snapshot.toObjects(University::class.java)
                        .sortedWith { u1, u2 -> trCollator.compare(u1.name, u2.name) }
                    trySend(Resource.Success(list))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getClubsByUniversity(universityId: String): Flow<Resource<List<Club>>> = callbackFlow {
        trySend(Resource.Loading())

        val clubsListener = firestore.collection("clubs")
            .whereEqualTo("universityId", universityId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.toTurkishErrorMessage("Kulüpler alınamadı.")))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val rawList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Club::class.java)?.copy(id = doc.id)
                    }
                    if (rawList.isEmpty()) {
                        trySend(Resource.Success(emptyList()))
                        return@addSnapshotListener
                    }

                    var pending = rawList.size
                    val updated = rawList.toMutableList()
                    rawList.forEachIndexed { index, club ->
                        firestore.collection("events").whereEqualTo("clubId", club.id).get()
                            .addOnSuccessListener { evSnap ->
                                firestore.collection("clubs").document(club.id).collection("members").get()
                                    .addOnSuccessListener { memSnap ->
                                        val eCount = maxOf(club.eventCount, evSnap.size())
                                        val mCount = maxOf(club.memberCount, memSnap.size())
                                        updated[index] = club.copy(eventCount = eCount, memberCount = mCount)
                                        pending--
                                        if (pending <= 0) trySend(Resource.Success(updated.toList()))
                                    }
                                    .addOnFailureListener {
                                        val eCount = maxOf(club.eventCount, evSnap.size())
                                        updated[index] = club.copy(eventCount = eCount)
                                        pending--
                                        if (pending <= 0) trySend(Resource.Success(updated.toList()))
                                    }
                            }
                            .addOnFailureListener {
                                pending--
                                if (pending <= 0) trySend(Resource.Success(updated.toList()))
                            }
                    }
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }

        awaitClose {
            clubsListener.remove()
        }
    }

    override fun getEventsByUniversity(universityId: String): Flow<Resource<List<Event>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("events")
            .whereEqualTo("universityId", universityId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.toTurkishErrorMessage("Etkinlikler alınamadı.")))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Event::class.java)?.copy(id = doc.id)
                    }
                    if (list.isEmpty()) {
                        trySend(Resource.Success(emptyList()))
                    } else {
                        var pendingCount = list.size
                        val updatedList = list.toMutableList()

                        list.forEachIndexed { index, ev ->
                            firestore.collection("events").document(ev.id)
                                .collection("attendees").get()
                                .addOnSuccessListener { attSnap ->
                                    val realCount = maxOf(ev.attendeeCount, attSnap.size())
                                    updatedList[index] = ev.copy(attendeeCount = realCount)
                                    pendingCount--
                                    if (pendingCount <= 0) {
                                        trySend(Resource.Success(updatedList.toList()))
                                    }
                                }
                                .addOnFailureListener {
                                    pendingCount--
                                    if (pendingCount <= 0) {
                                        trySend(Resource.Success(updatedList.toList()))
                                    }
                                }
                        }
                    }
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getClubDetails(clubId: String): Flow<Resource<Club>> = callbackFlow {
        trySend(Resource.Loading())
        val clubsRef = firestore.collection("clubs").document(clubId)
        val membersRef = clubsRef.collection("members")

        var currentClub: Club? = null
        var subcollectionCount: Int? = null

        fun emitCombined() {
            val club = currentClub ?: return
            val realCount = maxOf(club.memberCount, subcollectionCount ?: 0)
            trySend(Resource.Success(club.copy(memberCount = realCount)))
        }

        val docListener = clubsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.toTurkishErrorMessage("Kulüp bulunamadı.")))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val c = snapshot.toObject(Club::class.java)?.copy(id = snapshot.id)
                if (c != null) {
                    currentClub = c
                    emitCombined()
                } else {
                    trySend(Resource.Error("Kulüp bulunamadı."))
                }
            } else {
                trySend(Resource.Error("Kulüp bulunamadı."))
            }
        }

        val membersListener = membersRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                subcollectionCount = snapshot.size()
                emitCombined()
            }
        }

        awaitClose {
            docListener.remove()
            membersListener.remove()
        }
    }

    override fun updateClubDetails(
        clubId: String,
        name: String,
        category: String,
        description: String,
        socialLinks: Map<String, String>
    ): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val instagramUrl = socialLinks["Instagram"] ?: ""
        val updateMap = mapOf(
            "name" to name.trim(),
            "category" to category.trim(),
            "description" to description.trim(),
            "instagramUrl" to instagramUrl.trim(),
            "socialLinks" to socialLinks,
            "clubName" to name.trim(),
            "clubCategory" to category.trim(),
            "clubDescription" to description.trim()
        )

        firestore.collection("clubs").document(clubId)
            .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                firestore.collection("club_applications").document(clubId)
                    .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Kulüp bilgileri güncellenemedi.")))
                close()
            }
        awaitClose {}
    }

    override fun getEventsByClub(clubId: String): Flow<Resource<List<Event>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("events")
            .whereEqualTo("clubId", clubId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.toTurkishErrorMessage("Etkinlikler alınamadı.")))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Event::class.java)?.copy(id = doc.id)
                    }
                    trySend(Resource.Success(list))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getEventById(eventId: String): Flow<Resource<Event>> = callbackFlow {
        trySend(Resource.Loading())
        val docRef = firestore.collection("events").document(eventId)
        val attendeesRef = docRef.collection("attendees")

        var currentEvent: Event? = null
        var subcollectionCount: Int? = null

        fun emitCombined() {
            val ev = currentEvent ?: return
            val realCount = maxOf(ev.attendeeCount, subcollectionCount ?: 0)
            trySend(Resource.Success(ev.copy(attendeeCount = realCount)))
        }

        val docListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.toTurkishErrorMessage("Etkinlik bilgisi alınamadı.")))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                currentEvent = snapshot.toObject(Event::class.java)?.copy(id = snapshot.id)
                emitCombined()
            } else {
                trySend(Resource.Error("Etkinlik bulunamadı."))
            }
        }

        val attendeesListener = attendeesRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                subcollectionCount = snapshot.size()
                emitCombined()
            }
        }

        awaitClose {
            docListener.remove()
            attendeesListener.remove()
        }
    }

    override fun joinEvent(event: Event): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        if (event.isPast()) {
            trySend(Resource.Error("Tarihi geçmiş etkinliklere katılım sağlanamaz."))
            close()
            return@callbackFlow
        }
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Lutfen giris yapin."))
            close()
            return@callbackFlow
        }

        val ticket = EventTicket(
            id = event.id,
            eventTitle = event.title,
            category = event.category,
            dateMonth = event.dateMonth.ifBlank { "MAY" },
            dateDay = event.dateDay.ifBlank { "17" },
            time = event.timeText,
            location = event.location,
            deskNo = "A-" + (10..99).random(),
            isQrReady = true
        )

        firestore.collection("users").document(uid)
            .collection("tickets").document(event.id)
            .set(ticket)
            .addOnSuccessListener {
                firestore.collection("events").document(event.id)
                    .collection("attendees").document(uid)
                    .set(mapOf("uid" to uid, "joinedAt" to FieldValue.serverTimestamp()))
                firestore.collection("events").document(event.id)
                    .set(mapOf("attendeeCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Bilet olusturulamadi.")))
                close()
            }
        awaitClose {}
    }

    override fun isUserJoinedEvent(eventId: String): Flow<Boolean> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(uid)
            .collection("tickets").document(eventId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot != null && snapshot.exists())
            }
        awaitClose { listener.remove() }
    }

    override fun createEvent(event: Event): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val docRef = if (event.id.isNotBlank()) firestore.collection("events").document(event.id)
                     else firestore.collection("events").document()
        val finalEvent = event.copy(id = docRef.id)

        docRef.set(finalEvent)
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Etkinlik kaydedilemedi.")))
                close()
            }
        awaitClose {}
    }

    override fun updateEvent(event: Event): Flow<Resource<Unit>> = createEvent(event)

    override fun deleteEvent(eventId: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        firestore.collection("events").document(eventId).delete()
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Etkinlik silinemedi.")))
                close()
            }
        awaitClose {}
    }

    override fun getEventAttendees(eventId: String): Flow<Resource<List<UserProfile>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("events").document(eventId)
            .collection("attendees")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.toTurkishErrorMessage("Katılımcılar alınamadı.")))
                    return@addSnapshotListener
                }
                val uids = snapshot?.documents?.mapNotNull { doc ->
                    val u = doc.id.ifBlank { doc.getString("uid") }
                    if (u.isNullOrBlank()) null else u
                } ?: emptyList()

                if (uids.isEmpty()) {
                    trySend(Resource.Success(emptyList()))
                    return@addSnapshotListener
                }

                firestore.collection("users")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), uids.take(30))
                    .get()
                    .addOnSuccessListener { userSnaps ->
                        val profiles = userSnaps.documents.map { doc ->
                            UserProfile(
                                uid = doc.id,
                                firstName = doc.getString("firstName") ?: "",
                                lastName = doc.getString("lastName") ?: "",
                                email = doc.getString("email") ?: "",
                                faculty = doc.getString("faculty") ?: "",
                                department = doc.getString("department") ?: "",
                                grade = doc.getString("grade") ?: "",
                                profileImageUrl = doc.getString("profileImageUrl") ?: ""
                            )
                        }
                        trySend(Resource.Success(profiles))
                    }
                    .addOnFailureListener {
                        val stubProfiles = uids.map { uid -> UserProfile(uid = uid, firstName = "Öğrenci", lastName = "") }
                        trySend(Resource.Success(stubProfiles))
                    }
            }
        awaitClose { listener.remove() }
    }

    override fun joinClub(club: Club): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Lütfen giriş yapın."))
            close()
            return@callbackFlow
        }

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

        firestore.collection("users").document(uid)
            .collection("joined_clubs").document(club.id)
            .set(clubData)
            .addOnSuccessListener {
                firestore.collection("users").document(uid)
                    .collection("club_memberships").document(club.id)
                    .set(mapOf("clubId" to club.id, "joinedAt" to FieldValue.serverTimestamp()))
                firestore.collection("clubs").document(club.id)
                    .collection("members").document(uid)
                    .set(mapOf("uid" to uid, "joinedAt" to FieldValue.serverTimestamp()))
                firestore.collection("clubs").document(club.id)
                    .set(mapOf("memberCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
                firestore.collection("club_applications").document(club.id)
                    .set(mapOf("memberCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { err ->
                trySend(Resource.Error(err.toTurkishErrorMessage("Kulübe katılım sağlanamadı.")))
                close()
            }
        awaitClose {}
    }

    override fun isUserJoinedClub(clubId: String): Flow<Boolean> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(uid)
            .collection("club_memberships").document(clubId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot != null && snapshot.exists())
            }
        awaitClose { listener.remove() }
    }

    override fun getClubMembers(clubId: String): Flow<Resource<List<UserProfile>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("clubs").document(clubId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.toTurkishErrorMessage("Kulüp üyeleri alınamadı.")))
                    return@addSnapshotListener
                }
                val uids = snapshot?.documents?.mapNotNull { doc ->
                    val u = doc.id.ifBlank { doc.getString("uid") }
                    if (u.isNullOrBlank()) null else u
                } ?: emptyList()

                if (uids.isEmpty()) {
                    trySend(Resource.Success(emptyList()))
                    return@addSnapshotListener
                }

                firestore.collection("users")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), uids.take(30))
                    .get()
                    .addOnSuccessListener { userSnaps ->
                        val profiles = userSnaps.documents.map { doc ->
                            UserProfile(
                                uid = doc.id,
                                firstName = doc.getString("firstName") ?: "",
                                lastName = doc.getString("lastName") ?: "",
                                email = doc.getString("email") ?: "",
                                faculty = doc.getString("faculty") ?: "",
                                department = doc.getString("department") ?: "",
                                grade = doc.getString("grade") ?: "",
                                profileImageUrl = doc.getString("profileImageUrl") ?: ""
                            )
                        }
                        trySend(Resource.Success(profiles))
                    }
                    .addOnFailureListener {
                        val stubProfiles = uids.map { uid -> UserProfile(uid = uid, firstName = "Öğrenci", lastName = "") }
                        trySend(Resource.Success(stubProfiles))
                    }
            }
        awaitClose { listener.remove() }
    }
}