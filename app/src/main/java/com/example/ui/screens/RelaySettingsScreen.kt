package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatViewModel
import com.example.ui.components.ClestraPillInput
import com.example.ui.components.ClestraWallpaperBox
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.neumorphicPressed
import com.example.ui.components.neumorphicRaised
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraTextSoft
import com.example.ui.theme.ClestraWarmBeige

@Composable
fun RelaySettingsScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val account by viewModel.userAccount.collectAsState()
    val currentUrl by viewModel.serverlessUrl.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val testResult by viewModel.relayTestResult.collectAsState()
    val mongoSecret = viewModel.mongoDbSecret

    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }

    val myId = account?.id ?: "CLS-CONNECTING"
    val myName = account?.displayName ?: "Clestra User"

    ClestraWallpaperBox(
        intensePattern = false,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicRaised(
                        cornerRadius = 22.dp,
                        elevation = 5.dp,
                        containerColor = ClestraNeuSurface
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(42.dp)
                        .neumorphicRaised(
                            cornerRadius = 14.dp,
                            elevation = 3.dp,
                            containerColor = ClestraNeuSurface
                        )
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ClestraCopper
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings & Relay",
                    color = ClestraCharcoal,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                NeumorphicCard(
                    cornerRadius = 20.dp,
                    elevation = 6.dp,
                    containerColor = ClestraNeuSurface,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = myName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClestraCharcoal
                            )
                            val myTag = account?.activeTag
                            if (!myTag.isNullOrBlank()) {
                                Spacer(Modifier.width(6.dp))
                                com.example.ui.components.SoftUiBadgeTag(label = myTag)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = ClestraTextSoft,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (myId == "dev") "Dev Account • Active Clestra ID" else "Permanent Clestra ID",
                                fontSize = 11.sp,
                                color = if (myId == "dev") ClestraOnline else ClestraTextSoft,
                                fontWeight = if (myId == "dev") FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neumorphicPressed(
                                    cornerRadius = 14.dp,
                                    containerColor = ClestraNeuWell
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = myId,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClestraCharcoal,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(myId))
                                    Toast.makeText(context, "Copied ID: $myId", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .neumorphicRaised(
                                        cornerRadius = 10.dp,
                                        elevation = 3.dp,
                                        containerColor = ClestraNeuSurface
                                    )
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy ID",
                                    tint = ClestraCopperDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                NeumorphicCard(
                    cornerRadius = 20.dp,
                    elevation = 6.dp,
                    containerColor = ClestraNeuSurface,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = ClestraCopper,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "MongoDB Secrets Status",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClestraCharcoal
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        if (mongoSecret.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphicPressed(
                                        cornerRadius = 14.dp,
                                        containerColor = ClestraOnline.copy(alpha = 0.14f)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ClestraOnline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "MONGODB_URI configured via BuildConfig.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ClestraCharcoal
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphicPressed(
                                        cornerRadius = 14.dp,
                                        containerColor = ClestraWarmBeige
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "MONGODB_URI not configured yet.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ClestraCopperDark
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Add MONGODB_URI in the AI Studio Secrets panel or set your serverless relay endpoint URL below.",
                                    fontSize = 11.sp,
                                    color = ClestraTextSoft
                                )
                            }
                        }
                    }
                }
            }

            item {
                NeumorphicCard(
                    cornerRadius = 20.dp,
                    elevation = 6.dp,
                    containerColor = ClestraNeuSurface,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = ClestraCopper,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Serverless Relay Endpoint",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClestraCharcoal
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "URL for offline message queuing and sync.",
                            fontSize = 12.sp,
                            color = ClestraTextSoft
                        )

                        Spacer(Modifier.height(12.dp))

                        ClestraPillInput(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            hint = "https://clestra-relay.vercel.app",
                            modifier = Modifier.testTag("relay_url_input")
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            NeumorphicButton(
                                onClick = {
                                    viewModel.updateServerlessUrl(urlInput.trim())
                                    viewModel.testRelayConnection()
                                },
                                cornerRadius = 12.dp,
                                containerColor = ClestraNeuSurface,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("test_relay_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.NetworkCheck,
                                        contentDescription = null,
                                        tint = ClestraCopper,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Test", color = ClestraCopper, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            NeumorphicButton(
                                onClick = {
                                    viewModel.updateServerlessUrl(urlInput.trim())
                                    Toast.makeText(context, "URL saved", Toast.LENGTH_SHORT).show()
                                },
                                cornerRadius = 12.dp,
                                containerColor = ClestraCopper,
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("save_url_button")
                            ) {
                                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (testResult != null) {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphicPressed(
                                        cornerRadius = 12.dp,
                                        containerColor = ClestraWarmBeige
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = testResult ?: "",
                                    fontSize = 12.sp,
                                    color = ClestraCharcoal
                                )
                            }
                        }
                    }
                }
            }

            item {
                NeumorphicCard(
                    cornerRadius = 20.dp,
                    elevation = 6.dp,
                    containerColor = ClestraNeuSurface,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = null,
                                tint = ClestraCopper,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Message Synchronization",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClestraCharcoal
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Sync status: $syncStatus",
                            fontSize = 12.sp,
                            color = ClestraTextSoft
                        )

                        Spacer(Modifier.height(14.dp))

                        NeumorphicButton(
                            onClick = { viewModel.syncNow() },
                            enabled = !isSyncing,
                            cornerRadius = 14.dp,
                            containerColor = ClestraCopper,
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_sync_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Syncing...", color = Color.White, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pull Queued Messages Now", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
