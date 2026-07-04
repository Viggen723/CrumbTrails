package featuresAPI.authentication.data

sealed class Resource<out T> {
    object Idle : Resource<Nothing>()
    object Loading : Resource<Nothing>()
    data class Success<out T>(val theData: T) : Resource<T>()
    data class Error(val errorMessage: String) : Resource<Nothing>()
}