package ticketline;

import java.math.BigInteger;
import java.util.List;

/**
 * Model-side session statistics. All durations are whole seconds.
 * Created with GenAI assistance for Ari Gallo's timing feature.
 * HelpDeskModel records a sample only after successful ticket completion.
 */
public class ResolutionTimeCalculator {
    private BigInteger totalSeconds = BigInteger.ZERO;
    private long completedCount;

    /** Records one completed ticket; invalid durations leave statistics unchanged. */
    public void recordResolutionTime(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Resolution time cannot be negative.");
        }
        long nextCount = Math.addExact(completedCount, 1);
        totalSeconds = totalSeconds.add(BigInteger.valueOf(seconds));
        completedCount = nextCount;
    }

    /** Returns the session mean rounded down, or -1 when there are no samples. */
    public long getAverageResolutionTime() {
        if (completedCount == 0) {
            return -1;
        }
        return totalSeconds.divide(BigInteger.valueOf(completedCount)).longValueExact();
    }

    /**
     * Estimates time until a pending ticket starts, using the queue's current order.
     * Null activeElapsedSeconds means no active ticket; otherwise it must be >= 0.
     * Rejects invalid or missing IDs even when no timing samples exist.
     * Does not change the queue, tickets, or statistics. Large estimates saturate
     * at Long.MAX_VALUE rather than wrapping into a negative duration.
     */
    public long getEstimatedWaitTime(List<Ticket> pending, String id,
        Long activeElapsedSeconds) {
        String normalizedId = Ticket.normalizeId(id);
        if (pending == null || (activeElapsedSeconds != null
            && activeElapsedSeconds.longValue() < 0)) {
            throw new IllegalArgumentException("Invalid pending tickets or elapsed time.");
        }
        int position = -1;
        for (int i = 0; i < pending.size(); i++) {
            Ticket ticket = pending.get(i);
            if (ticket == null || !"Open".equals(ticket.getStatus())) {
                throw new IllegalArgumentException("Expected only open pending tickets.");
            }
            if (ticket.getId().equals(normalizedId)) {
                if (position != -1) {
                    throw new IllegalArgumentException("Duplicate pending ticket ID.");
                }
                position = i;
            }
        }
        if (position == -1) {
            throw new IllegalArgumentException("Ticket is not pending.");
        }
        long average = getAverageResolutionTime();
        if (average == -1) {
            return -1;
        }
        long remaining = activeElapsedSeconds == null ? 0
            : Math.max(0, average - activeElapsedSeconds.longValue());
        BigInteger estimate = BigInteger.valueOf(average)
            .multiply(BigInteger.valueOf(position)).add(BigInteger.valueOf(remaining));
        return estimate.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
}
