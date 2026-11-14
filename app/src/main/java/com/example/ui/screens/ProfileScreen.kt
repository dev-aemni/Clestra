package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.ChatViewModel
import com.example.ui.components.ClestraPillInput
import com.example.ui.components.ClestraWallpaperBox
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.SoftUiBadgeTag
import com.example.ui.components.neumorphicPressed
import com.example.ui.components.neumorphicRaised
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraError
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraTextSoft

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val account by viewModel.userAccount.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val redeemStatus by viewModel.redeemStatus.collectAsState()

    var redeemCodeInput by remember { mutableStateOf("") }
    var codeVisible by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.updateProfilePhoto(uri.toString())
        }
    }

    val displayName = account?.displayName?.ifBlank { null } ?: "Clestra User"
    val bio = account?.bio?.ifBlank { null } ?: "Private. Secure. Yours."
    val myId = account?.id ?: "CLS-CONNECTING"
    val photoUri = account?.photoUri
    val activeTag = account?.activeTag
    val unlockedTags = remember(account?.unlockedTags) {
        account?.unlockedTags
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
    }

    ClestraWallpaperBox(
        intensePattern = false,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 108.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(106.dp)
                    .neumorphicRaised(
                        cornerRadius = 26.dp,
                        elevation = 7.dp,
                        containerColor = ClestraBubbleSent
                    )
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp))
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NeumorphicButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                cornerRadius = 14.dp,
                containerColor = ClestraCopper,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier.testTag("pick_photo_button")
            ) {
                Text(
                    text = "Change Photo",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClestraCharcoal,
                    modifier = Modifier.testTag("profile_username")
                )
                if (!activeTag.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    SoftUiBadgeTag(
                        label = activeTag,
                        modifier = Modifier.testTag("profile_active_tag")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = bio,
                fontSize = 14.sp,
                color = ClestraTextSoft,
                modifier = Modifier.testTag("profile_bio")
            )

            Spacer(modifier = Modifier.height(22.dp))

            NeumorphicCard(
                cornerRadius = 20.dp,
                elevation = 6.dp,
                containerColor = Color.White,
                contentPadding = PaddingValues(16.dp),
                onClick = {
                    clipboard.setText(AnnotatedString(myId))
                    Toast.makeText(context, "Clestra ID copied: $myId", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Your Clestra ID",
                        fontSize = 11.sp,
                        color = ClestraTextSoft,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicPressed(
                                cornerRadius = 14.dp,
                                containerColor = ClestraNeuWell
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .neumorphicRaised(
                                    cornerRadius = 10.dp,
                                    elevation = 3.dp,
                                    containerColor = Color.White
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Clestra ID",
                                tint = ClestraCopper,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secret Tag Redeem Section
            NeumorphicCard(
                cornerRadius = 20.dp,
                elevation = 6.dp,
                containerColor = Color.White,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Redeem,
                            contentDescription = null,
                            tint = ClestraCopper,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Account Badges & Redeem",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClestraCharcoal
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter a redeem code or custom tag:NAME@#HEXCOLOR to unlock a badge.",
                        fontSize = 12.sp,
                        color = ClestraTextSoft
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ClestraPillInput(
                        value = redeemCodeInput,
                        onValueChange = {
                            redeemCodeInput = it
                            if (redeemStatus != null) viewModel.clearRedeemStatus()
                        },
                        hint = "Enter redeem code",
                        isPassword = true,
                        passwordVisible = codeVisible,
                        onTogglePasswordVisible = { codeVisible = !codeVisible },
                        modifier = Modifier.testTag("redeem_code_input")
                    )

                    if (redeemStatus != null) {
                        val (ok, msg) = redeemStatus!!
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            color = if (ok) ClestraOnline else ClestraError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    NeumorphicButton(
                        onClick = {
                            if (redeemCodeInput.isNotBlank()) {
                                viewModel.redeemTagCode(redeemCodeInput)
                                redeemCodeInput = ""
                            }
                        },
                        enabled = redeemCodeInput.isNotBlank(),
                        cornerRadius = 14.dp,
                        containerColor = if (redeemCodeInput.isNotBlank()) ClestraCopper else ClestraNeuWell,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("redeem_code_button")
                    ) {
                        Text(
                            text = "Redeem Tag",
                            color = if (redeemCodeInput.isNotBlank()) Color.White else ClestraTextSoft,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (unlockedTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Unlocked Tags (tap to switch)",
                            fontSize = 11.sp,
                            color = ClestraTextSoft,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            unlockedTags.forEach { tag ->
                                val isSelected = tag == activeTag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .then(
                                            if (isSelected) {
                                                Modifier.neumorphicPressed(
                                                    cornerRadius = 50.dp,
                                                    containerColor = ClestraNeuWell
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            viewModel.selectActiveTag(if (isSelected) null else tag)
                                        }
                                        .padding(3.dp)
                                ) {
                                    SoftUiBadgeTag(label = tag)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NeumorphicCard(
                cornerRadius = 20.dp,
                elevation = 6.dp,
                containerColor = Color.White,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Relay Status: $syncStatus",
                        fontSize = 12.sp,
                        color = ClestraTextSoft,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeumorphicButton(
                            onClick = { viewModel.syncNow() },
                            enabled = !isSyncing,
                            cornerRadius = 14.dp,
                            containerColor = ClestraNeuSurface,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = ClestraCopper,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSyncing) "Syncing..." else "Sync Relay",
                                    color = ClestraCopper,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        NeumorphicButton(
                            onClick = onOpenSettings,
                            cornerRadius = 14.dp,
                            containerColor = ClestraNeuSurface,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_settings_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = ClestraCopper,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Settings",
                                    color = ClestraCopper,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
