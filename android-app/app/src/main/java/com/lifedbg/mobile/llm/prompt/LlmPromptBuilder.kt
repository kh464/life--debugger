package com.lifedbg.mobile.llm.prompt

class LlmPromptBuilder {
    fun systemPrompt(): String {
        return """
            你是 Life Debugger 的行为复盘分析引擎。
            你只能基于用户提供的摘要事件进行分析。
            不要假装知道摘要之外的内容。
            不要推断用户在 App 内看了什么具体内容。
            不要给医学诊断。
            不要使用羞辱、指责、道德审判式语言。
            你的目标是帮助用户理解注意力流动、任务切换、可能的分心点和可执行的改进建议。
            请输出严格 JSON，不要输出 Markdown。
        """.trimIndent()
    }

    fun userPrompt(payloadJson: String): String {
        return """
            下面是用户一天的 Android 手机行为摘要。

            请分析：
            1. 今日行为概览；
            2. 主要时间投入；
            3. 可能的注意力切换点；
            4. 可能的任务漂移；
            5. 手机使用模式；
            6. 积极行为模式；
            7. 3 条具体改进建议；
            8. 需要用户确认的不确定点。

            要求：
            - 只基于摘要事件分析；
            - 不要推断具体 App 内内容；
            - 不要责备用户；
            - 输出 JSON；
            - JSON 字段必须包含 daily_summary、time_distribution、attention_shifts、possible_task_drift、positive_patterns、suggestions、questions_for_user、risk_level、tone。

            摘要数据如下：
            $payloadJson
        """.trimIndent()
    }
}
