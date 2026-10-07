package com.uniflow.app.presentation.admin_container

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniflow.app.presentation.admin_applications.AdminApplicationsScreen
import com.uniflow.app.presentation.admin_profile.AdminProfileScreen
import com.uniflow.app.presentation.admin_universities.AdminUniversitiesScreen

enum class AdminTab(val title: String) {
    UNIVERSITIES("Üniversiteler"),
    APPLICATIONS("İşlemler"),
    PROFILE("Profil")
}

@Composable
fun AdminContainerScreen(
    onLogout: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(AdminTab.UNIVERSITIES) }

    Scaffold(
        bottomBar = {
            AdminBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                AdminTab.UNIVERSITIES -> {
                    AdminUniversitiesScreen()
                }
                AdminTab.APPLICATIONS -> {
                    AdminApplicationsScreen(
                        onNavigateBack = null
                    )
                }
                AdminTab.PROFILE -> {
                    AdminProfileScreen(
                        onLogoutSuccess = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminBottomBar(
    currentTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdminBottomNavItem(
                title = "Üniversiteler",
                selectedIcon = Icons.Filled.School,
                unselectedIcon = Icons.Outlined.School,
                isSelected = currentTab == AdminTab.UNIVERSITIES,
                onClick = { onTabSelected(AdminTab.UNIVERSITIES) },
                modifier = Modifier.weight(1f)
            )

            AdminBottomNavItem(
                title = "İşlemler",
                selectedIcon = Icons.AutoMirrored.Filled.Assignment,
                unselectedIcon = Icons.AutoMirrored.Outlined.Assignment,
                isSelected = currentTab == AdminTab.APPLICATIONS,
                onClick = { onTabSelected(AdminTab.APPLICATIONS) },
                modifier = Modifier.weight(1f)
            )

            AdminBottomNavItem(
                title = "Profil",
                selectedIcon = Icons.Filled.AdminPanelSettings,
                unselectedIcon = Icons.Outlined.AdminPanelSettings,
                isSelected = currentTab == AdminTab.PROFILE,
                onClick = { onTabSelected(AdminTab.PROFILE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AdminBottomNavItem(
    title: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFF1D4ED8)
    val inactiveColor = Color(0xFF64748B)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = title,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeColor else inactiveColor
            )
        }
    }
}
