package io.getstream.video.android

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

class NoDirectTimeUsageRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "NoDirectTimeUsage",
        severity = Severity.Defect,
        description = "Do not use direct time calls. Use injected Clock/TimeProvider instead.",
        debt = Debt.FIVE_MINS
    )

    private val forbiddenCalls = listOf(
        "OffsetDateTime.now",
        "Instant.now",
        "LocalDateTime.now",
        "LocalDate.now",
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        // For qualified calls like System.currentTimeMillis(), the KtCallExpression
        // text is only "currentTimeMillis()" — walk up to the dot-qualified parent
        // to get the full "System.currentTimeMillis()" text.
        val fullText = (expression.parent as? KtDotQualifiedExpression)?.text
            ?: expression.text
        if (forbiddenCalls.any { fullText.startsWith(it) }) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    message = "NoDirectTimeUsage: use an injected time source instead of `$fullText`"
                )
            )
        }
    }
}
