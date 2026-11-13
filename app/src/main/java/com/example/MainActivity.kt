package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ChatViewModel
import com.example.ui.components.NewChatDialog
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RelaySettingsScreen
import com.example.ui.screens.SetupAccountScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.ClestraTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        viewModel.setAppInForeground(true)
    }

    override fun onStop() {
        super.onStop()
        viewModel.setAppInForeground(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val initialPeerId = intent?.getStringExtra("open_peer_id")

        setContent {
            ClestraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    val account by viewModel.userAccount.collectAsState()

                    LaunchedEffect(Unit) {
                        delay(1100)
                        showSplash = false
                    }

                    val destinationState = when {
                        showSplash -> "splash"
                        account == null -> "setup"
                        else -> "home"
                    }

                    val view = LocalView.current
                    if (!view.isInEditMode) {
                        SideEffect {
                            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
                            val controller = WindowCompat.getInsetsController(window, view)
                            controller.isAppearanceLightStatusBars = destinationState != "splash"
                            controller.isAppearanceLightNavigationBars = destinationState != "splash"
                        }
                    }

                    Crossfade(
                        targetState = destinationState,
                        label = "root_crossfade"
                    ) { state ->
                        when (state) {
                            "splash" -> SplashScreen()
                            "setup" -> SetupAccountScreen(viewModel = viewModel)
                            else -> ClestraApp(
                                viewModel = viewModel,
                                initialPeerId = initialPeerId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClestraApp(
    viewModel: ChatViewModel,
    initialPeerId: String? = null
) {
    val navController = rememberNavController()
    var showNewChatDialog by remember { mutableStateOf(false) }
    val peers by viewModel.peers.collectAsState()

    LaunchedEffect(initialPeerId) {
        if (!initialPeerId.isNullOrBlank()) {
            navController.navigate("chat/$initialPeerId")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onOpenChat = { peerId ->
                    navController.navigate("chat/$peerId")
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
