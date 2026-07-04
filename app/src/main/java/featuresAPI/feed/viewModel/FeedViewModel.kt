package featuresAPI.feed.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import featuresAPI.feed.data.FeedRepository
import featuresAPI.feed.data.SharedRoutePost
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class FeedViewModel : ViewModel() {

    private val repository = FeedRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    val posts: StateFlow<List<SharedRoutePost>> = repository.getSharedRoutesForUser(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
