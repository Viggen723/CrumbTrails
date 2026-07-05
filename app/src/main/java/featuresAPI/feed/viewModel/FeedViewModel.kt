package featuresAPI.feed.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import featuresAPI.feed.data.FeedRepository
import featuresAPI.feed.data.SharedRoutePost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FeedActionStatus {
    data object Idle : FeedActionStatus
    data object Loading : FeedActionStatus
    data class Error(val message: String) : FeedActionStatus
}

class FeedViewModel : ViewModel() {

    private val repository = FeedRepository()
    val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    private val _actionStatus = MutableStateFlow<FeedActionStatus>(FeedActionStatus.Idle)
    val actionStatus: StateFlow<FeedActionStatus> = _actionStatus.asStateFlow()

    val posts: StateFlow<List<SharedRoutePost>> = repository.getSharedRoutes()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun editCaption(post: SharedRoutePost, newCaption: String) {
        viewModelScope.launch {
            _actionStatus.value = FeedActionStatus.Loading
            val result = repository.updatePostCaption(
                postId = post.postId,
                newCaption = newCaption,
                currentUserId = currentUserId
            )
            _actionStatus.value = result.fold(
                onSuccess = { FeedActionStatus.Idle },
                onFailure = { FeedActionStatus.Error(it.toSafeActionMessage()) }
            )
        }
    }

    fun deletePost(post: SharedRoutePost) {
        viewModelScope.launch {
            _actionStatus.value = FeedActionStatus.Loading
            val result = repository.deletePost(
                post = post,
                currentUserId = currentUserId
            )
            _actionStatus.value = result.fold(
                onSuccess = { FeedActionStatus.Idle },
                onFailure = { FeedActionStatus.Error(it.toSafeActionMessage()) }
            )
        }
    }

    private fun Throwable.toSafeActionMessage(): String {
        return "Feed update failed (${javaClass.simpleName})."
    }
}
