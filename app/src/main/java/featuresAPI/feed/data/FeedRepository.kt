package featuresAPI.feed.data

import android.content.ContentResolver
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.PolyUtil
import data.local.track.TrackedRoute
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class SharedRoutePhoto(
    val url: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

class FeedRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val contentResolver: ContentResolver? = null
) {

    suspend fun uploadSharedRoute(
        route: TrackedRoute,
        caption: String,
        userId: String?,
        photoUris: List<Uri> = emptyList()
    ): Result<Unit> {
        return try {
            val postReference = database.getReference("sharedRoutes").push()
            val postId = postReference.key
            if (postId == null) {
                return Result.failure(IllegalStateException("Could not create shared route id"))
            }
            val photos = uploadSharedRoutePhotos(
                userId = userId,
                postId = postId,
                photoUris = photoUris
            )
            val photoUrls = photos.map { it.url }
            val author = loadAuthorProfile(userId)
            val routeString = PolyUtil.encode(route.trackedRoute)
            val payload = mapOf(
                "postId" to postId,
                "userId" to userId.orEmpty(),
                "authorName" to author.first,
                "authorUsername" to author.second,
                "routeId" to route.id,
                "tripName" to route.tripName,
                "caption" to caption.trim(),
                "routeString" to routeString,
                "createdAt" to System.currentTimeMillis(),
                "pointCount" to route.trackedRoute.size,
                "photoUrls" to photoUrls,
                "photos" to photos.map { it.toFirebaseMap() }
            )

            suspendCancellableCoroutine { continuation ->
                postReference.setValue(payload)
                    .addOnSuccessListener {
                        if (continuation.isActive) {
                            continuation.resume(Result.success(Unit))
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(exception))
                        }
                    }
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun updatePostCaption(
        postId: String,
        newCaption: String,
        currentUserId: String?
    ): Result<Unit> {
        if (postId.isBlank() || currentUserId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Missing post or user"))
        }

        return try {
            val postReference = database.getReference("sharedRoutes").child(postId)
            val snapshot = postReference.get().awaitTask()
            val ownerId = snapshot.child("userId").getValue(String::class.java).orEmpty()
            if (ownerId != currentUserId) {
                return Result.failure(SecurityException("Post does not belong to current user"))
            }
            postReference.child("caption").setValue(newCaption.trim()).awaitTask()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun deletePost(
        post: SharedRoutePost,
        currentUserId: String?
    ): Result<Unit> {
        if (post.postId.isBlank() || currentUserId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Missing post or user"))
        }
        if (post.userId != currentUserId) {
            return Result.failure(SecurityException("Post does not belong to current user"))
        }

        return try {
            database.getReference("sharedRoutes").child(post.postId).removeValue().awaitTask()
            deletePostPhotosBestEffort(post)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend fun uploadSharedRoutePhotos(
        userId: String?,
        postId: String,
        photoUris: List<Uri>
    ): List<SharedRoutePhoto> {
        if (photoUris.isEmpty()) {
            // no photos selected, so this post just gets an empty photoUrls list
            return emptyList()
        }

        val storageUserId = userId?.takeIf { it.isNotBlank() } ?: "unknownUser"

        // upload photos first so the post can save real download URLs
        return photoUris.mapIndexed { index, uri ->
            val photoReference = storage.reference
                .child("sharedRoutes/$storageUserId/$postId/photos/$index.jpg")
            val photoLocation = readPhotoLocation(uri)
            photoReference.putFile(uri).awaitTask()
            val downloadUrl = photoReference.downloadUrl.awaitTask().toString()
            SharedRoutePhoto(
                url = downloadUrl,
                latitude = photoLocation?.first,
                longitude = photoLocation?.second
            )
        }
    }

    private fun readPhotoLocation(uri: Uri): Pair<Double, Double>? {
        return runCatching {
            val resolver = contentResolver ?: return null
            resolver.openInputStream(uri)?.use { inputStream ->
                // EXIF GPS is optional, so missing location is fine
                val latLong = FloatArray(2)
                val hasLocation = ExifInterface(inputStream).getLatLong(latLong)
                if (hasLocation) {
                    latLong[0].toDouble() to latLong[1].toDouble()
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private suspend fun loadAuthorProfile(userId: String?): Pair<String, String> {
        if (userId.isNullOrBlank()) {
            return "You" to ""
        }

        val snapshot = runCatching {
            database.getReference("users").child(userId).get().awaitTask()
        }.getOrNull()

        val username = snapshot?.child("username")?.getValue(String::class.java).orEmpty()
        val displayName = snapshot?.child("displayName")?.getValue(String::class.java).orEmpty()
        val authUser = auth.currentUser
        val fallbackName = authUser?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: authUser?.email
                ?.substringBefore("@")
                ?.takeIf { it.isNotBlank() }
            ?: "You"

        return displayName.ifBlank { username.ifBlank { fallbackName } } to username
    }

    private suspend fun deletePostPhotosBestEffort(post: SharedRoutePost) {
        val count = maxOf(post.photos.size, post.photoUrls.size)
        repeat(count) { index ->
            runCatching {
                storage.reference
                    .child("sharedRoutes/${post.userId}/${post.postId}/photos/$index.jpg")
                    .delete()
                    .awaitTask()
            }
        }
    }

    fun getSharedRoutesForUser(userId: String?): Flow<List<SharedRoutePost>> = callbackFlow {
        if (userId.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // grab only this user's shared routes for the Feed
        val query = database.getReference("sharedRoutes")
            .orderByChild("userId")
            .equalTo(userId)

        // this listens to Firebase and updates the Feed when posts change
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = snapshot.children.mapNotNull { it.toSharedRoutePost() }
                    .sortedByDescending { it.createdAt }
                trySend(posts)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    fun getSharedRoutes(): Flow<List<SharedRoutePost>> = callbackFlow {
        val query = database.getReference("sharedRoutes")
            .orderByChild("createdAt")
            .limitToLast(50)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = snapshot.children.mapNotNull { it.toSharedRoutePost() }
                    .sortedByDescending { it.createdAt }
                trySend(posts)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    private fun DataSnapshot.toSharedRoutePost(): SharedRoutePost? {
        val postId = child("postId").getValue(String::class.java) ?: key ?: return null
        val existingPhotoUrls = child("photoUrls").children.mapNotNull {
            it.getValue(String::class.java)
        }
        val photos = child("photos").children.mapNotNull { it.toSharedRoutePhoto() }
        val finalPhotos = photos.ifEmpty {
            existingPhotoUrls.map { url -> SharedRoutePhoto(url = url) }
        }
        val finalPhotoUrls = existingPhotoUrls.ifEmpty {
            finalPhotos.map { it.url }
        }

        return SharedRoutePost(
            postId = postId,
            userId = child("userId").getValue(String::class.java).orEmpty(),
            authorName = child("authorName").getValue(String::class.java).orEmpty(),
            authorUsername = child("authorUsername").getValue(String::class.java).orEmpty(),
            routeId = child("routeId").getValue(String::class.java).orEmpty(),
            tripName = child("tripName").getValue(String::class.java).orEmpty(),
            caption = child("caption").getValue(String::class.java).orEmpty(),
            routeString = child("routeString").getValue(String::class.java).orEmpty(),
            createdAt = (child("createdAt").value as? Number)?.toLong() ?: 0L,
            pointCount = (child("pointCount").value as? Number)?.toInt() ?: 0,
            photoUrls = finalPhotoUrls,
            photos = finalPhotos
        )
    }

    private fun DataSnapshot.toSharedRoutePhoto(): SharedRoutePhoto? {
        val url = child("url").getValue(String::class.java).orEmpty()
        if (url.isBlank()) return null

        return SharedRoutePhoto(
            url = url,
            latitude = (child("latitude").value as? Number)?.toDouble(),
            longitude = (child("longitude").value as? Number)?.toDouble()
        )
    }
}

private fun SharedRoutePhoto.toFirebaseMap(): Map<String, Any?> {
    return mapOf(
        "url" to url,
        "latitude" to latitude,
        "longitude" to longitude
    )
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
