package io.getstream.video.android.core.utils

import android.content.Context
import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class AndroidUtilsKtTest {

    @Test
    fun `For valid resId the value is returned from stringOrDefault`() {
        // Given
        val resId = 123
        val context = mockk<Context>(relaxed = true)
        val expected = "String from strings.xml"
        every { context.getString(resId) } returns expected

        // When
        val actual = stringOrDefault(context, resId, "some default")

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `For invalid resId the default value is returned from stringOrDefault`() {
        // Given
        val resId = 123
        val context = mockk<Context>(relaxed = true)
        val default = "some default"
        every { context.getString(resId) } throws Resources.NotFoundException()

        // When
        val actual = stringOrDefault(context, resId, default)

        // Then
        assertEquals(default, actual)
    }
}