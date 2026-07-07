package com.example.laketownturf.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Bottom navigation destinations for the main app.
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(
        route = "main/home",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    BOOKINGS(
        route = "main/bookings",
        label = "Bookings",
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt,
    ),
    PROFILE(
        route = "main/profile",
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
    ),
}

/**
 * App bottom navigation bar with animated icon transitions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomBar(
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        NavigationBar(
            modifier = modifier,
            containerColor = cs.surface,
            contentColor = cs.onSurfaceVariant,
        ) {
            BottomNavItem.entries.forEach { item ->
                val isSelected = currentRoute == item.route

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                    label = "icon_scale_${item.name}",
                )

                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.5f),
                    label = "icon_tint_${item.name}",
                )

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onItemClick(item) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) cs.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.scale(iconScale).androidx.compose.foundation.layout.size(32.dp),
                                tint = tintColor,
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = tintColor,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = cs.primary,
                        unselectedIconColor = cs.onSurfaceVariant.copy(alpha = 0.5f),
                        selectedTextColor = cs.primary,
                        unselectedTextColor = cs.onSurfaceVariant.copy(alpha = 0.5f),
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}
