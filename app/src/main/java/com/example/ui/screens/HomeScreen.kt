package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatViewModel
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraTextSoft

@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onOpenChat: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = ClestraCream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                                contentDescription = "Chats"
                            )
                        },
                        label = {
                            Text(
                                text = "Chats",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ClestraCopper,
                            unselectedIconColor = ClestraTextSoft,
                            selectedTextColor = ClestraCharcoal,
                            unselectedTextColor = ClestraTextSoft,
                            indicatorColor = ClestraBubbleSent.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("nav_chats")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.PersonAdd else Icons.Outlined.PersonAdd,
                                contentDescription = "Requests"
                            )
                        },
                        label = {
                            Text(
                                text = "Requests",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ClestraCopper,
                            unselectedIconColor = ClestraTextSoft,
                            selectedTextColor = ClestraCharcoal,
                            unselectedTextColor = ClestraTextSoft,
                            indicatorColor = ClestraBubbleSent.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("nav_requests")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "Profile"
                            )
                        },
                        label = {
                            Text(
                                text = "Profile",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ClestraCopper,
                            unselectedIconColor = ClestraTextSoft,
                            selectedTextColor = ClestraCharcoal,
                            unselectedTextColor = ClestraTextSoft,
                            indicatorColor = ClestraBubbleSent.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onOpenNewChat,
                    shape = CircleShape,
                    containerColor = ClestraCopper,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("new_chat_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat"
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
                1 -> RequestsScreen(
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
