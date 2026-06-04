package com.lifedbg.mobile.summarize

import android.content.Context
import android.content.pm.PackageManager
import com.lifedbg.mobile.db.dao.AppCategoryRuleDao
import com.lifedbg.mobile.db.entity.AppCategoryRuleEntity
import com.lifedbg.mobile.core.time.TimeUtils

class AppClassifier(
    private val context: Context,
    private val ruleDao: AppCategoryRuleDao,
) {
    suspend fun classify(packageName: String): AppClassification {
        val existing = ruleDao.getByPackageName(packageName)
        if (existing != null) {
            return AppClassification(
                packageName = packageName,
                appLabel = existing.appLabel,
                category = existing.category,
                activityType = existing.activityType,
                isSensitive = existing.isSensitive,
                confidence = if (existing.userOverride) 0.98 else 0.86,
            )
        }

        val appLabel = readAppLabel(packageName)
        val rule = defaultRule(packageName, appLabel)
        val now = TimeUtils.nowMillis()
        ruleDao.upsert(
            AppCategoryRuleEntity(
                packageName = packageName,
                appLabel = appLabel,
                category = rule.category,
                activityType = rule.activityType,
                isSensitive = rule.isSensitive,
                userOverride = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return AppClassification(
            packageName = packageName,
            appLabel = appLabel ?: packageName,
            category = rule.category,
            activityType = rule.activityType,
            isSensitive = rule.isSensitive,
            confidence = rule.confidence,
        )
    }

    private fun readAppLabel(packageName: String): String? {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun defaultRule(packageName: String, appLabel: String?): Rule {
        val text = "${packageName.lowercase()} ${(appLabel ?: "").lowercase()}"
        return when {
            any(text, "youtube", "tiktok", "douyin", "抖音", "kuaishou", "快手", "bilibili") ->
                Rule("short_video", "entertainment", confidence = 0.82)
            any(text, "wechat", "微信", "telegram", "whatsapp", "line", "messenger", "qq") ->
                Rule("chat", "communication", confidence = 0.82)
            any(text, "chrome", "firefox", "edge", "browser", "samsung internet") ->
                Rule("browser", "consumption", confidence = 0.82)
            any(text, "gmail", "outlook", "mail") ->
                Rule("work", "communication", confidence = 0.74)
            any(text, "notion", "obsidian", "kindle", "read", "阅读") ->
                Rule("study", "productive", confidence = 0.74)
            any(text, "alipay", "支付宝", "bank", "银行", "finance", "wallet") ->
                Rule("finance", "life_service", isSensitive = true, confidence = 0.82)
            any(text, "health", "医院", "medical", "fit") ->
                Rule("health", "life_service", isSensitive = true, confidence = 0.76)
            any(text, "settings", "launcher", "systemui", "packageinstaller") ->
                Rule("system", "system", confidence = 0.78)
            else ->
                Rule("unknown", "unknown", confidence = 0.3)
        }
    }

    private fun any(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it.lowercase()) }
    }

    private data class Rule(
        val category: String,
        val activityType: String,
        val isSensitive: Boolean = false,
        val confidence: Double,
    )
}
