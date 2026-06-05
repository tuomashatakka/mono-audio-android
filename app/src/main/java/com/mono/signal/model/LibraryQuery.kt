package com.mono.signal.model

enum class SortBy(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
}

enum class GroupBy(val label: String) {
    NONE("None"),
    ARTIST("Artist"),
    ALBUM("Album"),
}

/** A rendered library section: an optional header plus its tracks (already sorted). */
data class TrackGroup(
    val header: String?,
    val tracks: List<Track>,
)

/** Pure: sort then group a flat track list for display. */
fun List<Track>.arrange(sortBy: SortBy, groupBy: GroupBy): List<TrackGroup> {
    val sorted = sortedWith(
        when (sortBy) {
            SortBy.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortBy.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            SortBy.ALBUM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.album }
            SortBy.DURATION -> compareBy { it.durationMs }
        },
    )
    return when (groupBy) {
        GroupBy.NONE -> listOf(TrackGroup(null, sorted))
        GroupBy.ARTIST -> sorted.groupBy { it.artist.ifBlank { "Unknown artist" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (k, v) -> TrackGroup(k, v) }
        GroupBy.ALBUM -> sorted.groupBy { it.album.ifBlank { "Unknown album" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (k, v) -> TrackGroup(k, v) }
    }
}
