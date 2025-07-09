package io.getstream.video.android.client.internal.common

import io.getstream.log.TaggedLogger
import io.getstream.video.android.client.api.subscribe.StreamSubscriber
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

public class StreamSubscriptionManagerImplTest {

    private class DummySubscriber : StreamSubscriber
    private val logger: TaggedLogger = mockk(relaxed = true)
    private val manager = StreamSubscriptionManagerImpl<DummySubscriber>(logger)

    @Test
    public fun `concurrent subscribe unsubscribe and forEach`() {
        val nThreads = 50
        val nOpsPerThread = 1_000
        val pool = Executors.newFixedThreadPool(nThreads)

        // keep track of all the generated ids, so we can unsubscribe them too
        val allIds = ConcurrentLinkedQueue<Int>()

        repeat(nThreads) {
            pool.submit {
                repeat(nOpsPerThread) {
                    // 50% chance to subscribe, 50% to unsubscribe or forEach
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        val id = manager.subscribe(DummySubscriber()).getOrThrow()
                        allIds += id
                    } else {
                        manager.forEach { /* no-op */ }
                        // randomly pick an id to unsubscribe
                        allIds.poll()?.let { manager.unsubscribe(it) }
                    }
                }
            }
        }

        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))

        // After everyone’s done, cleanup and ensure size <= max
        manager.clear().getOrThrow()
        assertTrue(manager.forEach { }.isSuccess)
    }

    @Test
    public fun `weak references are pruned after GC`() {
        val manager = StreamSubscriptionManagerImpl<DummySubscriber>(logger, 100)
        var oneStrongRef: DummySubscriber? = null // Assign to any DummySubscriber()

        fun makeAndRegister(): WeakReference<DummySubscriber> {
            val sub = DummySubscriber()
            val weak = WeakReference(sub)
            if (oneStrongRef == null) {
                oneStrongRef = sub
            }
            manager.subscribe(sub).getOrThrow()
            return weak
        }

        val weakRefs = (1..100).map { makeAndRegister() }

        // Drop strong refs:
        System.gc()
        Thread.sleep(100)


        var called = 0
        manager.forEach {
            // There should be only one
            called++
        }

        // Count how many have been reclaimed:
        val reclaimed = weakRefs.count { it.get() == null }
        println("Reclaimed $reclaimed of ${weakRefs.size}")

        // We expect *all* to be reclaimed, since we dropped strong refs:
        assertEquals(99, reclaimed)
        assertEquals(100, weakRefs.size)
        assertEquals(1, called)
    }

    @Test
    public fun `max listeners is enforced`() {
        val manager = StreamSubscriptionManagerImpl<DummySubscriber>(logger, 100)
        repeat(101) {
            manager.subscribe(DummySubscriber()).getOrThrow()
        }
        assert(manager.subscribe(DummySubscriber()).isFailure)
    }

}
