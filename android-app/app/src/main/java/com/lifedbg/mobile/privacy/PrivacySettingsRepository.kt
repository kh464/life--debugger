package com.lifedbg.mobile.privacy

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.privacyDataStore by preferencesDataStore(name = "privacy_settings")

class PrivacySettingsRepository(private val context: Context) {
    val settings: Flow<PrivacySettings> = context.privacyDataStore.data.map { preferences ->
        PrivacySettings(
            allowAppLabelInLlm = preferences[ALLOW_APP_LABEL] ?: true,
            allowPackageNameInLlm = preferences[ALLOW_PACKAGE_NAME] ?: false,
            allowFinanceInLlm = preferences[ALLOW_FINANCE] ?: false,
            allowHealthInLlm = preferences[ALLOW_HEALTH] ?: false,
            allowUnknownInLlm = preferences[ALLOW_UNKNOWN] ?: true,
            confirmBeforeLlmAnalysis = preferences[CONFIRM_BEFORE_LLM] ?: true,
        )
    }

    suspend fun update(transform: (PrivacySettings) -> PrivacySettings) {
        context.privacyDataStore.edit { preferences ->
            val current = PrivacySettings(
                allowAppLabelInLlm = preferences[ALLOW_APP_LABEL] ?: true,
                allowPackageNameInLlm = preferences[ALLOW_PACKAGE_NAME] ?: false,
                allowFinanceInLlm = preferences[ALLOW_FINANCE] ?: false,
                allowHealthInLlm = preferences[ALLOW_HEALTH] ?: false,
                allowUnknownInLlm = preferences[ALLOW_UNKNOWN] ?: true,
                confirmBeforeLlmAnalysis = preferences[CONFIRM_BEFORE_LLM] ?: true,
            )
            val next = transform(current)
            preferences[ALLOW_APP_LABEL] = next.allowAppLabelInLlm
            preferences[ALLOW_PACKAGE_NAME] = next.allowPackageNameInLlm
            preferences[ALLOW_FINANCE] = next.allowFinanceInLlm
            preferences[ALLOW_HEALTH] = next.allowHealthInLlm
            preferences[ALLOW_UNKNOWN] = next.allowUnknownInLlm
            preferences[CONFIRM_BEFORE_LLM] = next.confirmBeforeLlmAnalysis
        }
    }

    private companion object {
        val ALLOW_APP_LABEL = booleanPreferencesKey("allow_app_label_in_llm")
        val ALLOW_PACKAGE_NAME = booleanPreferencesKey("allow_package_name_in_llm")
        val ALLOW_FINANCE = booleanPreferencesKey("allow_finance_in_llm")
        val ALLOW_HEALTH = booleanPreferencesKey("allow_health_in_llm")
        val ALLOW_UNKNOWN = booleanPreferencesKey("allow_unknown_in_llm")
        val CONFIRM_BEFORE_LLM = booleanPreferencesKey("confirm_before_llm_analysis")
    }
}
