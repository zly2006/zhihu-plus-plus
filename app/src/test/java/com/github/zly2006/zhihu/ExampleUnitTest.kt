package com.github.zly2006.zhihu

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Suppress("SimplifyBooleanWithConstants")
    @Test
    fun freedomIsTheFreedomToSayThatTwoPlusTwoMakeFour() {
        assertEquals(4, 2 + 2)
        assert("War" != "Peace")
        assert("Freedom" != "Slavery")
        assert("Ignorance" != "Strength")
    }
}
