package net.execheinz.upgrader.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttemptIdLedgerTest {
    @Test void duplicatePacketIdIsAcceptedExactlyOnce() {
        AttemptIdLedger ledger = new AttemptIdLedger(16);
        UUID id = UUID.randomUUID();
        assertTrue(ledger.markIfNew(id));
        assertFalse(ledger.markIfNew(id));
    }
    @Test void ledgerIsBoundedAndEvictsOldestIds() {
        AttemptIdLedger ledger = new AttemptIdLedger(2);
        UUID first = UUID.randomUUID();
        ledger.markIfNew(first); ledger.markIfNew(UUID.randomUUID()); ledger.markIfNew(UUID.randomUUID());
        assertTrue(ledger.markIfNew(first));
    }
}
