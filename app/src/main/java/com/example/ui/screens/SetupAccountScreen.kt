package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ClestraIdGenerator
import com.example.ui.ChatViewModel
import com.example.ui.components.ClestraPillInput
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraError
import com.example.ui.theme.ClestraTextSoft

@Composable
fun SetupAccountScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val previewId by remember(password, name) {
        derivedStateOf {
            if (password.length >= 4 && name.isNotBlank()) {
                ClestraIdGenerator.generateUniqueClestraId(password, name)
            } else {
                "CLS-••••-••••-••••"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClestraCream)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Clestra Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Clestra",
                color = ClestraCharcoal,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Create your profile for a unique, uncollidable Clestra ID",
                color = ClestraTextSoft,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            ClestraPillInput(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                hint = "Display Name",
                modifier = Modifier.testTag("setup_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            ClestraPillInput(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                hint = "Account Password",
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                modifier = Modifier.testTag("setup_password_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            ClestraPillInput(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                hint = "Confirm Password",
                isPassword = true,
                passwordVisible = passwordVisible,
                modifier = Modifier.testTag("setup_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ClestraBubbleSent.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your Permanent Clestra ID",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClestraTextSoft
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = previewId,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClestraCharcoal
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    color = ClestraError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        name.isBlank() -> errorMessage = "Please enter your display name."
                        password.length < 4 -> errorMessage = "Password must be at least 4 characters."
                        password != confirmPassword -> errorMessage = "Passwords do not match."
                        else -> {
                            viewModel.setupAccount(
                                name = name.trim(),
                                password = password
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClestraCopper,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("complete_setup_button")
            ) {
                Text(
                    text = "Generate ID & Enter Clestra",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
