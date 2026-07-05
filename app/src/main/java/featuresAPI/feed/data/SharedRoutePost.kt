package featuresAPI.feed.data

data class SharedRoutePost(
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val routeId: String = "",
    val tripName: String = "",
    val caption: String = "",
    val routeString: String = "",
    val createdAt: Long = 0L,
    val pointCount: Int = 0,
    val photoUrls: List<String> = emptyList(),
    val photos: List<SharedRoutePhoto> = emptyList()
)