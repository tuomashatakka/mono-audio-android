package com.mono.signal.model

enum class SortBy(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
    DATE("Date"),
}

enum class GroupBy(val label: String) {
    NONE("None"),
    ARTIST("Artist"),
    ALBUM("Album"),
    FILE_PATH("File path"),
    FOLDER("Folder"),
}

/** A rendered library section: an optional header plus its tracks (already sorted). */
data class TrackGroup(
    val header: String?,
    val tracks: List<Track>,
)

/** Pure: sort then group a flat track list for display. Groups and contents follow [sortBy]. */
fun List<Track>.arrange(sortBy: SortBy, groupBy: GroupBy): List<TrackGroup> {
    val trackComparator = comparatorFor(sortBy)
    val sorted = sortedWith(trackComparator)
    return when (groupBy) {
        GroupBy.NONE -> listOf(TrackGroup(null, sorted))
        GroupBy.ARTIST -> sorted.toSortedGroups(trackComparator) { it.artist.ifBlank { "Unknown artist" } }
        GroupBy.ALBUM -> sorted.toSortedGroups(trackComparator) { it.album.ifBlank { "Unknown album" } }
        GroupBy.FILE_PATH -> sorted.toSortedGroups(trackComparator) { it.filePath.ifBlank { "Unknown path" } }
        GroupBy.FOLDER -> sorted.toSortedGroups(trackComparator) { it.folder.ifBlank { "Unknown folder" } }
    }
}

private fun comparatorFor(sortBy: SortBy): Comparator<Track> = when (sortBy) {
    SortBy.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    SortBy.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
    SortBy.ALBUM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.album }
    SortBy.DURATION -> compareBy { it.durationMs }
    SortBy.DATE -> compareByDescending { it.dateAddedSeconds }
}

private inline fun List<Track>.toSortedGroups(
    trackComparator: Comparator<Track>,
    crossinline key: (Track) -> String,
): List<TrackGroup> = groupBy { key(it) }
    .map { (header, tracks) -> TrackGroup(header, tracks.sortedWith(trackComparator)) }
    .sortedWith(compareBy<TrackGroup> { it.tracks.firstOrNull() == null }.then(trackGroupComparator(trackComparator)))

private fun trackGroupComparator(trackComparator: Comparator<Track>): Comparator<TrackGroup> = Comparator { left, right ->
    val l = left.tracks.firstOrNull()
    val r = right.tracks.firstOrNull()
    when {
        l != null && r != null -> trackComparator.compare(l, r).takeIf { it != 0 }
            ?: String.CASE_INSENSITIVE_ORDER.compare(left.header.orEmpty(), right.header.orEmpty())
        else -> String.CASE_INSENSITIVE_ORDER.compare(left.header.orEmpty(), right.header.orEmpty())
    }
}
