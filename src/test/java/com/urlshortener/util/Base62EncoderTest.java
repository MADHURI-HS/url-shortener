package com.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class Base62EncoderTest {

    @Test
    void encodesSingleDigitValues() {
        assertEquals("1", Base62Encoder.encode(1));
        assertEquals("Z", Base62Encoder.encode(61));
    }

    @Test
    void rollsOverAtBase() {
        assertEquals("10", Base62Encoder.encode(62));
    }

    @Test
    void decodeReversesEncode() {
        for (long n : new long[]{1, 61, 62, 99_999, 100_000, 123_456_789L}) {
            assertEquals(n, Base62Encoder.decode(Base62Encoder.encode(n)));
        }
    }

    @Test
    void consecutiveIdsProduceDifferentCodes() {
        assertNotEquals(Base62Encoder.encode(100_000), Base62Encoder.encode(100_001));
    }
}
