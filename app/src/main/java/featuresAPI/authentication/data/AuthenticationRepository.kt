package featuresAPI.authentication.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AuthenticationRepository(private val firebaseAuth: FirebaseAuth) {

    val currentUserUid: String?
        get() = firebaseAuth.currentUser?.uid

    val hasUserSession: Boolean
        get() = firebaseAuth.currentUser != null

    fun loginWithEmail(email: String, wordPass: String): Flow<Resource<String>> = callbackFlow {
        trySend(Resource.Loading)

        firebaseAuth.signInWithEmailAndPassword(email, wordPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = task.result?.user?.uid ?: ""
                    trySend(Resource.Success(userId))
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Authentication failed"
                    trySend(Resource.Error(errorMsg))
                }
            }

        awaitClose { /* Clean up any listeners here if needed */ }
    }

    fun signUpWithEmail(email: String, wordPass: String): Flow<Resource<String>> = callbackFlow {
        trySend(Resource.Loading)

        firebaseAuth.createUserWithEmailAndPassword(email, wordPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = task.result?.user?.uid ?: ""
                    trySend(Resource.Success(userId))
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Registration failed"
                    trySend(Resource.Error(errorMsg))
                }
            }

        awaitClose { /* Clean up */ }
    }

    fun logUserOut() {
        firebaseAuth.signOut()
    }
}