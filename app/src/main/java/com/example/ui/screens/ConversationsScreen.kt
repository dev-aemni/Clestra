package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ConversationEntity
import com.example.ui.ChatViewModel
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraError
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraSand
import com.example.ui.theme.ClestraTextSoft
import com.example.ui.theme.ClestraWarmBeige
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationsScreen(
    viewModel: ChatViewModel,
    onOpenChat: (String) -> Unit,
    onOpenNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.filteredConversations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showSearch by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClestraCream)
    ) {
        // Top App Bar matching fragment_chat_list_27b38c16.xml
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Clestra",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Clestra",
                        color = ClestraCharcoal,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (showSearch) {
                                viewModel.searchQuery.value = ""
                            }
                            showSearch = !showSearch
                        },
                        modifier = Modifier.testTag("btn_search")
                    ) {
                        Icon(
                            imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search chats",
                            tint = ClestraTextSoft,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showSearch) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = {
                            Text(
                                text = "Search by name, message, or Clestra ID...",
                                color = ClestraTextSoft,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ClestraCream,
                            unfocusedContainerColor = ClestraCream,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = ClestraCharcoal,
                            unfocusedTextColor = ClestraCharcoal,
                            cursorColor = ClestraCopper
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            .testTag("search_field")
                    )
                }
            }
        }

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(ClestraBubbleSent),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Clestra Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching chats" else "No conversations yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClestraCharcoal
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter a user's Clestra ID to start an end-to-end encrypted chat right away.",
                        fontSize = 14.sp,
                        color = ClestraTextSoft,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onOpenNewChat,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClestraCopper,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("empty_start_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start New Chat",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("conversations_list")
            ) {
                items(conversations, key = { it.peerId }) { convo ->
                    ConversationItem(
                        conversation = convo,
                        onClick = { onOpenChat(convo.peerId) }
                    )
                    HorizontalDivider(
                        color = ClestraSand.copy(alpha = 0.35f),
                        modifier = Modifier.padding(start = 80.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowBg = if (conversation.isOfficial) {
        ClestraWarmBeige.copy(alpha = 0.55f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable { onClick() }
            .padding(16.dp)
            .testTag("conversation_item_${conversation.peerId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ClestraBubbleSent)
                    .then(
                        if (conversation.isOfficial) {
                            Modifier.border(1.5.dp, ClestraCopper, RoundedCornerShape(16.dp))
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (conversation.isOfficial) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Clestra Official",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = conversation.peerName.firstOrNull()?.uppercase() ?: "?",
                        color = ClestraCharcoal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(
                        when {
                            conversation.isBlocked -> ClestraError
                            conversation.isPeerOnline || conversation.isOfficial -> ClestraOnline
                            else -> ClestraCopper
                        }
                    )
                    .border(2.dp, ClestraCream, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = conversation.peerName,
                        color = ClestraCharcoal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.isOfficial) {
                        Spacer(modifier = Modifier.width(6.dp))
                        OfficialTag()
                    }
                    if (conversation.isMuted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Muted",
                            tint = ClestraTextSoft,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (conversation.isBlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Blocked",
                            color = ClestraError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatTime(conversation.lastTimestamp),
                    color = ClestraTextSoft,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage,
                    color = ClestraTextSoft,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(ClestraCopper),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date(millis))
        }
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(millis))
        }
    }
}
