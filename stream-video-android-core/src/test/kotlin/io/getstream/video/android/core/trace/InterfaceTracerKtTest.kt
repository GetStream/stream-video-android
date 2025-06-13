package io.getstream.video.android.core.trace

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

interface TestInterface {
    fun foo(x: Int): String
    fun bar(y: String): Int
}

class TestImpl : TestInterface {
    override fun foo(x: Int) = "foo$x"
    override fun bar(y: String) = y.length
}

class InterfaceTracerKtTest {
    @Test
    fun `tracedWith proxies method calls and traces them`() {
        val tracer = spyk(Tracer("iface"))
        val impl = TestImpl()
        val proxy = tracedWith<TestInterface>(impl, tracer)

        val fooResult = proxy.foo(42)
        val barResult = proxy.bar("hello")

        verify { tracer.trace("foo", match { it is Array<*> && it[0] == 42 }) }
        verify { tracer.trace("bar", match { it is Array<*> && it[0] == "hello" }) }
        assert(fooResult == "foo42")
        assert(barResult == 5)
    }
}

