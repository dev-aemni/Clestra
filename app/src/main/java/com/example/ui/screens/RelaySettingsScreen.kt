package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraCream
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

    val serverlessSnippet = remember {
        """
// Clestra Serverless Offline Relay (Vercel / Netlify / Node.js)
const { MongoClient } = require('mongodb');
let cachedClient = null;

async function getDb() {
  if (!cachedClient) {
    cachedClient = new MongoClient(process.env.MONGODB_URI);
    await cachedClient.connect();
  }
  return cachedClient.db('clestra_relay');
}

module.exports = async (req, res) => {
  const db = await getDb();
  const col = db.collection('pending_messages');

  if (req.method === 'POST') {
    const { senderId, recipientId, compressedContent, timestamp } = req.body;
    await col.insertOne({ senderId, recipientId, compressedContent, timestamp, delivered: false });
    return res.status(201).json({ success: true, status: 'queued_offline_serverless' });
  }

  if (req.method === 'GET') {
    const recipientId = req.query.recipientId;
    const pending = await col.find({ recipientId }).sort({ timestamp: 1 }).toArray();
    if (pending.length > 0) {
      await col.deleteMany({ _id: { ${'$'}in: pending.map(m => m._id) } });
    }
    return res.status(200).json({ success: true, count: pending.length, messages: pending });
  }
};
        """.trimIndent()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClestraCream)
    ) {
        Surface(
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ClestraCopper
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
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
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = myName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClestraCharcoal
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ClestraTextSoft, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Permanent Clestra ID",
                                fontSize = 11.sp,
                                color = ClestraTextSoft
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ClestraBubbleSent.copy(alpha = 0.55f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = myId,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClestraCharcoal
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(myId))
                                    Toast.makeText(context, "Copied ID: $myId", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = ClestraCopperDark, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = ClestraCopper, modifier = Modifier.size(20.dp))
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
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ClestraOnline.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ClestraOnline, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "MONGODB_URI configured via BuildConfig.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ClestraCharcoal
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ClestraWarmBeige,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
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
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = ClestraCopper, modifier = Modifier.size(20.dp))
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
                            containerColor = ClestraCream,
                            modifier = Modifier.testTag("relay_url_input")
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateServerlessUrl(urlInput.trim())
                                    viewModel.testRelayConnection()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("test_relay_button")
                            ) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = ClestraCopper, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Test", color = ClestraCopper)
                            }

                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    viewModel.updateServerlessUrl(urlInput.trim())
                                    Toast.makeText(context, "URL saved", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ClestraCopper, contentColor = Color.White),
                                modifier = Modifier.testTag("save_url_button")
                            ) {
                                Text("Save")
                            }
                        }

                        if (testResult != null) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ClestraWarmBeige,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = testResult ?: "",
                                    fontSize = 12.sp,
                                    color = ClestraCharcoal,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = ClestraCopper, modifier = Modifier.size(20.dp))
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

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.syncNow() },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ClestraCopper, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_sync_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Pull Queued Messages Now")
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = ClestraCopper, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Node.js Serverless API",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ClestraCharcoal
                                )
                            }

                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(serverlessSnippet))
                                    Toast.makeText(context, "Serverless code copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_snippet_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = ClestraCopper)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ClestraCharcoal,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = serverlessSnippet,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = ClestraBubbleSent,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
