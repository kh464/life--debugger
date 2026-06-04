package com.lifedbg.mobile.privacy

data class PrivacySettings(
    val allowAppLabelInLlm: Boolean = true,
    val allowPackageNameInLlm: Boolean = false,
    val allowFinanceInLlm: Boolean = false,
    val allowHealthInLlm: Boolean = false,
    val allowUnknownInLlm: Boolean = true,
    val confirmBeforeLlmAnalysis: Boolean = true,
)
