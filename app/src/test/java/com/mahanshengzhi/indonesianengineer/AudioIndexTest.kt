package com.mahanshengzhi.indonesianengineer

import com.mahanshengzhi.indonesianengineer.audio.AudioIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioIndexTest {
    @Test
    fun sha256_uses_exact_utf8_text() {
        assertEquals(
            "47e8f094e1aca27195e6de64d47e8aa2b4f9c234d95da517084e0926fdaa8ba1",
            AudioIndex.sha256("Tes")
        )
    }
}
