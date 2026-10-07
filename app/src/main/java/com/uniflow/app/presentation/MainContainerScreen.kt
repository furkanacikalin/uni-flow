package com.uniflow.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.uniflow.app.presentation.club_list.ClubListScreen
import com.uniflow.app.presentation.components.MainTab
import com.uniflow.app.presentation.components.UniFlowBottomBar
import com.uniflow.app.presentation.components.UniFlowHeader
import com.uniflow.app.presentation.event_list.EventListScreen
import com.uniflow.app.presentation.profile.ProfileScreen

@Composable
fun MainContainerScreen(
    universityId: String,
    universityName: String,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToClubManagement: (String) -> Unit = {},
    onNavigateToEventDetail: (String) -> Unit = {},
    onNavigateToClubDetail: (String) -> Unit = {},
    onNavigateToPastEvents: (String) -> Unit = {}
) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.EVENTS) }

    Scaffold(
        topBar = {
            UniFlowHeader(
                title = when (currentTab) {
                    MainTab.EVENTS -> "Etkinlikler"
                    MainTab.CLUBS -> "Kulüpler"
                    MainTab.PROFILE -> "Profil"
                },
                universityName = universityName,
                onNotificationClick = {  }
            )
        },
        bottomBar = {
            UniFlowBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
            .background(Color(0xFFF8FAFC))
        ) {
            when (currentTab) {
                MainTab.EVENTS -> {
                    EventListScreen(
                        universityId = universityId,
                        onEventClick = { event -> onNavigateToEventDetail(event.id) },
                        onNavigateToPastEvents = onNavigateToPastEvents
                    )
                }
                MainTab.CLUBS -> {
                    ClubListScreen(
                        universityName = universityName,
                        onNavigateToClubManagement = onNavigateToClubManagement,
                        onNavigateToClubDetail = onNavigateToClubDetail
                    )
                }
                MainTab.PROFILE -> {
                    ProfileScreen(
                        onLogoutSuccess = onLogout,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToClubManagement = onNavigateToClubManagement,
                        onNavigateToEventDetail = onNavigateToEventDetail,
                        onNavigateToClubDetail = onNavigateToClubDetail
                    )
                }
            }
        }
    }
}
