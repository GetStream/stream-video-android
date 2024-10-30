package io.getstream.video.android

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import strikt.assertions.isEqualTo

class ActivityScenarioOwnRule<T : Activity>(private val activityClass: Class<T>?) : TestRule {

    private var internalScenario: ActivityScenario<T>? = null
    val scenario: ActivityScenario<T>
        get() = internalScenario
            ?: throw IllegalStateException("Activity not started, use `launchActivity()` or `launchActivityForResult()`")
    val result: Instrumentation.ActivityResult get() = scenario.result

    fun launchActivity() {
        internalScenario = ActivityScenario.launch(activityClass)
    }

    fun launchActivity(vararg extras: Pair<String, Any>) {
        internalScenario = ActivityScenario.launch(
            Intent(ApplicationProvider.getApplicationContext<T>(), activityClass)
                .putExtras(bundleOf(*extras))
        )
    }

    fun launchActivity(intent: Intent) {
        internalScenario = ActivityScenario.launch(intent)
    }

    fun launchActivityForResult(intent: Intent) {
        internalScenario = ActivityScenario.launchActivityForResult(intent)
    }

    fun launchActivityForResult() {
        internalScenario = ActivityScenario.launchActivityForResult(activityClass as Class<T>)
    }

    fun ensureAppClosed() {
        strikt.api.expectThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() =
                cleanup({ base.evaluate() }, { scenario.close() })
        }
    }
}

private fun <T> cleanup(block: () -> T, cleanup: () -> Unit): T {
    var exceptionThrown = false
    try {
        return block()
    } catch (exception: Throwable) {
        exceptionThrown = true
        throw exception
    } finally {
        if (exceptionThrown) {
            try {
                cleanup()
            } catch (ignoreException: Throwable) {
                // ignore closing exception in case another exception is thrown
                // we don't want to mask the original exception.
            }
        } else {
            cleanup()
        }
    }
}
