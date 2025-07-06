package com.example.fediversezdfregurgitated.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fediversezdfregurgitated.data.local.RealmStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineItem(status: RealmStatus, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(status.authorAvatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${status.authorDisplayName}'s avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = status.authorDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${status.authorUsername}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // TODO: Render HTML content properly. For now, basic text.
            // Consider using libraries like HtmlCompat or a WebView for rich content if needed.
            Text(
                text = status.content.replace("<[^>]*>".toRegex(), ""), // Basic HTML stripping
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatTimestamp(status.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatusActionsRow(
                status = status,
                onReply = { /* TODO */ },
                onBoost = { /* TODO */ },
                onFavourite = { /* TODO */ },
                onBookmark = { /* TODO */ }
            )
        }
    }
}


@Composable
fun StatusActionsRow(
    status: RealmStatus,
    onReply: (String) -> Unit,
    onBoost: (String, Boolean) -> Unit,
    onFavourite: (String, Boolean) -> Unit,
    onBookmark: (String, Boolean) -> Unit,
    // In a real app, you'd likely have a ViewModel to handle these actions
    // and update the UI optimistically / after confirmation.
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        // horizontalArrangement = Arrangement.SpaceAround // Or SpaceBetween
    ) {
        ActionButton(
            icon = Icons.Default.Reply, // Assuming Icons.Default is available
            text = status.replyCount.takeIf { it > 0 }?.toString(),
            onClick = { onReply(status.id) },
            contentDescription = "Reply to ${status.authorDisplayName}. Current replies: ${status.replyCount}"
        )
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(
            icon = if (status.boosted) Icons.Filled.RepeatOn else Icons.Default.Repeat, // Assuming Icons.Filled/Default is available
            text = status.boostCount.takeIf { it > 0 }?.toString(),
            onClick = { onBoost(status.id, !status.boosted) },
            contentDescription = if (status.boosted) "Undo boost for status by ${status.authorDisplayName}. Current boosts: ${status.boostCount}" else "Boost status by ${status.authorDisplayName}. Current boosts: ${status.boostCount}",
            tint = if (status.boosted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(
            icon = if (status.favourited) Icons.Filled.Favorite else Icons.Default.FavoriteBorder, // Assuming Icons.Filled/Default is available
            text = status.favouriteCount.takeIf { it > 0 }?.toString(),
            onClick = { onFavourite(status.id, !status.favourited) },
            contentDescription = if (status.favourited) "Remove favourite for status by ${status.authorDisplayName}. Current favourites: ${status.favouriteCount}" else "Favourite status by ${status.authorDisplayName}. Current favourites: ${status.favouriteCount}",
            tint = if (status.favourited) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(
            icon = if (status.bookmarked) Icons.Filled.Bookmark else Icons.Default.BookmarkBorder, // Assuming Icons.Filled/Default is available
            onClick = { onBookmark(status.id, !status.bookmarked) },
            contentDescription = if (status.bookmarked) "Remove bookmark for status by ${status.authorDisplayName}" else "Bookmark status by ${status.authorDisplayName}",
            tint = if (status.bookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    text: String? = null,
    onClick: () -> Unit,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp) // Smaller icon for actions
            )
            text?.let {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = tint)
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

//@Preview(showBackground = true)
//@Composable
//fun TimelineItemPreview() {
//    FediverseZDFRegurgitatedTheme {
//        val previewStatus = RealmStatus(
//            id = "1",
//            content = "This is a sample toot content. It's quite interesting! 🐘",
//            authorAvatarUrl = "https://via.placeholder.com/150", // Replace with a real image URL for preview
//            authorDisplayName = "John Doe",
//            authorUsername = "john.doe@example.com",
//            createdAt = System.currentTimeMillis(),
//            timelineType = "local"
//        )
//        TimelineItem(status = previewStatus)
//    }
//}
