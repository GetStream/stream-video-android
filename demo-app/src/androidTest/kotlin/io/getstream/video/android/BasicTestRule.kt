package io.getstream.video.android

import android.app.Activity
import android.os.Build
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfigServerException
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

inline fun <reified T: Activity> testRule() = BasicTestRule(T::class.java)
fun launchAppTestRule() = BasicTestRule(MainActivity::class.java)

class BasicTestRule<T : Activity>(activityClass: Class<T>?) : TestRule  {

    private val baseRule = BaseRule()
    val server = baseRule.server

    val activity = ActivityScenarioOwnRule(activityClass)
    val nonFatalErrorsCrasher = baseRule.nonFatalErrorsCrasher

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain.outerRule(activity)
            .around(baseRule)
            .apply(base, description)
    }
}

class BaseRule : TestRule {

    val server = StreamMockWebServerRule()
    val nonFatalErrorsCrasher = NonFatalErrorsCrasher()

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain.emptyRuleChain()
            .around(server)
            .around(nonFatalErrorsCrasher)
            .apply(base, description)
    }
}

class NonFatalErrorsCrasher  : TestRule {

    // This list contains that we would like to ignore during a test execution
    val ignoredErrorsFilter: MutableList<(Throwable) -> Boolean> = mutableListOf<(Throwable) -> Boolean>()
        .apply {
            // This is known bug in splash screen that we would like to ignore
            add { it.message == "SplashProvider iconView is null"}

            // KCHAT-362 During tests, we sometimes face limits of Firebase that assumes that our
            // IP address makes too much requests to Firebase. We probably in our integration tests
            // shouldn't fetch real firebase config, but this is not yet done.
            add { it is FirebaseRemoteConfigServerException }
        }
    val capturedExceptions = mutableListOf<Throwable>()
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        base.evaluate()
                        val notIgnoredErrors = capturedExceptions
                                .filter { throwable -> !ignoredErrorsFilter.any { matcher -> matcher(throwable) } }
                        if (notIgnoredErrors.isNotEmpty()) {
                            throw notIgnoredErrors.first()
                        }
                    } finally {
                        capturedExceptions.clear()
                    }
                } else {
                    base.evaluate()
                }

            }
        }
}

fun Throwable.anyThrowableInStackTrace(predicate: (Throwable) -> Boolean): Boolean = if (predicate(this)) {
    true
} else {
    cause?.anyThrowableInStackTrace(predicate) ?: false
}