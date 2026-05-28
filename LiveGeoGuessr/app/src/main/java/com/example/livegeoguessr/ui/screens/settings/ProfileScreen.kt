package com.example.livegeoguessr.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.livegeoguessr.R
import com.example.livegeoguessr.ui.components.ProfileImage
import com.example.livegeoguessr.ui.navigation.Screen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.updateProfilePicture(it) }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempUri?.let { viewModel.updateProfilePicture(it) }
            }
        }
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 16.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture with arc and badges
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Primary Border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                
                ProfileImage(
                    imageUrl = uiState.profileImageUrl,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable { showSheet = true }
                )

                // Score Badge (Primary)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 4.dp)
                        .height(32.dp)
                        .width(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.pointsTotal.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.displayName,
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f),
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Cursive,
                modifier = Modifier.clickable { showNameDialog = true }
            )

            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_friend),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Cards
            ProfileStatsCard(
                icon = Icons.Default.Group,
                label = stringResource(R.string.friends),
                value = uiState.friendsCount.toString()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ProfileStatsCard(
                icon = Icons.Default.CameraAlt,
                label = stringResource(R.string.posts),
                value = uiState.postsCount.toString()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ProfileStatsCard(
                icon = Icons.Default.LocationOn,
                label = stringResource(R.string.guesses),
                value = uiState.guessesCount.toString()
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.choose_photo_source),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                
                ListItem(
                    headlineContent = { Text(stringResource(R.string.camera)) },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val file = File.createTempFile("profile_", ".jpg", context.cacheDir).apply {
                            deleteOnExit()
                        }
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        tempUri = uri
                        cameraLauncher.launch(uri)
                        showSheet = false
                    }
                )
                
                ListItem(
                    headlineContent = { Text(stringResource(R.string.gallery)) },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                        showSheet = false
                    }
                )
            }
        }
    }

    if (showNameDialog) {
        var newName by remember { mutableStateOf(uiState.displayName) }
        
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.change_name)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.display_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDisplayName(newName)
                        viewModel.saveProfile()
                        showNameDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.cancel_retake)) // Or use a separate "Cancel" string
                }
            }
        )
    }
}

@Composable
fun ProfileStatsCard(
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            Text(
                text = value,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace, // Digital look
                fontWeight = FontWeight.Bold
            )
        }
    }
}
