package com.uniflow.app.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uniflow.app.domain.model.ClubApplication
import com.uniflow.app.presentation.club_list.ModernClubCard
import com.uniflow.app.presentation.components.CategoryChipRow
import com.uniflow.app.presentation.components.ProfileHeaderCard
import com.uniflow.app.presentation.components.ProfileStatsRow
import com.uniflow.app.presentation.components.StandardEventCard
import com.uniflow.app.presentation.components.TicketCard

@Composable
fun ProfileScreen(
    onLogoutSuccess: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToClubManagement: ((String) -> Unit)? = null,
    onNavigateToEventDetail: ((String) -> Unit)? = null,
    onNavigateToClubDetail: ((String) -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val tabs = listOf("Kayıtlarım", "Üyeliklerim", "Kulüplerim", "Kaydedilenler", "Geçmiş")

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            onLogoutSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    ProfileHeaderCard(
                        userProfile = state.userProfile,
                        onSettingsClick = onNavigateToSettings
                    )
                }


                item {
                    ProfileStatsRow(
                        joinedClubsCount = state.joinedClubs.size + state.managedClubs.size,
                        attendedEventsCount = state.tickets.size + state.pastEvents.size
                    )
                }


                item {
                    CategoryChipRow(
                        categories = tabs,
                        selectedCategory = state.selectedTab,
                        onCategorySelected = { viewModel.onTabSelected(it) }
                    )
                }


                when (state.selectedTab) {
                    "Kayıtlarım" -> {
                        item {
                            SectionHeader(
                                icon = "🎫 ",
                                title = "Kayıtlı Aktif Etkinliklerim",
                                badgeText = "${state.tickets.size} Aktif",
                                badgeColor = Color(0xFFDBEAFE),
                                badgeTextColor = Color(0xFF1D4ED8)
                            )
                        }

                        if (state.tickets.isEmpty()) {
                            item {
                                EmptyProfileSectionCard(
                                    title = "Aktif Kayıt Bulunmuyor",
                                    description = "Henüz kayıt yaptığınız aktif bir etkinlik bulunmuyor. Etkinlikler sayfasından ilgini çeken etkinliklere katılabilirsin."
                                )
                            }
                        } else {
                            items(state.tickets) { ticket ->
                                TicketCard(
                                    ticket = ticket,
                                    onCardClick = { onNavigateToEventDetail?.invoke(ticket.id) }
                                )
                            }
                        }
                    }

                    "Üyeliklerim" -> {
                        item {
                            SectionHeader(
                                icon = "👥 ",
                                title = "Üye Olduğum Kulüpler",
                                badgeText = "${state.joinedClubs.size} Kulüp",
                                badgeColor = Color(0xFFDCFCE7),
                                badgeTextColor = Color(0xFF15803D)
                            )
                        }

                        if (state.joinedClubs.isEmpty()) {
                            item {
                                EmptyProfileSectionCard(
                                    title = "Kulüp Üyeliğiniz Yok",
                                    description = "Henüz normal üye olduğunuz bir kulüp bulunmuyor. Kulüpler sayfasından ilgilendiğiniz kulüplere katılabilirsiniz."
                                )
                            }
                        } else {
                            items(state.joinedClubs) { club ->
                                ModernClubCard(
                                    club = club,
                                    currentUserId = state.userProfile.uid,
                                    onManageClick = { onNavigateToClubManagement?.invoke(club.id) },
                                    onCardClick = { onNavigateToClubDetail?.invoke(club.id) }
                                )
                            }
                        }
                    }

                    "Kulüplerim" -> {
                        item {
                            SectionHeader(
                                icon = "👑 ",
                                title = "Yöneticisi Olduğum Kulüpler",
                                badgeText = "${state.managedClubs.size} Kulüp",
                                badgeColor = Color(0xFFFEF3C7),
                                badgeTextColor = Color(0xFFB45309)
                            )
                        }

                        if (state.managedClubs.isEmpty()) {
                            item {
                                EmptyProfileSectionCard(
                                    title = "Yönettiğiniz Kulüp Yok",
                                    description = "Henüz yöneticisi olduğunuz aktif bir kulüp bulunmuyor. Kulüp kurma başvurusu yapmak için kulüp kurma sayfasını ziyaret edebilirsiniz."
                                )
                            }
                        } else {
                            items(state.managedClubs) { club ->
                                ModernClubCard(
                                    club = club,
                                    currentUserId = state.userProfile.uid,
                                    onManageClick = { onNavigateToClubManagement?.invoke(club.id) },
                                    onCardClick = { onNavigateToClubDetail?.invoke(club.id) }
                                )
                            }
                        }
                    }

                    "Kaydedilenler" -> {
                        item {
                            SectionHeader(
                                icon = "🔖 ",
                                title = "Kaydedilen Etkinlikler",
                                badgeText = "${state.bookmarkedEvents.size} Kaydedilen",
                                badgeColor = Color(0xFFF3E8FF),
                                badgeTextColor = Color(0xFF7E22CE)
                            )
                        }

                        if (state.bookmarkedEvents.isEmpty()) {
                            item {
                                EmptyProfileSectionCard(
                                    title = "Kaydedilen Etkinlik Yok",
                                    description = "Henüz kaydettiğiniz bir etkinlik bulunmuyor. Etkinlikler sayfasından yer işareti butonuna basarak kaydedebilirsiniz."
                                )
                            }
                        } else {
                            items(state.bookmarkedEvents) { event ->
                                StandardEventCard(
                                    event = event,
                                    onCardClick = { onNavigateToEventDetail?.invoke(event.id) },
                                    onBookmarkClick = { viewModel.toggleBookmark(it) }
                                )
                            }
                        }
                    }

                    "Geçmiş" -> {
                        item {
                            SectionHeader(
                                icon = "📜 ",
                                title = "Geçmiş Etkinlik Katılımlarım",
                                badgeText = "${state.pastEvents.size} Geçmiş",
                                badgeColor = Color(0xFFF1F5F9),
                                badgeTextColor = Color(0xFF475569)
                            )
                        }

                        if (state.pastEvents.isEmpty()) {
                            item {
                                EmptyProfileSectionCard(
                                    title = "Geçmiş Etkinlik Katılımı Yok",
                                    description = "Geçmişte katıldığınız veya tamamlanan etkinlikleriniz burada görüntülenecektir."
                                )
                            }
                        } else {
                            items(state.pastEvents) { pastTicket ->
                                TicketCard(
                                    ticket = pastTicket,
                                    onCardClick = { onNavigateToEventDetail?.invoke(pastTicket.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: String,
    title: String,
    badgeText: String,
    badgeColor: Color,
    badgeTextColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 16.sp)
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        Surface(
            shape = CircleShape,
            color = badgeColor
        ) {
            Text(
                text = badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = badgeTextColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyProfileSectionCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileApplicationCard(application: ClubApplication) {
    val (statusBg, statusText, statusLabel) = when (application.status) {
        "APPROVED" -> Triple(Color(0xFFD1FAE5), Color(0xFF047857), "Onaylandı")
        "REJECTED" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Reddedildi")
        else -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "İncelemede")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = application.clubName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Kategori: ${application.clubCategory}",
                fontSize = 12.sp,
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.SemiBold
            )

            if (application.clubDescription.isNotBlank()) {
                Text(
                    text = application.clubDescription,
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}
