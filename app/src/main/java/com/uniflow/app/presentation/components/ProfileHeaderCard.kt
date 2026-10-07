package com.uniflow.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniflow.app.domain.model.UserProfile

import androidx.compose.material.icons.filled.Settings

@Composable
fun ProfileHeaderCard(
    userProfile: UserProfile,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ayarlar",
                    tint = Color(0xFF64748B)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFDBEAFE),
                        modifier = Modifier
                            .size(80.dp)
                            .border(3.dp, Color(0xFF93C5FD), CircleShape)
                            .clip(CircleShape)
                    ) {
                        if (userProfile.profileImageUrl.isNotBlank()) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val imageModel = androidx.compose.runtime.remember(userProfile.profileImageUrl) {
                                if (userProfile.profileImageUrl.startsWith("data:image")) {
                                    try {
                                        val base64Data = userProfile.profileImageUrl.substringAfter(",")
                                        android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                    } catch (e: Exception) {
                                        userProfile.profileImageUrl
                                    }
                                } else {
                                    userProfile.profileImageUrl
                                }
                            }
                            val imageRequest = androidx.compose.runtime.remember(imageModel) {
                                coil.request.ImageRequest.Builder(context)
                                    .data(imageModel)
                                    .crossfade(true)
                                    .allowHardware(false)
                                    .build()
                            }
                            coil.compose.SubcomposeAsyncImage(
                                model = imageRequest,
                                contentDescription = "Profil Fotoğrafı",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color(0xFF1D4ED8),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                error = {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF1D4ED8),
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1D4ED8))
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userProfile.fullName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (userProfile.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Doğrulanmış",
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${userProfile.department} • ${userProfile.grade}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2563EB)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "🪪 Öğrenci No: ${userProfile.studentNo}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun ProfileStatsRow(
    joinedClubsCount: Int,
    attendedEventsCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            count = "$joinedClubsCount",
            label = "Üye Olunan\nKulüp",
            modifier = Modifier.weight(1f)
        )

        StatCard(
            count = "$attendedEventsCount",
            label = "Katılınan\nEtkinlik",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    count: String,
    label: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) Color(0xFFFFF7ED) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) Color(0xFFC2410C) else Color(0xFF1E3A8A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                lineHeight = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun rememberBitmapFromUri(uriString: String?, context: android.content.Context): androidx.compose.ui.graphics.ImageBitmap? {
    return androidx.compose.runtime.remember(uriString) {
        if (uriString.isNullOrBlank()) null
        else {
            try {
                val uri = android.net.Uri.parse(uriString)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source).asImageBitmap()
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri).asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
