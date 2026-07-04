package featuresAPI.feed.data

import com.example.routetracker.data.local.track.TrackedRoute
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.PolyUtil
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

data class SharedRoutePost(
    val postId: String = "",
    val userId: String = "",
    val routeId: String = "",
    val tripName: String = "",
    val caption: String = "",
    val routeString: String = "",
    val createdAt: Long = 0L,
    val pointCount: Int = 0,
    val photoUrls: List<String> = emptyList()
)

class FeedRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    suspend fun uploadSharedRoute(
        route: TrackedRoute,
        caption: String,
        userId: String?
    ): Result<Unit> {
        return try {
            val postReference = database.getReference("sharedRoutes").push()
            val postId = postReference.key
                ?: return Result.failure(IllegalStateException("Could not create shared route id"))
            val routeString = PolyUtil.encode(route.trackedRoute)
            val payload = mapOf(
                "postId" to postId,
                "userId" to userId.orEmpty(),
                "routeId" to route.id,
                "tripName" to route.tripName,
                "caption" to caption.trim(),
                "routeString" to routeString,
                "createdAt" to System.currentTimeMillis(),
                "pointCount" to route.trackedRoute.size,
                // TODO: Upload selected photos to Firebase Storage.
                // TODO: Replace empty photoUrls with Firebase Storage download URLs.
                "photoUrls" to emptyList<String>()
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

    private fun DataSnapshot.toSharedRoutePost(): SharedRoutePost? {
        val postId = child("postId").getValue(String::class.java) ?: key ?: return null
        val photoUrls = child("photoUrls").children.mapNotNull {
            it.getValue(String::class.java)
        }

        // photos are just URLs here, Storage upload happens before this step later
        return SharedRoutePost(
            postId = postId,
            userId = child("userId").getValue(String::class.java).orEmpty(),
            routeId = child("routeId").getValue(String::class.java).orEmpty(),
            tripName = child("tripName").getValue(String::class.java).orEmpty(),
            caption = child("caption").getValue(String::class.java).orEmpty(),
            routeString = child("routeString").getValue(String::class.java).orEmpty(),
            createdAt = (child("createdAt").value as? Number)?.toLong() ?: 0L,
            pointCount = (child("pointCount").value as? Number)?.toInt() ?: 0,
            photoUrls = photoUrls
        )
    }

    // TODO: Add delete post functionality later.
}
