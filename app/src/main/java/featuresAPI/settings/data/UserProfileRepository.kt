package featuresAPI.settings.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val updatedAt: Long = 0L
)

class UserProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    val currentUserEmail: String
        get() = auth.currentUser?.email.orEmpty()

    val currentFirebaseDisplayName: String
        get() = auth.currentUser?.displayName.orEmpty()

    suspend fun getCurrentProfile(): Result<UserProfile> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("No signed-in user"))

        return try {
            val snapshot = database.getReference("users").child(uid).get().awaitTask()
            Result.success(snapshot.toUserProfile(uid))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun saveCurrentProfile(username: String, displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("No signed-in user"))

        val profile = UserProfile(
            uid = uid,
            username = username.trim(),
            displayName = displayName.trim(),
            updatedAt = System.currentTimeMillis()
        )
        val payload = mapOf(
            "uid" to profile.uid,
            "username" to profile.username,
            "displayName" to profile.displayName,
            "updatedAt" to profile.updatedAt
        )

        return try {
            database.getReference("users").child(uid).setValue(payload).awaitTask()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private fun DataSnapshot.toUserProfile(uid: String): UserProfile {
        return UserProfile(
            uid = child("uid").getValue(String::class.java).orEmpty().ifBlank { uid },
            username = child("username").getValue(String::class.java).orEmpty(),
            displayName = child("displayName").getValue(String::class.java).orEmpty(),
            updatedAt = (child("updatedAt").value as? Number)?.toLong() ?: 0L
        )
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
    }
}
