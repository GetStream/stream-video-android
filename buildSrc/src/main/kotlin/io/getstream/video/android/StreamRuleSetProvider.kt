package io.getstream.video.android

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class StreamRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = "StreamRules"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            NoDirectTimeUsageRule(config)
        )
    )
}
