package net.execheinz.upgrader.logic;

import java.util.LinkedHashSet;
import java.util.UUID;

public final class AttemptIdLedger {
    private final int limit;
    private final LinkedHashSet<UUID> seen = new LinkedHashSet<>();

    public AttemptIdLedger(int limit) { this.limit = Math.max(1, limit); }

    public synchronized boolean markIfNew(UUID id) {
        if (id == null || !seen.add(id)) return false;
        while (seen.size() > limit) seen.remove(seen.iterator().next());
        return true;
    }

    public synchronized boolean contains(UUID id) { return seen.contains(id); }
}
