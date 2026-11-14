package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.ChatViewModel
import com.example.ui.components.neumorphicPressed
import com.example.ui.components.neumorphicRaised
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraTextSoft

@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onOpenChat: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        containerColor = ClestraCream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicRaised(
                            cornerRadius = 28.dp,
                            elevation = 6.dp,
                            containerColor = ClestraNeuSurface
                        )
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicNavTab(
                        selected = selectedTab == 0,
                        icon = if (selectedTab == 0) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                        label = "Chats",
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nav_chats")
                    )
                    NeumorphicNavTab(
                        selected = selectedTab == 1,
                        icon = if (selectedTab == 1) Icons.Filled.People else Icons.Outlined.People,
                        label = "Contacts",
                        onClick = { selectedTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nav_contacts")
                    )
                    NeumorphicNavTab(
                        selected = selectedTab == 2,
                        icon = if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
                        label = "Profile",
                        onClick = { selectedTab = 2 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nav_profile")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(58.dp)
                        .neumorphicRaised(
                            cornerRadius = 29.dp,
                            elevation = 6.dp,
                            containerColor = ClestraCopper,
                            accentBorderColor = Color.White.copy(alpha = 0.55f)
                        )
                        .clip(CircleShape)
                        .clickable { onOpenNewChat() }
                        .testTag("new_chat_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ConversationsScreen(
                    viewModel = viewModel,
                    onOpenChat = onOpenChat,
                    onOpenNewChat = onOpenNewChat
                )
                1 -> ContactsScreen(
                    viewModel = viewModel,
                    onChatWithPeer = onOpenChat
                )
                2 -> ProfileScreen(
                    viewModel = viewModel,
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun NeumorphicNavTab(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillModifier = if (selected) {
        Modifier.neumorphicPressed(
            cornerRadius = 22.dp,
            containerColor = ClestraNeuSurface
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .then(pillModifier)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) com.example.ui.theme.ClestraCopperDark else ClestraTextSoft,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (selected) ClestraCharcoal else ClestraTextSoft,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
