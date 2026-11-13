package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PeerEntity
import com.example.ui.ChatViewModel
import com.example.ui.components.ClestraPillInput
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraSand
import com.example.ui.theme.ClestraTextSoft

@Composable
fun RequestsScreen(
    viewModel: ChatViewModel,
    onChatWithPeer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var requestInput by remember { mutableStateOf("") }
    val statusText by viewModel.friendRequestStatus.collectAsState()
    val peers by viewModel.peers.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClestraCream)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Friend Requests",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ClestraCharcoal
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Send request by username",
            fontSize = 14.sp,
            color = ClestraTextSoft
        )

        Spacer(modifier = Modifier.height(12.dp))

        ClestraPillInput(
            value = requestInput,
            onValueChange = { requestInput = it },
            hint = "Enter username or Clestra ID",
            modifier = Modifier.testTag("request_username_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                viewModel.sendFriendRequest(requestInput)
                if (requestInput.isNotBlank()) {
                    requestInput = ""
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ClestraCopper,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("send_request_button")
        ) {
            Text(
                text = "Send Request",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (statusText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusText,
                fontSize = 13.sp,
                color = ClestraCharcoal,
                modifier = Modifier.testTag("request_status_text")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = ClestraSand.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Connected Peers",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ClestraCharcoal
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (peers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No friends connected yet. Enter a username or Clestra ID above.",
                    color = ClestraTextSoft,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(peers, key = { it.peerId }) { peer ->
                    RequestPeerItem(
                        peer = peer,
                        onToggleOnline = { viewModel.togglePeerOnline(peer.peerId) },
                        onChat = { onChatWithPeer(peer.peerId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestPeerItem(
    peer: PeerEntity,
    onToggleOnline: () -> Unit,
    onChat: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onChat() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ClestraBubbleSent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = peer.displayName.firstOrNull()?.uppercase() ?: "?",
                    color = ClestraCharcoal,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.displayName,
                    color = ClestraCharcoal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = peer.peerId,
                    fontFamily = FontFamily.Monospace,
                    color = ClestraTextSoft,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (peer.isOnline) ClestraOnline else ClestraCopper)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (peer.isOnline) "P2P Online" else "Offline • Relay",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (peer.isOnline) ClestraOnline else ClestraCopperDark
                    )
                }
            }

            Switch(
                checked = peer.isOnline,
                onCheckedChange = { onToggleOnline() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ClestraOnline,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = ClestraCopper
                )
            )

            IconButton(onClick = onChat) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Chat with ${peer.displayName}",
                    tint = ClestraCopper
                )
            }
        }
    }
}
