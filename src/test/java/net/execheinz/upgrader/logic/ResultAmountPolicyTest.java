package net.execheinz.upgrader.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class ResultAmountPolicyTest {
    @Test
    void configuredMaximumCanNeverExceedOneStack() {
        assertEquals(64, ResultAmountPolicy.maximum(256));
        assertEquals(64, ResultAmountPolicy.maximum(64));
        assertEquals(16, ResultAmountPolicy.maximum(16));
        assertEquals(1, ResultAmountPolicy.maximum(0));
    }
}
