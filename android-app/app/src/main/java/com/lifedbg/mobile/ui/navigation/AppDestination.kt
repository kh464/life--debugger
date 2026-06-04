package com.lifedbg.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Wysiwyg
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Dashboard : AppDestination("dashboard", "首页", Icons.Outlined.Home)
    data object Timeline : AppDestination("timeline", "时间线", Icons.Outlined.Timeline)
    data object Review : AppDestination("review", "复盘", Icons.Outlined.Wysiwyg)
    data object Settings : AppDestination("settings", "设置", Icons.Outlined.Settings)
}

object UtilityRoutes {
    const val ONBOARDING = "onboarding"
    const val PERMISSION = "permission"
    const val DEVICES = "devices"
    const val LLM_SETTINGS = "llm_settings"
    const val LLM_PREVIEW = "llm_preview"
    const val PRIVACY = "privacy"
    const val INTENT = "intent"
    const val DATA = "data"
}
