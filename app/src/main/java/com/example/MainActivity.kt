package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ChatViewModel
import com.example.ui.components.NewChatDialog
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ConversationsScreen
import com.example.ui.screens.PeersScreen
import com.example.ui.screens.RelaySettingsScreen
import com.example.ui.screens.SetupAccountScreen
import com.example.ui.theme.ClestraTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClestraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val account by viewModel.userAccount.collectAsState()

                    Crossfade(
                        targetState = account != null,
                        label = "onboarding_crossfade"
                    ) { isConfigured ->
                        if (!isConfigured) {
                            SetupAccountScreen(viewModel = viewModel)
                        } else {
                            ClestraApp(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClestraApp(viewModel: ChatViewModel) {
    val navController = rememberNavController()
    var showNewChatDialog by remember { mutableStateOf(false) }
    val peers by viewModel.peers.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "conversations"
    ) {
        composable("conversations") {
            ConversationsScreen(
                viewModel = viewModel,
                onOpenChat = { peerId ->
                    navController.navigate("chat/$peerId")
                },
                onOpenPeers = {
                    navController.navigate("peers")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onOpenNewChat = {
                    showNewChatDialog = true
                }
            )
        }

        composable(
            route = "chat/{peerId}",
            arguments = listOf(navArgument("peerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: return@composable
            ChatDetailScreen(
                viewModel = viewModel,
                peerId = peerId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("peers") {
            PeersScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                },
                onChatWithPeer = { peerId ->
                    navController.navigate("chat/$peerId") {
                        popUpTo("conversations")
                    }
                }
            )
        }

        composable("settings") {
            RelaySettingsScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }

    if (showNewChatDialog) {
        NewChatDialog(
            peers = peers,
            onDismiss = { showNewChatDialog = false },
            onSelectPeer = { peerId ->
                showNewChatDialog = false
                navController.navigate("chat/$peerId")
            },
            onAddNewPeerAndChat = { peerId, name ->
                viewModel.addPeer(peerId, name)
                showNewChatDialog = false
                navController.navigate("chat/$peerId")
            }
        )
    }
}
